package gov.nasa.jpl.aerie.merlin.server.services.constraints

import gov.nasa.ammos.aerie.procedural.constraints.Violation
import gov.nasa.ammos.aerie.procedural.timeline.Interval
import gov.nasa.ammos.aerie.procedural.timeline.Interval.Companion.between
import gov.nasa.jpl.aerie.json.BasicParsers.*
import gov.nasa.jpl.aerie.json.JsonParser
import gov.nasa.jpl.aerie.json.Uncurry.tuple
import gov.nasa.jpl.aerie.json.Uncurry.untuple
import gov.nasa.jpl.aerie.merlin.server.http.MerlinParsers.durationP
import gov.nasa.jpl.aerie.types.ActivityInstanceId

object ConstraintResultParser {
  @JvmStatic val inclusivityP: JsonParser<Interval.Inclusivity> = enumP(Interval.Inclusivity::class.java) {
      obj -> obj.name
  }

  @JvmStatic val intervalP: JsonParser<Interval> = productP
    .field("start", durationP)
    .field("end", durationP)
    .field("startInclusivity", inclusivityP)
    .field("endInclusivity", inclusivityP)
    .map(
      untuple { start, end, startInclusivity, endInclusivity ->
        between(start, end, startInclusivity, endInclusivity)
      }
    ) {
      tuple(
        it.start, it.end, it.startInclusivity, it.endInclusivity
      )
    }

  private val violationP: JsonParser<Violation> = productP
    .field("windows", listP(intervalP))
    .field("activityInstanceIds", listP(longP))
    .map(
      untuple { windows: List<Interval>, instanceIds: List<Long> ->
        Violation(windows.first(), instanceIds.map(::ActivityInstanceId))
      }
    ) { v -> tuple(
      listOf(v.interval),
      v.ids.map { when (it) {
        is ActivityInstanceId -> it.id()
        else -> throw IllegalStateException("eDSL constraints can't refer to directives")
      } }
    ) }

  @JvmStatic val constraintResultP: JsonParser<ConstraintResult> = productP
    .field("violations", listP(violationP))
    .field("constraintId", longP)
    .field("constraintRevision", longP)
    .field("constraintName", stringP)
    .map(
      untuple { violations, id, revision, name ->
        ConstraintResult(violations, id, revision, name)
      }
    ) {
      tuple(
        it.violations,
        it.constraintId,
        it.constraintRevision,
        it.constraintName
      )
    }
}
