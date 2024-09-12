package gov.nasa.jpl.aerie.merlin.server.services.constraints

import gov.nasa.ammos.aerie.procedural.constraints.Violation
import gov.nasa.ammos.aerie.procedural.constraints.Violations
import gov.nasa.ammos.aerie.procedural.constraints.Violations.Companion.violateOn
import gov.nasa.ammos.aerie.procedural.timeline.BaseTimeline
import gov.nasa.ammos.aerie.procedural.timeline.BoundsTransformer
import gov.nasa.ammos.aerie.procedural.timeline.CollectOptions
import gov.nasa.ammos.aerie.procedural.timeline.Interval
import gov.nasa.ammos.aerie.procedural.timeline.collections.Universal
import gov.nasa.ammos.aerie.procedural.timeline.collections.profiles.Booleans
import gov.nasa.ammos.aerie.procedural.timeline.collections.profiles.Constants
import gov.nasa.ammos.aerie.procedural.timeline.collections.profiles.Real
import gov.nasa.ammos.aerie.procedural.timeline.ops.GeneralOps
import gov.nasa.ammos.aerie.procedural.timeline.ops.SerialSegmentOps
import gov.nasa.ammos.aerie.procedural.timeline.payloads.LinearEquation
import gov.nasa.ammos.aerie.procedural.timeline.payloads.Segment
import gov.nasa.ammos.aerie.procedural.timeline.payloads.activities.AnyInstance
import gov.nasa.ammos.aerie.procedural.timeline.payloads.activities.Instance
import gov.nasa.ammos.aerie.procedural.timeline.plan.Plan
import gov.nasa.ammos.aerie.procedural.timeline.plan.SimulationResults
import gov.nasa.jpl.aerie.json.BasicParsers
import gov.nasa.jpl.aerie.json.JsonObjectParser
import gov.nasa.jpl.aerie.json.JsonParser
import gov.nasa.jpl.aerie.json.Uncurry
import gov.nasa.jpl.aerie.merlin.driver.json.SerializedValueJsonParser
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue
import gov.nasa.jpl.aerie.merlin.server.services.constraints.ConstraintRunner.Metadata
import org.apache.commons.lang3.NotImplementedException
import org.apache.commons.lang3.tuple.Pair
import java.util.*
import kotlin.jvm.optionals.getOrNull

typealias Spans = Universal<Segment<Metadata?>>

data class ConstraintRunner(private val plan: Plan, private val simResults: SimulationResults) {
  private val intervalAliases: MutableMap<String, Interval> = HashMap()
  private val activityInstanceAliases: MutableMap<String, Instance<AnyInstance>> = HashMap()

  private fun <I, O> errorSerializer(i: I?): O? {
    throw NotImplementedException("Constraints cannot be unparsed")
  }

  data class Metadata(val activityInstance: Instance<AnyInstance>)



  private val intervalAliasP: JsonParser<Interval> = BasicParsers.productP
    .field("kind", BasicParsers.literalP("IntervalAlias"))
    .field("alias", BasicParsers.stringP)
    .map(
        Uncurry.untuple { _, alias -> intervalAliases[alias] },
      ::errorSerializer
    )

  private val absoluteIntervalP: JsonParser<Interval> = BasicParsers.productP
    .field("kind", BasicParsers.literalP("AbsoluteInterval"))
    .optionalField("start", BasicParsers.instantP)
    .optionalField("end", BasicParsers.instantP)
    .optionalField("startInclusivity", ConstraintResultParser.inclusivityP)
    .optionalField("endInclusivity", ConstraintResultParser.inclusivityP)
    .map(
        Uncurry.untuple { _, start, end, startInclusivity, endInclusivity ->
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

  private val intervalExpressionP: JsonParser<Interval> = BasicParsers.chooseP(intervalAliasP, absoluteIntervalP)

  private fun <V: Any, P: SerialSegmentOps<V, *, P>> assignGapsF(profileParser: JsonParser<P>): JsonParser<P> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("AssignGapsExpression"))
      .field("originalProfile", profileParser)
      .field("defaultProfile", profileParser)
      .map(
          Uncurry.untuple { _, originalProfile: P, defaultProfile: P -> originalProfile.assignGaps(defaultProfile) },
        ::errorSerializer
      )
  }

  private fun <TL: GeneralOps<*, TL>> shiftByF(profileParser: JsonParser<TL>): JsonParser<TL> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("ProfileExpressionShiftBy"))
      .field("expression", profileParser)
      .field("duration", durationExprP)
      .map(
          Uncurry.untuple { _, expression, duration -> expression.shift(duration) },
        ::errorSerializer
      )
  }

  private val discreteResourceP: JsonParser<Constants<SerializedValue>> = BasicParsers.productP
    .field("kind", BasicParsers.literalP("DiscreteProfileResource"))
    .field("name", BasicParsers.stringP)
    .map(
        Uncurry.untuple { _, name -> simResults.resource(name, ::Constants) },
      ::errorSerializer
    )

  private val discreteValueP: JsonParser<Constants<SerializedValue>> = BasicParsers.productP
    .field("kind", BasicParsers.literalP("DiscreteProfileValue"))
    .field("value", SerializedValueJsonParser.serializedValueP)
    .optionalField("interval", intervalExpressionP)
    .map(
        Uncurry.untuple { _, value, interval ->
            val result = Constants(value)
            interval.map(result::select).orElse(result)
        },
      ::errorSerializer
    )

  private val discreteParameterP: JsonParser<Constants<SerializedValue>> = BasicParsers.productP
    .field("kind", BasicParsers.literalP("DiscreteProfileParameter"))
    .field("alias", BasicParsers.stringP)
    .field("name", BasicParsers.stringP)
    .map(
        Uncurry.untuple { _, alias, name ->
            Constants(BaseTimeline(::Constants) { opts ->
                listOf(Segment(opts.bounds, activityInstanceAliases[alias]!!.inner.arguments[name]!!))
            })
        },
      ::errorSerializer
    )

  private fun discreteProfileExprF(
      profileExpressionP: JsonParser<out SerialSegmentOps<*, *, *>>,
      spansExpressionP: JsonParser<Spans>
  ): JsonParser<Constants<SerializedValue>> {
    return BasicParsers.recursiveP { selfP ->
        BasicParsers.chooseP(
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
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("StructProfileExpression"))
      .field("expressions", BasicParsers.mapP(profileParser))
      .map(
          Uncurry.untuple { _, expressions ->
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
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("ListProfileExpression"))
      .field("expressions", BasicParsers.listP(profileParser))
      .map(
          Uncurry.untuple { _, expressions ->
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

  private val realResourceP: JsonParser<Real> = BasicParsers.productP
    .field("kind", BasicParsers.literalP("RealProfileResource"))
    .field("name", BasicParsers.stringP)
    .map<Real?>(
        Uncurry.untuple { _, name -> simResults.resource(name, Real.deserializer()) },
      ::errorSerializer
    )

  private val realValueP: JsonParser<Real> = BasicParsers.productP
    .field("kind", BasicParsers.literalP("RealProfileValue"))
    .field("value", BasicParsers.doubleP)
    .field("rate", BasicParsers.doubleP)
    .optionalField("interval", intervalExpressionP)
    .map(
        Uncurry.untuple { _, value, rate, interval ->
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

  private val realParameterP: JsonParser<Real> = BasicParsers.productP
    .field("kind", BasicParsers.literalP("RealProfileParameter"))
    .field("alias", BasicParsers.stringP)
    .field("name", BasicParsers.stringP)
    .map(
        Uncurry.untuple { _, alias, name ->
            Real(BaseTimeline(::Real) { opts ->
                listOf(
                    Segment(
                        opts.bounds,
                        LinearEquation(activityInstanceAliases[alias]!!.inner.arguments[name]!!.asReal().get())
                    )
                )
            })
        },
      ::errorSerializer
    )

  private fun plusF(linearProfileExpressionP: JsonParser<Real>): JsonParser<Real> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("RealProfilePlus"))
      .field("left", linearProfileExpressionP)
      .field("right", linearProfileExpressionP)
      .map(
          Uncurry.untuple { _, left, right -> left + right },
        ::errorSerializer
      )
  }

  private fun timesF(linearProfileExpressionP: JsonParser<Real>): JsonParser<Real> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("RealProfileTimes"))
      .field("profile", linearProfileExpressionP)
      .field("multiplier", BasicParsers.doubleP)
      .map(
          Uncurry.untuple { _, profile, multiplier -> profile * multiplier },
        ::errorSerializer
      )
  }

  private fun rateF(linearProfileExpressionP: JsonParser<Real>): JsonParser<Real> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("RealProfileRate"))
      .field("profile", linearProfileExpressionP)
      .map(
          Uncurry.untuple { _, profile -> profile.rate(Duration.SECOND).toReal() },
        ::errorSerializer
      )
  }

  private fun windowsAccumulatedDurationF(booleansExpressionP: JsonParser<Booleans>): JsonParser<Real> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("RealProfileAccumulatedDuration"))
      .field("intervalsExpression", booleansExpressionP)
      .field("unit", durationExprP)
      .map(
          Uncurry.untuple { _, intervals, unit -> intervals.accumulatedTrueDuration(unit) },
        ::errorSerializer
      )
  }

  private fun spansAccumulatedDurationF(spansExpressionP: JsonParser<Spans>): JsonParser<Real> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("RealProfileAccumulatedDuration"))
      .field("intervalsExpression", spansExpressionP)
      .field("unit", durationExprP)
      .map(
          Uncurry.untuple { _, intervals, unit -> intervals.accumulatedDuration(unit!!) },
        ::errorSerializer
      )
  }

  private fun accumulatedDurationF(
      booleansExpressionP: JsonParser<Booleans>,
      spansExpressionP: JsonParser<Spans>
  ): JsonParser<Real> {
    return BasicParsers.chooseP(
        windowsAccumulatedDurationF(booleansExpressionP),
        spansAccumulatedDurationF(spansExpressionP)
    )
  }

  private fun linearProfileExprF(
      windowsP: JsonParser<Booleans>,
      spansP: JsonParser<Spans>
  ): JsonParser<Real> {
    return BasicParsers.recursiveP { selfP: JsonParser<Real> ->
        BasicParsers.chooseP(
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
  ): JsonParser<SerialSegmentOps<*, *, *>> {
    return BasicParsers.recursiveP { selfP ->
        BasicParsers.chooseP(
            linearProfileExprP,
            discreteProfileExprF(selfP, spansExpressionP),
            durationExprP.map(
                { Constants(it.micros) },
                ::errorSerializer
            )
        )
    }
  }

  private val intervalDurationP: JsonParser<Duration> = BasicParsers.productP
    .field("kind", BasicParsers.literalP("IntervalDuration"))
    .field("interval", intervalExpressionP)
    .map(
        Uncurry.untuple { _, interval -> interval.duration() },
      ::errorSerializer
    )

  private val durationP: JsonParser<Duration> = BasicParsers.longP
    .map(
        Duration::microseconds,
      ::errorSerializer
    )

  private val durationExprP: JsonParser<Duration> = BasicParsers.chooseP(intervalDurationP, durationP)

  private fun transitionP(
      profileExpressionP: JsonParser<out SerialSegmentOps<*, *, *>>,
      spansExpressionP: JsonParser<Spans>
  ): JsonParser<Booleans> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("DiscreteProfileTransition"))
      .field("profile", discreteProfileExprF(profileExpressionP, spansExpressionP))
      .field("from", SerializedValueJsonParser.serializedValueP)
      .field("to", SerializedValueJsonParser.serializedValueP)
      .map(
          Uncurry.untuple { _, profile, from, to -> profile.transitions(from, to) },
        ::errorSerializer
      )
  }

  private val activityWindowP: JsonParser<Booleans> = BasicParsers.productP
    .field("kind", BasicParsers.literalP("WindowsExpressionActivityWindow"))
    .field("alias", BasicParsers.stringP)
    .map(
        Uncurry.untuple { _, alias ->
            Booleans(Segment(activityInstanceAliases[alias]!!.interval, true)).assignGaps(
                false
            )
        },
      ::errorSerializer
    )

  private val activitySpanP: JsonParser<Spans> = BasicParsers.productP
    .field("kind", BasicParsers.literalP("SpansExpressionActivitySpan"))
    .field("alias", BasicParsers.stringP)
    .map(
        Uncurry.untuple { _, alias ->
            val activity = activityInstanceAliases[alias]!!
            Universal(Segment(activity.interval, Metadata(activity)))
        },
      ::errorSerializer
    )

  private fun spansSelectWhenTrueF(
    spansP: JsonParser<Spans>,
    windowsP: JsonParser<Booleans>
  ): JsonParser<Spans> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("SpansSelectWhenTrue"))
      .field("spansExpression", spansP)
      .field("windowsExpression", windowsP)
      .map(
          Uncurry.untuple { _, spans, windows -> spans.filterByWindows(windows.highlightTrue()) },
        ::errorSerializer
      )
  }

  private val startOfP: JsonParser<Booleans> = BasicParsers.productP
    .field("kind", BasicParsers.literalP("WindowsExpressionStartOf"))
    .field("alias", BasicParsers.stringP)
    .map(
        Uncurry.untuple { _, alias ->
            Booleans(
                Segment(
                    Interval.at(activityInstanceAliases[alias]!!.interval.start),
                    true
                )
            ).assignGaps(false)
        },
      ::errorSerializer
    )

  private val endOfP: JsonParser<Booleans> = BasicParsers.productP
    .field("kind", BasicParsers.literalP("WindowsExpressionEndOf"))
    .field("alias", BasicParsers.stringP)
    .map(
        Uncurry.untuple { _, alias ->
            Booleans(
                Segment(
                    Interval.at(activityInstanceAliases[alias]!!.interval.end),
                    true
                )
            ).assignGaps(false)
        },
      ::errorSerializer
    )

  private fun keepTrueSegmentP(windowsExpressionP: JsonParser<Booleans>): JsonParser<Booleans> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("WindowsExpressionKeepTrueSegment"))
      .field("expression", windowsExpressionP)
      .field("index", BasicParsers.intP)
      .map(
          Uncurry.untuple { _, expression, index ->
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

  private fun <T> optionalRangeF(boundP: JsonParser<T>): JsonParser<Pair<Optional<T>, Optional<T>>> {
    return BasicParsers.productP
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

    val requirementP: JsonParser<Requirement> = BasicParsers.productP
      .field("count", optionalRangeF<Int>(BasicParsers.intP))
      .field("duration", optionalRangeF(durationExprP))
      .map(
          Uncurry.untuple { count, duration ->
              Requirement(count.left, count.right, duration!!.left, duration.right)
          },
        ::errorSerializer
      )
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("SpansExpressionContains"))
      .field("parents", spansExpressionP)
      .field("children", spansExpressionP)
      .field("requirement", requirementP)
      .map(
          Uncurry.untuple { _, parents, children, requirement -> TODO() },
        ::errorSerializer
      )
  }

  private val windowsValueP: JsonParser<Booleans> = BasicParsers.productP
    .field("kind", BasicParsers.literalP("WindowsExpressionValue"))
    .field("value", BasicParsers.boolP)
    .optionalField("interval", intervalExpressionP)
    .map(
        Uncurry.untuple { _, value, interval ->
            val result = Booleans(value)
            interval.map(result::select).orElse(result)
        },
      ::errorSerializer
    )

  private fun windowsShiftEdgesF(windowsExpressionP: JsonParser<Booleans>): JsonParser<Booleans> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("IntervalsExpressionShiftEdges"))
      .field("expression", windowsExpressionP)
      .field("fromStart", durationExprP)
      .field("fromEnd", durationExprP)
      .map(
          Uncurry.untuple { _, expression, fromStart, fromEnd ->
              expression.shiftEdges(fromStart, fromEnd)
          },
        ::errorSerializer
      )
  }

  private fun spansShiftEdgesF(spansExpressionP: JsonParser<Spans>): JsonParser<Spans> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("IntervalsExpressionShiftEdges"))
      .field("expression", spansExpressionP)
      .field("fromStart", durationExprP)
      .field("fromEnd", durationExprP)
      .map(
          Uncurry.untuple { _, expression, fromStart, fromEnd ->
              expression.shiftEndpoints(fromStart, fromEnd)
          },
        ::errorSerializer
      )
  }

  private fun <V: Any, P: SerialSegmentOps<V, *, P>> equalF(expressionParser: JsonParser<P>): JsonParser<Booleans> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("ExpressionEqual"))
      .field("left", expressionParser)
      .field("right", expressionParser)
      .map(
          Uncurry.untuple { _, left, right ->
              left equalTo right
          },
        ::errorSerializer
      )
  }

  private fun <V: Any, P: SerialSegmentOps<V, *, P>> notEqualF(expressionParser: JsonParser<P>): JsonParser<Booleans> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("ExpressionNotEqual"))
      .field("left", expressionParser)
      .field("right", expressionParser)
      .map(
          Uncurry.untuple { _, left, right ->
              left notEqualTo right
          },
        ::errorSerializer
      )
  }

  private fun lessThanF(linearProfileExpressionP: JsonParser<Real>): JsonParser<Booleans> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("RealProfileLessThan"))
      .field("left", linearProfileExpressionP)
      .field("right", linearProfileExpressionP)
      .map(
          Uncurry.untuple { _, left, right -> left lessThan right },
        ::errorSerializer
      )
  }
  private fun lessThanOrEqualF(linearProfileExpressionP: JsonParser<Real>): JsonParser<Booleans> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("RealProfileLessThanOrEqual"))
      .field("left", linearProfileExpressionP)
      .field("right", linearProfileExpressionP)
      .map(
          Uncurry.untuple { _, left, right -> left lessThanOrEqualTo right },
        ::errorSerializer
      )
  }
  private fun greaterThanF(linearProfileExpressionP: JsonParser<Real>): JsonParser<Booleans> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("RealProfileGreaterThan"))
      .field("left", linearProfileExpressionP)
      .field("right", linearProfileExpressionP)
      .map(
          Uncurry.untuple { _, left, right -> left greaterThan right },
        ::errorSerializer
      )
  }
  private fun greaterThanOrEqualF(linearProfileExpressionP: JsonParser<Real>): JsonParser<Booleans> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("RealProfileGreaterThanOrEqual"))
      .field("left", linearProfileExpressionP)
      .field("right", linearProfileExpressionP)
      .map(
          Uncurry.untuple { _, left, right -> left greaterThanOrEqualTo right },
        ::errorSerializer
      )
  }

  private fun longerThanP(windowsExpressionP: JsonParser<Booleans>): JsonParser<Booleans> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("WindowsExpressionLongerThan"))
      .field("windowExpression", windowsExpressionP)
      .field("duration", durationExprP)
      .map(
          Uncurry.untuple { _, windowsExpression, duration -> windowsExpression.falsifyShorterThan(duration) },
        ::errorSerializer
      )
  }

  private fun shorterThanP(windowsExpressionP: JsonParser<Booleans>): JsonParser<Booleans> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("WindowsExpressionShorterThan"))
      .field("windowExpression", windowsExpressionP)
      .field("duration", durationExprP)
      .map(
          Uncurry.untuple { _, windowsExpression, duration -> windowsExpression.falsifyLongerThan(duration) },
        ::errorSerializer
      )
  }

  private fun andF(windowsExpressionP: JsonParser<Booleans>): JsonParser<Booleans> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("WindowsExpressionAnd"))
      .field("expressions", BasicParsers.listP(windowsExpressionP))
      .map(
          Uncurry.untuple { _, expressions -> expressions.fold(Booleans(true)) { acc, new -> acc and new } },
        ::errorSerializer
      )
  }

  private fun orF(windowsExpressionP: JsonParser<Booleans>): JsonParser<Booleans> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("WindowsExpressionOr"))
      .field("expressions", BasicParsers.listP(windowsExpressionP))
      .map(
          Uncurry.untuple { _, expressions -> expressions.fold(Booleans(false)) { acc, new -> acc or new } },
        ::errorSerializer
      )
  }

  private fun notF(windowsExpressionP: JsonParser<Booleans>): JsonParser<Booleans> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("WindowsExpressionNot"))
      .field("expression", windowsExpressionP)
      .map(
          Uncurry.untuple { _, expression -> !expression },
        ::errorSerializer
      )
  }

  private fun windowsStartsF(windowsExpressionP: JsonParser<Booleans>): JsonParser<Booleans> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("IntervalsExpressionStarts"))
      .field("expression", windowsExpressionP)
      .map(
          Uncurry.untuple { _, expression -> expression.risingEdges() },
        ::errorSerializer
      )
  }

  private fun windowsEndsF(windowsExpressionP: JsonParser<Booleans>): JsonParser<Booleans> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("IntervalsExpressionEnds"))
      .field("expression", windowsExpressionP)
      .map(
          Uncurry.untuple { _, expression -> expression.fallingEdges() },
        ::errorSerializer
      )
  }

  private fun spansStartsF(spansExpressionP: JsonParser<Spans>): JsonParser<Spans> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("IntervalsExpressionStarts"))
      .field("expression", spansExpressionP)
      .map(
          Uncurry.untuple { _, expression -> expression.starts() },
        ::errorSerializer
      )
  }

  private fun spansEndsF(spansExpressionP: JsonParser<Spans>): JsonParser<Spans> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("IntervalsExpressionEnds"))
      .field("expression", spansExpressionP)
      .map(
          Uncurry.untuple { _, expression -> expression.ends() },
        ::errorSerializer
      )
  }

  private fun spansSplitF(intervalExpressionP: JsonParser<Spans>): JsonParser<Spans> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("SpansExpressionSplit"))
      .field("intervals", intervalExpressionP)
      .field("numberOfSubIntervals", BasicParsers.intP)
      .map(
          Uncurry.untuple { _, expr, numberOfSubWindows -> expr.split { numberOfSubWindows } },
        ::errorSerializer
      )
  }

  private fun windowsSplitF(intervalExpressionP: JsonParser<Booleans>): JsonParser<Spans> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("SpansExpressionSplit"))
      .field("intervals", intervalExpressionP)
      .field("numberOfSubIntervals", BasicParsers.intP)
      .map(
          Uncurry.untuple { _, expr, numberOfSubWindows ->
              expr.filter { it.value }.unsafeMap(::Universal, BoundsTransformer.IDENTITY, false) {
                  it.withNewValue(null as Metadata?)
              }
                  .split { numberOfSubWindows }
          },
        ::errorSerializer
      )
  }

  private fun windowsFromSpansF(spansExpressionP: JsonParser<Spans>): JsonParser<Booleans> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("WindowsExpressionFromSpans"))
      .field("spansExpression", spansExpressionP)
      .map(
          Uncurry.untuple { _, spans -> spans.active().assignGaps(false) },
        ::errorSerializer
      )
  }

  private fun forEachActivitySpansF(spansExpressionP: JsonParser<Spans>): JsonParser<Spans> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("ForEachActivitySpans"))
      .field("activityType", BasicParsers.stringP)
      .field("alias", BasicParsers.stringP)
      .field("expression", spansExpressionP)
      .map(
          Uncurry.untuple { _, type, alias, expression ->
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
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("ForEachActivityViolations"))
      .field("activityType", BasicParsers.stringP)
      .field("alias", BasicParsers.stringP)
      .field("expression", violationListExpressionP)
      .map(
          Uncurry.untuple { _, type, alias, expression ->
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
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("ValueAtExpression"))
      .field("profile", profileExpressionP)
      .field("timepoint", spansExpressionP)
      .map(
          Uncurry.untuple { _, profile, timepoint ->
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
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("ProfileChanges"))
      .field("expression", profileExpressionF(spansExpressionP, linearProfileExprP))
      .map(
          Uncurry.untuple { _, expression -> expression.changes() },
        ::errorSerializer
      )
  }

  private fun windowsExpressionF(
    spansP: JsonParser<Spans>,
    linearProfileExprP: JsonParser<Real>
  ): JsonParser<Booleans> {
    return BasicParsers.recursiveP { selfP ->
        BasicParsers.chooseP(
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
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("SpansExpressionFromWindows"))
      .field("windowsExpression", windowsExpressionP)
      .map(
          Uncurry.untuple { _, expression ->
              expression.unsafeMap(::Universal, BoundsTransformer.IDENTITY, false) {
                  it.withNewValue(null)
              }
          },
        ::errorSerializer
      )
  }

  private fun connectTo(spansExpressionP: JsonParser<Spans>): JsonParser<Spans> {
    return BasicParsers.productP
      .field("kind", BasicParsers.literalP("SpansExpressionConnectTo"))
      .field("from", spansExpressionP)
      .field("to", spansExpressionP)
      .map(
          Uncurry.untuple { _, from, to ->
              from.connectTo(to, false)
                  .unsafeMap(::Universal, BoundsTransformer.IDENTITY, false) {
                      Segment(it.interval, it.from?.value)
                  }
          },
        ::errorSerializer
      )
  }

  private val spansIntervalP: JsonParser<Spans> = BasicParsers.productP
    .field("kind", BasicParsers.literalP("SpansExpressionInterval"))
    .field("interval", intervalExpressionP)
    .map(
        Uncurry.untuple { _, interval -> Spans(Segment(interval, null)) },
      ::errorSerializer
    )

  private fun spansExpressionF(windowsP: JsonParser<Booleans>): JsonParser<Spans> {
    return BasicParsers.recursiveP { selfP ->
        BasicParsers.chooseP(
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
      BasicParsers.recursiveP { selfP ->
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
      BasicParsers.recursiveP { selfP ->
          spansExpressionF(windowsExpressionF(selfP, linearProfileExprP))
      }
  private val profileExpressionP: JsonParser<SerialSegmentOps<*, *, *>> = profileExpressionF(
    spansExpressionP, linearProfileExprP
  )
  private val discreteProfileExprP: JsonParser<Constants<SerializedValue>> = discreteProfileExprF(
    profileExpressionP, spansExpressionP
  )

  private val violationsOfP: JsonParser<Violations> = BasicParsers.productP
    .field("kind", BasicParsers.literalP("ViolationsOf"))
    .field("expression", windowsExpressionP)
    .map(
        Uncurry.untuple { _, expression -> expression.violateOn(false) },
      ::errorSerializer
    )

  private val rollingThresholdAlgorithmP: JsonParser<RollingThresholdAlgorithm> = BasicParsers.enumP(
      RollingThresholdAlgorithm::class.java
  ) { obj -> obj.name }

  private val rollingThresholdP: JsonParser<Violations> = BasicParsers.productP
    .field("kind", BasicParsers.literalP("RollingThreshold"))
    .field("spans", spansExpressionP)
    .field("width", durationExprP)
    .field("threshold", durationExprP)
    .field("algorithm", rollingThresholdAlgorithmP)
    .map(
        Uncurry.untuple { _, spans, width, threshold, algorithm -> TODO() },
      ::errorSerializer
    )

  public val constraintP: JsonParser<Violations> =
      BasicParsers.recursiveP { selfP ->
          BasicParsers.chooseP(
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
