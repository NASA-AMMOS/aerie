package gov.nasa.jpl.aerie.timeline.plan

import gov.nasa.jpl.aerie.merlin.driver.json.SerializedValueJsonParser
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue
import gov.nasa.jpl.aerie.timeline.BaseTimeline
import gov.nasa.jpl.aerie.timeline.Duration
import gov.nasa.jpl.aerie.timeline.Duration.Companion.minus
import gov.nasa.jpl.aerie.timeline.Duration.Companion.plus
import gov.nasa.jpl.aerie.timeline.Interval.Companion.between
import gov.nasa.jpl.aerie.timeline.payloads.activities.Directive
import gov.nasa.jpl.aerie.timeline.collections.Directives
import java.io.StringReader
import java.sql.Connection
import java.time.Instant
import javax.json.Json

/** A connection to Aerie's database for a particular simulation result. */
class AeriePostgresPlan(
    /** A connection to Aerie's database. */
    private val c: Connection,
    override val id: Int
): Plan {

  private val planInfo by lazy {
    val statement = c.prepareStatement("select start_time, duration from plan where id = ?;")
    statement.setInt(1, id)
    intervalStyleStatement(c).execute()
    val response = statement.executeQuery()
    if (!response.next()) throw DatabaseError("Expected exactly one result for query, found none: $statement")
    val result = object {
      val startTime = response.getTimestamp(1).toInstant()
      val duration = Duration.parseISO8601(response.getString(2))
      val id = this@AeriePostgresPlan.id
    }
    if (response.next()) throw DatabaseError("Expected exactly one result for query, found more than one: $statement")
    result
  }

  override fun totalBounds() = between(Duration.ZERO, planInfo.duration)

  override fun toRelative(abs: Instant) = abs - planInfo.startTime
  override fun toAbsolute(rel: Duration) = planInfo.startTime + rel


  /***/ class DatabaseError(message: String): Error(message)

  private fun parseJson(jsonStr: String): SerializedValue = Json.createReader(StringReader(jsonStr)).use { reader ->
      val requestJson = reader.readValue()
      val result = SerializedValueJsonParser.serializedValueP.parse(requestJson)
      return result.getSuccessOrThrow { DatabaseError(it.toString()) }
  }

  private val allDirectivesStatement = c.prepareStatement(
      "select name, start_offset, type, arguments, id from activity_directive where plan_id = ?" +
        " and start_offset > ?::interval and start_offset < ?::interval;"
  )
  private val filteredDirectivesStatement = c.prepareStatement(
      "select name, start_offset, type, arguments, id from activity_directive where plan_id = ?" +
          " and start_offset > ?::interval and start_offset < ?::interval and type = ?;"
  )
  override fun <A: Any> directives(type: String?, deserializer: (SerializedValue) -> A) = BaseTimeline(::Directives) { opts ->
    val statement = if (type == null) allDirectivesStatement else filteredDirectivesStatement
    statement.clearParameters()
    statement.setInt(1, planInfo.id)
    statement.setString(2, opts.bounds.start.toISO8601())
    statement.setString(3, opts.bounds.end.toISO8601())
    if (type != null) statement.setString(4, type)
    intervalStyleStatement(c).execute()
    val response = statement.executeQuery()
    val result = mutableListOf<Directive<A>>()
    while (response.next()) {
      result.add(Directive(
          deserializer(parseJson(response.getString(4))),
          response.getString(1),
          response.getLong(5),
          response.getString(3),
          Duration.parseISO8601(response.getString(2))
      ))
    }
    result
  }.specialize()

  companion object {
  }
}
