package gov.nasa.jpl.aerie.merlin.server

import gov.nasa.ammos.aerie.procedural.constraints.Violation
import gov.nasa.ammos.aerie.procedural.constraints.Violations
import gov.nasa.ammos.aerie.procedural.constraints.Violations.Companion.violateOn
import gov.nasa.ammos.aerie.procedural.timeline.BaseTimeline
import gov.nasa.ammos.aerie.procedural.timeline.BoundsTransformer
import gov.nasa.ammos.aerie.procedural.timeline.CollectOptions
import gov.nasa.ammos.aerie.procedural.timeline.Interval
import gov.nasa.ammos.aerie.procedural.timeline.Interval.Companion.between
import gov.nasa.ammos.aerie.procedural.timeline.collections.Universal
import gov.nasa.ammos.aerie.procedural.timeline.collections.profiles.Booleans
import gov.nasa.ammos.aerie.procedural.timeline.collections.profiles.Constants
import gov.nasa.ammos.aerie.procedural.timeline.collections.profiles.Real
import gov.nasa.ammos.aerie.procedural.timeline.collections.profiles.Real.Companion.deserializer
import gov.nasa.ammos.aerie.procedural.timeline.ops.GeneralOps
import gov.nasa.ammos.aerie.procedural.timeline.ops.NonZeroDurationOps
import gov.nasa.ammos.aerie.procedural.timeline.ops.SerialSegmentOps
import gov.nasa.ammos.aerie.procedural.timeline.payloads.LinearEquation
import gov.nasa.ammos.aerie.procedural.timeline.payloads.Segment
import gov.nasa.ammos.aerie.procedural.timeline.payloads.activities.AnyInstance
import gov.nasa.ammos.aerie.procedural.timeline.payloads.activities.Instance
import gov.nasa.ammos.aerie.procedural.timeline.plan.Plan
import gov.nasa.ammos.aerie.procedural.timeline.plan.SimulationResults
import gov.nasa.jpl.aerie.constraints.tree.RollingThreshold
import gov.nasa.jpl.aerie.json.*
import gov.nasa.jpl.aerie.json.BasicParsers.*
import gov.nasa.jpl.aerie.json.Uncurry.tuple
import gov.nasa.jpl.aerie.json.Uncurry.untuple
import gov.nasa.jpl.aerie.json.Unit
import gov.nasa.jpl.aerie.merlin.driver.json.SerializedValueJsonParser.serializedValueP
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue
import gov.nasa.jpl.aerie.merlin.server.ConstraintRunner.Metadata
import gov.nasa.jpl.aerie.types.ActivityDirectiveId
import gov.nasa.jpl.aerie.types.ActivityInstanceId
import org.apache.commons.lang3.NotImplementedException
import org.apache.commons.lang3.tuple.Pair
import java.util.*
import java.util.function.BiFunction
import java.util.function.Function
import kotlin.Any
import kotlin.Error
import kotlin.Int
import kotlin.String
import kotlin.collections.set
import kotlin.jvm.optionals.getOrNull
import kotlin.let

typealias Spans = Universal<Segment<Metadata?>>

data class ConstraintRunner(private val plan: Plan, private val simResults: SimulationResults) {
  private val intervalAliases: MutableMap<String, Interval> = HashMap()
  private val activityInstanceAliases: MutableMap<String, Instance<AnyInstance>> = HashMap()

  private fun <I, O> errorSerializer(i: I?): O? {
    throw NotImplementedException("Constraints cannot be unparsed")
  }

  data class Metadata(val activityInstance: Instance<AnyInstance>)

  private val inclusivityP: JsonParser<Interval.Inclusivity> = enumP(Interval.Inclusivity::class.java) {
      obj -> obj.name
  }

  private val intervalAliasP: JsonParser<Interval> = productP
    .field("kind", literalP("IntervalAlias"))
    .field("alias", stringP)
    .map(
      untuple { _, alias -> intervalAliases[alias] },
      ::errorSerializer
    )

  private val absoluteIntervalP: JsonParser<Interval> = productP
    .field("kind", literalP("AbsoluteInterval"))
    .optionalField("start", instantP)
    .optionalField("end", instantP)
    .optionalField("startInclusivity", inclusivityP)
    .optionalField("endInclusivity", inclusivityP)
    .map(
      untuple { _, start, end, startInclusivity, endInclusivity ->
        val relativeStart = start.getOrNull()?.let { plan.toRelative(it) } ?: plan.totalBounds().start
        val relativeEnd = end.getOrNull()?.let { plan.toRelative(it) } ?: plan.totalBounds().end
        Interval(
          relativeStart,
          relativeEnd,
          startInclusivity.orElse(plan.totalBounds().startInclusivity),
          endInclusivity.orElse(plan.totalBounds().endInclusivity)
        )
      },
      ::errorSerializer
    )

  private val intervalExpressionP: JsonParser<Interval> = chooseP(intervalAliasP, absoluteIntervalP)

  private fun <V: Any, P: SerialSegmentOps<V, *, P>> assignGapsF(profileParser: JsonParser<P>): JsonParser<P> {
    return productP
      .field("kind", literalP("AssignGapsExpression"))
      .field("originalProfile", profileParser)
      .field("defaultProfile", profileParser)
      .map(
        untuple { _, originalProfile: P, defaultProfile: P -> originalProfile.assignGaps(defaultProfile) },
        ::errorSerializer
      )
  }

  private fun <TL: GeneralOps<*, TL>> shiftByF(profileParser: JsonParser<TL>): JsonParser<TL> {
    return productP
      .field("kind", literalP("ProfileExpressionShiftBy"))
      .field("expression", profileParser)
      .field("duration", durationExprP)
      .map(
        untuple { _, expression, duration -> expression.shift(duration) },
        ::errorSerializer
      )
  }

  private val discreteResourceP: JsonParser<Constants<SerializedValue>> = productP
    .field("kind", literalP("DiscreteProfileResource"))
    .field("name", stringP)
    .map(
      untuple { _, name -> simResults.resource(name, ::Constants) },
      ::errorSerializer
    )

  private val discreteValueP: JsonParser<Constants<SerializedValue>> = productP
    .field("kind", literalP("DiscreteProfileValue"))
    .field("value", serializedValueP)
    .optionalField("interval", intervalExpressionP)
    .map(
      untuple { _, value, interval ->
        val result = Constants(value)
        interval.map(result::select).orElse(result)
      },
      ::errorSerializer
    )

  private val discreteParameterP: JsonParser<Constants<SerializedValue>> = productP
    .field("kind", literalP("DiscreteProfileParameter"))
    .field("alias", stringP)
    .field("name", stringP)
    .map(
      untuple { _, alias , name -> Constants(BaseTimeline(::Constants) { opts ->
        listOf(Segment(opts.bounds, activityInstanceAliases[alias]!!.inner.arguments[name]!!))
      }) },
      ::errorSerializer
    )

  private fun discreteProfileExprF(
    profileExpressionP: JsonParser<out SerialSegmentOps<*, *, *>>,
    spansExpressionP: JsonParser<Spans>
  ): JsonParser<Constants<SerializedValue>> {
    return recursiveP { selfP ->
        chooseP(
          discreteResourceP,
          discreteValueP,
          discreteParameterP,
          assignGapsF(selfP),
          shiftByF(selfP),
          valueAtExpressionF(profileExpressionP, spansExpressionP),
          listExpressionF(profileExpressionP),
          structExpressionF(profileExpressionP)
        )
      }
  }

  private fun <P: SerialSegmentOps<*, *, P>>structExpressionF(profileParser: JsonParser<P>): JsonParser<Constants<SerializedValue>> {
    return productP
      .field("kind", literalP("StructProfileExpression"))
      .field("expressions", mapP(profileParser))
      .map(
        untuple { _, expressions ->
          Constants(
            BaseTimeline(::Constants) { opts ->
              if (!opts.bounds.isPoint()) throw Error("The StructExpressionAt node should be used only with singleton bounds.")
              val result = mutableMapOf<String, SerializedValue>()
              for (key in expressions.keys) {
                result[key] = SerializedValue.ofAny(expressions[key]!!.sample(opts.bounds.start))
              }
              listOf(Segment(opts.bounds, SerializedValue.of(result)))
            }
          )
        },
        ::errorSerializer
      )
  }

  private fun <P: SerialSegmentOps<*, *, P>> listExpressionF(profileParser: JsonParser<P>): JsonParser<Constants<SerializedValue>> {
    return productP
      .field("kind", literalP("ListProfileExpression"))
      .field("expressions", listP(profileParser))
      .map(
        untuple { _, expressions ->
          Constants(
            BaseTimeline(::Constants) { opts: CollectOptions? ->
              if (!opts!!.bounds.isPoint()) throw Error("The StructExpressionAt node should be used only with singleton bounds.")
              val result = mutableListOf<SerializedValue>()
              for (expression in expressions) {
                result.add(SerializedValue.ofAny(expression.sample(opts.bounds.start)))
              }
              listOf(Segment(opts.bounds, SerializedValue.of(result)))
            }
          )
        },
        ::errorSerializer
      )
  }

  private val realResourceP: JsonParser<Real> = productP
    .field("kind", literalP("RealProfileResource"))
    .field("name", stringP)
    .map<Real?>(
      untuple { _, name -> simResults.resource(name, deserializer()) },
      ::errorSerializer
    )

  private val realValueP: JsonParser<Real> = productP
    .field("kind", literalP("RealProfileValue"))
    .field("value", doubleP)
    .field("rate", doubleP)
    .optionalField("interval", intervalExpressionP)
    .map(
      untuple { _, value, rate, interval ->
        val result = Real(
          LinearEquation(
            Duration.ZERO,
            value, rate
          )
        )
        interval.map(result::select).orElse(result)
      },
      ::errorSerializer
    )

  private val realParameterP: JsonParser<Real> = productP
    .field("kind", literalP("RealProfileParameter"))
    .field("alias", stringP)
    .field("name", stringP)
    .map(
      untuple { _, alias , name -> Real(BaseTimeline(::Real) { opts ->
        listOf(Segment(opts.bounds, LinearEquation(activityInstanceAliases[alias]!!.inner.arguments[name]!!.asReal().get())))
      }) },
      ::errorSerializer
    )

  private fun plusF(linearProfileExpressionP: JsonParser<Real>): JsonParser<Real> {
    return productP
      .field("kind", literalP("RealProfilePlus"))
      .field("left", linearProfileExpressionP)
      .field("right", linearProfileExpressionP)
      .map(
        untuple { _, left, right -> left + right },
        ::errorSerializer
      )
  }

  private fun timesF(linearProfileExpressionP: JsonParser<Real>): JsonParser<Real> {
    return productP
      .field("kind", literalP("RealProfileTimes"))
      .field("profile", linearProfileExpressionP)
      .field("multiplier", doubleP)
      .map(
        untuple { _, profile, multiplier -> profile * multiplier },
        ::errorSerializer
      )
  }

  private fun rateF(linearProfileExpressionP: JsonParser<Real>): JsonParser<Real> {
    return productP
      .field("kind", literalP("RealProfileRate"))
      .field("profile", linearProfileExpressionP)
      .map(
        untuple { _, profile -> profile.rate(Duration.SECOND).toReal() },
        ::errorSerializer
      )
  }

  private fun windowsAccumulatedDurationF(booleansExpressionP: JsonParser<Booleans>): JsonParser<Real> {
    return productP
      .field("kind", literalP("RealProfileAccumulatedDuration"))
      .field("intervalsExpression", booleansExpressionP)
      .field("unit", durationExprP)
      .map(
        untuple { _, intervals, unit -> intervals.accumulatedTrueDuration(unit) },
        ::errorSerializer
      )
  }

  private fun spansAccumulatedDurationF(spansExpressionP: JsonParser<Spans>): JsonParser<Real> {
    return productP
      .field("kind", literalP("RealProfileAccumulatedDuration"))
      .field("intervalsExpression", spansExpressionP)
      .field("unit", durationExprP)
      .map(
        untuple { _, intervals, unit -> intervals.accumulatedDuration( unit!! ) },
        ::errorSerializer
      )
  }

  private fun accumulatedDurationF(
    booleansExpressionP: JsonParser<Booleans>,
    spansExpressionP: JsonParser<Spans>
  ): JsonParser<Real> {
    return chooseP(
      windowsAccumulatedDurationF(booleansExpressionP),
      spansAccumulatedDurationF(spansExpressionP)
    )
  }

  private fun linearProfileExprF(
    windowsP: JsonParser<Booleans>,
    spansP: JsonParser<Spans>
  ): JsonParser<Real> {
    return recursiveP { selfP: JsonParser<Real> ->
      chooseP(
        realResourceP,
        realValueP,
        realParameterP,
        plusF(selfP),
        timesF(selfP),
        rateF(selfP),
        assignGapsF(selfP),
        shiftByF(selfP),
        accumulatedDurationF(windowsP, spansP)
      )
    }
  }

  private fun profileExpressionF(
    spansExpressionP: JsonParser<Spans>,
    linearProfileExprP: JsonParser<Real>
  ): JsonParser<SerialSegmentOps<*,*,*>> {
    return recursiveP { selfP ->
      chooseP(
        linearProfileExprP,
        discreteProfileExprF(selfP, spansExpressionP),
        durationExprP.map(
          { Constants(it.micros) },
          ::errorSerializer
        )
      )
    }
  }

  private val intervalDurationP: JsonParser<Duration> = productP
    .field("kind", literalP("IntervalDuration"))
    .field("interval", intervalExpressionP)
    .map(
      untuple { _, interval -> interval.duration() },
      ::errorSerializer
    )

  private val durationP: JsonParser<Duration> = longP
    .map(
      Duration::microseconds,
      ::errorSerializer
    )

  private val durationExprP: JsonParser<Duration> = chooseP(intervalDurationP, durationP)

  private fun transitionP(
    profileExpressionP: JsonParser<out SerialSegmentOps<*,*,*>>,
    spansExpressionP: JsonParser<Spans>
  ): JsonParser<Booleans> {
    return productP
      .field("kind", literalP("DiscreteProfileTransition"))
      .field("profile", discreteProfileExprF(profileExpressionP, spansExpressionP))
      .field("from", serializedValueP)
      .field("to", serializedValueP)
      .map(
        untuple { _, profile, from, to -> profile.transitions(from, to) },
        ::errorSerializer
      )
  }

  private val activityWindowP: JsonParser<Booleans> = productP
    .field("kind", literalP("WindowsExpressionActivityWindow"))
    .field("alias", stringP)
    .map(
      untuple { _, alias -> Booleans(Segment(activityInstanceAliases[alias]!!.interval, true)).assignGaps(false) },
      ::errorSerializer
    )

  private val activitySpanP: JsonParser<Spans> = productP
    .field("kind", literalP("SpansExpressionActivitySpan"))
    .field("alias", stringP)
    .map(
      untuple { _, alias ->
        val activity = activityInstanceAliases[alias]!!
        Universal(Segment(activity.interval, Metadata(activity)))
      },
      ::errorSerializer
    )

  private fun spansSelectWhenTrueF(
    spansP: JsonParser<Spans>,
    windowsP: JsonParser<Booleans>
  ): JsonParser<Spans> {
    return productP
      .field("kind", literalP("SpansSelectWhenTrue"))
      .field("spansExpression", spansP)
      .field("windowsExpression", windowsP)
      .map(
        untuple { _, spans, windows -> spans.filterByWindows(windows.highlightTrue())},
        ::errorSerializer
      )
  }

  private val startOfP: JsonParser<Booleans> = productP
    .field("kind", literalP("WindowsExpressionStartOf"))
    .field("alias", stringP)
    .map(
      untuple { _, alias -> Booleans(Segment(Interval.at(activityInstanceAliases[alias]!!.interval.start), true)).assignGaps(false) },
      ::errorSerializer
    )

  private val endOfP: JsonParser<Booleans> = productP
    .field("kind", literalP("WindowsExpressionEndOf"))
    .field("alias", stringP)
    .map(
      untuple { _, alias -> Booleans(Segment(Interval.at(activityInstanceAliases[alias]!!.interval.end), true)).assignGaps(false) },
      ::errorSerializer
    )

  private fun keepTrueSegmentP(windowsExpressionP: JsonParser<Booleans>): JsonParser<Booleans> {
    return productP
      .field("kind", literalP("WindowsExpressionKeepTrueSegment"))
      .field("expression", windowsExpressionP)
      .field("index", intP)
      .map(
        untuple { _, expression, index ->
          expression.unsafeOperate { opts ->
            val list = collect(opts).toMutableList()
            var counter = 0
            list.map {
              if (it.value) {
                if (counter != index) {
                  return@map it.withNewValue(false)
                }
                counter += 1
              }
              it
            }
          }
        },
        ::errorSerializer
      )
  }

  private val intervalP: JsonParser<Interval> = productP
    .field("start", durationP)
    .field("end", durationP)
    .field("startInclusivity", inclusivityP)
    .field("endInclusivity", inclusivityP)
    .map(
      untuple { start, end, startInclusivity, endInclusivity ->
        between(start, end, startInclusivity, endInclusivity)
      },
      ::errorSerializer
    )

  private val violationP: JsonParser<Violation> = productP
    .field("windows", listP(intervalP))
    .field("activityInstanceIds", listP(longP))
    .map(::errorSerializer) { v -> tuple(
      listOf(v.interval),
      v.ids.map { when (it) {
        is ActivityInstanceId -> it.id()
        else -> throw IllegalStateException("eDSL constraints can't refer to directives")
      } }
    ) }

  val constraintResultP: JsonParser<ConstraintResult> = productP
    .field("violations", listP(violationP))
    .field("constraintId", longP)
    .field("constraintRevision", longP)
    .field("constraintName", stringP)
    .map( ::errorSerializer ) {
      tuple(
        it.violations,
        it.constraintId,
        it.constraintRevision,
        it.constraintName
      )
    }

  private fun <T> optionalRangeF(boundP: JsonParser<T>): JsonParser<Pair<Optional<T>, Optional<T>>> {
    return productP
      .optionalField("min", boundP)
      .optionalField("max", boundP)
  }

  private fun spansContainsF(spansExpressionP: JsonParser<Spans>): JsonObjectParser<Booleans> {
    data class Requirement(
      val minCount: Optional<Int>,
      val maxCount: Optional<Int>,
      val minDur: Optional<Duration>,
      val maxDur: Optional<Duration>
    )

    val requirementP: JsonParser<Requirement> = productP
      .field("count", optionalRangeF<Int>(intP))
      .field("duration", optionalRangeF(durationExprP))
      .map(
        untuple { count, duration ->
          Requirement( count.left, count.right, duration!!.left, duration.right )
        },
        ::errorSerializer
      )
    return productP
      .field("kind", literalP("SpansExpressionContains"))
      .field("parents", spansExpressionP)
      .field("children", spansExpressionP)
      .field("requirement", requirementP)
      .map(
        untuple { _, parents, children, requirement -> TODO() },
        ::errorSerializer
      )
  }

  private val windowsValueP: JsonParser<Booleans> = productP
    .field("kind", literalP("WindowsExpressionValue"))
    .field("value", boolP)
    .optionalField("interval", intervalExpressionP)
    .map(
      untuple { _, value, interval ->
        val result = Booleans(value)
        interval.map(result::select).orElse(result)
      },
      ::errorSerializer
    )

  private fun windowsShiftEdgesF(windowsExpressionP: JsonParser<Booleans>): JsonParser<Booleans> {
    return productP
      .field("kind", literalP("IntervalsExpressionShiftEdges"))
      .field("expression", windowsExpressionP)
      .field("fromStart", durationExprP)
      .field("fromEnd", durationExprP)
      .map(
        untuple { _, expression, fromStart, fromEnd ->
          expression.shiftEdges(fromStart, fromEnd)
        },
        ::errorSerializer
      )
  }

  private fun spansShiftEdgesF(spansExpressionP: JsonParser<Spans>): JsonParser<Spans> {
    return productP
      .field("kind", literalP("IntervalsExpressionShiftEdges"))
      .field("expression", spansExpressionP)
      .field("fromStart", durationExprP)
      .field("fromEnd", durationExprP)
      .map(
        untuple { _, expression, fromStart, fromEnd ->
          expression.shiftEndpoints(fromStart, fromEnd)
        },
        ::errorSerializer
      )
  }

  private fun <V: Any, P: SerialSegmentOps<V,*,P>> equalF(expressionParser: JsonParser<P>): JsonParser<Booleans> {
    return productP
      .field("kind", literalP("ExpressionEqual"))
      .field("left", expressionParser)
      .field("right", expressionParser)
      .map(
        untuple { _, left, right ->
          left equalTo right
        },
        ::errorSerializer
      )
  }

  private fun <V: Any, P: SerialSegmentOps<V,*,P>> notEqualF(expressionParser: JsonParser<P>): JsonParser<Booleans> {
    return productP
      .field("kind", literalP("ExpressionNotEqual"))
      .field("left", expressionParser)
      .field("right", expressionParser)
      .map(
        untuple { _, left, right ->
          left notEqualTo right
        },
        ::errorSerializer
      )
  }

  private fun lessThanF(linearProfileExpressionP: JsonParser<Real>): JsonParser<Booleans> {
    return productP
      .field("kind", literalP("RealProfileLessThan"))
      .field("left", linearProfileExpressionP)
      .field("right", linearProfileExpressionP)
      .map(
        untuple { _, left, right -> left lessThan right },
        ::errorSerializer
      )
  }
  private fun lessThanOrEqualF(linearProfileExpressionP: JsonParser<Real>): JsonParser<Booleans> {
    return productP
      .field("kind", literalP("RealProfileLessThanOrEqual"))
      .field("left", linearProfileExpressionP)
      .field("right", linearProfileExpressionP)
      .map(
        untuple { _, left, right -> left lessThanOrEqualTo right },
        ::errorSerializer
      )
  }
  private fun greaterThanF(linearProfileExpressionP: JsonParser<Real>): JsonParser<Booleans> {
    return productP
      .field("kind", literalP("RealProfileGreaterThan"))
      .field("left", linearProfileExpressionP)
      .field("right", linearProfileExpressionP)
      .map(
        untuple { _, left, right -> left greaterThan right },
        ::errorSerializer
      )
  }
  private fun greaterThanOrEqualF(linearProfileExpressionP: JsonParser<Real>): JsonParser<Booleans> {
    return productP
      .field("kind", literalP("RealProfileGreaterThanOrEqual"))
      .field("left", linearProfileExpressionP)
      .field("right", linearProfileExpressionP)
      .map(
        untuple { _, left, right -> left greaterThanOrEqualTo right },
        ::errorSerializer
      )
  }

  private fun longerThanP(windowsExpressionP: JsonParser<Booleans>): JsonParser<Booleans> {
    return productP
      .field("kind", literalP("WindowsExpressionLongerThan"))
      .field("windowExpression", windowsExpressionP)
      .field("duration", durationExprP)
      .map(
        untuple { _, windowsExpression, duration -> windowsExpression.falsifyShorterThan(duration) },
        ::errorSerializer
      )
  }

  private fun shorterThanP(windowsExpressionP: JsonParser<Booleans>): JsonParser<Booleans> {
    return productP
      .field("kind", literalP("WindowsExpressionShorterThan"))
      .field("windowExpression", windowsExpressionP)
      .field("duration", durationExprP)
      .map(
        untuple { _, windowsExpression, duration -> windowsExpression.falsifyLongerThan(duration) },
        ::errorSerializer
      )
  }

  private fun andF(windowsExpressionP: JsonParser<Booleans>): JsonParser<Booleans> {
    return productP
      .field("kind", literalP("WindowsExpressionAnd"))
      .field("expressions", listP(windowsExpressionP))
      .map(
        untuple { _, expressions -> expressions.fold(Booleans(true)) { acc, new -> acc and new } },
        ::errorSerializer
      )
  }

  private fun orF(windowsExpressionP: JsonParser<Booleans>): JsonParser<Booleans> {
    return productP
      .field("kind", literalP("WindowsExpressionOr"))
      .field("expressions", listP(windowsExpressionP))
      .map(
        untuple { _, expressions -> expressions.fold(Booleans(false)) { acc, new -> acc or new } },
        ::errorSerializer
      )
  }

  private fun notF(windowsExpressionP: JsonParser<Booleans>): JsonParser<Booleans> {
    return productP
      .field("kind", literalP("WindowsExpressionNot"))
      .field("expression", windowsExpressionP)
      .map(
        untuple { _, expression -> !expression },
        ::errorSerializer
      )
  }

  private fun windowsStartsF(windowsExpressionP: JsonParser<Booleans>): JsonParser<Booleans> {
    return productP
      .field("kind", literalP("IntervalsExpressionStarts"))
      .field("expression", windowsExpressionP)
      .map(
        untuple { _, expression -> expression.risingEdges() },
        ::errorSerializer
      )
  }

  private fun windowsEndsF(windowsExpressionP: JsonParser<Booleans>): JsonParser<Booleans> {
    return productP
      .field("kind", literalP("IntervalsExpressionEnds"))
      .field("expression", windowsExpressionP)
      .map(
        untuple { _, expression -> expression.fallingEdges() },
        ::errorSerializer
      )
  }

  private fun spansStartsF(spansExpressionP: JsonParser<Spans>): JsonParser<Spans> {
    return productP
      .field("kind", literalP("IntervalsExpressionStarts"))
      .field("expression", spansExpressionP)
      .map(
        untuple { _, expression -> expression.starts() },
        ::errorSerializer
      )
  }

  private fun spansEndsF(spansExpressionP: JsonParser<Spans>): JsonParser<Spans> {
    return productP
      .field("kind", literalP("IntervalsExpressionEnds"))
      .field("expression", spansExpressionP)
      .map(
        untuple { _, expression -> expression.ends() },
        ::errorSerializer
      )
  }

  private fun spansSplitF(intervalExpressionP: JsonParser<Spans>): JsonParser<Spans> {
    return productP
      .field("kind", literalP("SpansExpressionSplit"))
      .field("intervals", intervalExpressionP)
      .field("numberOfSubIntervals", intP)
      .map(
        untuple { _, expr, numberOfSubWindows -> expr.split { numberOfSubWindows } },
        ::errorSerializer
      )
  }

  private fun windowsSplitF(intervalExpressionP: JsonParser<Booleans>): JsonParser<Spans> {
    return productP
      .field("kind", literalP("SpansExpressionSplit"))
      .field("intervals", intervalExpressionP)
      .field("numberOfSubIntervals", intP)
      .map(
        untuple { _, expr, numberOfSubWindows ->
          expr.filter { it.value }.unsafeMap(::Universal, BoundsTransformer.IDENTITY, false) {
            it.withNewValue(null as Metadata?)
          }
            .split { numberOfSubWindows } },
        ::errorSerializer
      )
  }

  private fun windowsFromSpansF(spansExpressionP: JsonParser<Spans>): JsonParser<Booleans> {
    return productP
      .field("kind", literalP("WindowsExpressionFromSpans"))
      .field("spansExpression", spansExpressionP)
      .map(
        untuple { _, spans -> spans.active().assignGaps(false) },
        ::errorSerializer
      )
  }

  private fun forEachActivitySpansF(spansExpressionP: JsonParser<Spans>): JsonParser<Spans> {
    return productP
      .field("kind", literalP("ForEachActivitySpans"))
      .field("activityType", stringP)
      .field("alias", stringP)
      .field("expression", spansExpressionP)
      .map(
        untuple { _, type, alias, expression ->
          val result = mutableListOf<Segment<Metadata?>>()
          for (activity in simResults.instances(type)) {
            activityInstanceAliases[alias] = activity
            result.addAll(expression.collect(plan.totalBounds()))
            activityInstanceAliases.remove(alias)
          }
          Spans(result)
        },
        ::errorSerializer
      )
  }

  private fun forEachActivityViolationsF(violationListExpressionP: JsonParser<Violations>): JsonParser<Violations> {
    return productP
      .field("kind", literalP("ForEachActivityViolations"))
      .field("activityType", stringP)
      .field("alias", stringP)
      .field("expression", violationListExpressionP)
      .map(
        untuple { _, type, alias, expression ->
          val result = mutableListOf<Violation>()
          for (activity in simResults.instances(type)) {
            activityInstanceAliases[alias] = activity
            result.addAll(expression.collect(plan.totalBounds()))
            activityInstanceAliases.remove(alias)
          }
          Violations(result)
        },
        ::errorSerializer
      )
  }

  private fun <P: SerialSegmentOps<*, *, P>> valueAtExpressionF(
    profileExpressionP: JsonParser<P>,
    spansExpressionP: JsonParser<Spans>
  ): JsonParser<Constants<SerializedValue>> {
    return productP
      .field("kind", literalP("ValueAtExpression"))
      .field("profile", profileExpressionP)
      .field("timepoint", spansExpressionP)
      .map(
        untuple { _, profile, timepoint ->
          val trues = timepoint.collect(plan.totalBounds())
          if (trues.size != 1 || !trues.first().interval.isPoint()) throw Error("spans input to valueAt must be a single timepoint")
          val time = trues.first().interval.start
          Constants(SerializedValue.ofAny(profile.sample(time)))
        },
        ::errorSerializer
      )
  }

  private fun changesF(
    spansExpressionP: JsonParser<Spans>,
    linearProfileExprP: JsonParser<Real>
  ): JsonParser<Booleans> {
    return productP
      .field("kind", literalP("ProfileChanges"))
      .field("expression", profileExpressionF(spansExpressionP, linearProfileExprP))
      .map(
        untuple { _, expression -> expression.changes() },
        ::errorSerializer
      )
  }

  private fun windowsExpressionF(
    spansP: JsonParser<Spans>,
    linearProfileExprP: JsonParser<Real>
  ): JsonParser<Booleans> {
    return recursiveP { selfP ->
      chooseP(
        windowsValueP,
        startOfP,
        endOfP,
        changesF(spansP, linearProfileExprP),
        lessThanF(linearProfileExprP),
        lessThanOrEqualF(linearProfileExprP),
        greaterThanF(linearProfileExprP),
        greaterThanOrEqualF(linearProfileExprP),
        longerThanP(selfP),
        shorterThanP(selfP),
        transitionP(profileExpressionF(spansP, linearProfileExprP), spansP),
        equalF(linearProfileExprP),
        equalF(
          discreteProfileExprF(
            profileExpressionF(spansP, linearProfileExprP),
            spansP
          )
        ),
        notEqualF(linearProfileExprP),
        notEqualF(
          discreteProfileExprF(
            profileExpressionF(
              spansP,
              linearProfileExprP
            ), spansP
          )
        ),
        andF(selfP),
        orF(selfP),
        notF(selfP),
        windowsShiftEdgesF(selfP),
        windowsStartsF(selfP),
        windowsEndsF(selfP),
        windowsFromSpansF(spansP),
        activityWindowP,
        assignGapsF(selfP),
        shiftByF(selfP),
        keepTrueSegmentP(selfP),
        spansContainsF(spansP)
      )
    }
  }

  private fun spansFromWindowsF(windowsExpressionP: JsonParser<Booleans>): JsonParser<Spans> {
    return productP
      .field("kind", literalP("SpansExpressionFromWindows"))
      .field("windowsExpression", windowsExpressionP)
      .map(
        untuple { _, expression -> expression.unsafeMap(::Universal, BoundsTransformer.IDENTITY, false) {
          it.withNewValue(null)
        } },
        ::errorSerializer
      )
  }

  private fun connectTo(spansExpressionP: JsonParser<Spans>): JsonParser<Spans> {
    return productP
      .field("kind", literalP("SpansExpressionConnectTo"))
      .field("from", spansExpressionP)
      .field("to", spansExpressionP)
      .map(
        untuple { _, from, to ->
         from.connectTo(to, false)
           .unsafeMap(::Universal, BoundsTransformer.IDENTITY, false) {
             Segment(it.interval, it.from?.value)
           }
        },
        ::errorSerializer
      )
  }

  private val spansIntervalP: JsonParser<Spans> = productP
    .field("kind", literalP("SpansExpressionInterval"))
    .field("interval", intervalExpressionP)
    .map(
      untuple { _, interval -> Spans(Segment(interval, null)) },
      ::errorSerializer
    )

  private fun spansExpressionF(windowsP: JsonParser<Booleans>): JsonParser<Spans> {
    return recursiveP { selfP ->
      chooseP(
        spansIntervalP,
        spansStartsF(selfP),
        spansEndsF(selfP),
        connectTo(selfP),
        spansShiftEdgesF(selfP),
        spansSplitF(selfP),
        windowsSplitF(windowsP),
        spansFromWindowsF(windowsP),
        forEachActivitySpansF(selfP),
        activitySpanP,
        spansSelectWhenTrueF(selfP, windowsP)
      )
    }
  }

  private val windowsExpressionP: JsonParser<Booleans> =
    recursiveP { selfP ->
      windowsExpressionF(
        spansExpressionF(selfP),
        linearProfileExprF(selfP, spansExpressionF(selfP))
      )
    }
  private val linearProfileExprP: JsonParser<Real> = linearProfileExprF(
    windowsExpressionP,
    spansExpressionF(windowsExpressionP)
  )
  private val spansExpressionP: JsonParser<Spans> =
    recursiveP { selfP ->
      spansExpressionF( windowsExpressionF(selfP, linearProfileExprP) )
    }
  private val profileExpressionP: JsonParser<SerialSegmentOps<*,*,*>> = profileExpressionF(
    spansExpressionP, linearProfileExprP
  )
  private val discreteProfileExprP: JsonParser<Constants<SerializedValue>> = discreteProfileExprF(
    profileExpressionP, spansExpressionP
  )

  private val violationsOfP: JsonParser<Violations> = productP
    .field("kind", literalP("ViolationsOf"))
    .field("expression", windowsExpressionP)
    .map(
      untuple { _, expression -> expression.violateOn(false) },
      ::errorSerializer
    )

  private val rollingThresholdAlgorithmP: JsonParser<RollingThreshold.RollingThresholdAlgorithm> = enumP(
    RollingThreshold.RollingThresholdAlgorithm::class.java
  ) { obj -> obj.name }

  private val rollingThresholdP: JsonParser<Violations> = productP
    .field("kind", literalP("RollingThreshold"))
    .field("spans", spansExpressionP)
    .field("width", durationExprP)
    .field("threshold", durationExprP)
    .field("algorithm", rollingThresholdAlgorithmP)
    .map(
      untuple { _, spans, width, threshold, algorithm -> TODO() },
      ::errorSerializer
    )

  val constraintP: JsonParser<Violations> =
    recursiveP { selfP ->
      chooseP(
        forEachActivityViolationsF(selfP),
        windowsExpressionP.map(
          { expression -> expression.violateOn(false) },
          ::errorSerializer
        ),
        violationsOfP,
        rollingThresholdP
      )
    }
}
