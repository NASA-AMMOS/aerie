package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresRepository.parseActivitiesJson;

/*package-local*/ final class GetAllPlansAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    with
      full_activity as
        ( select
            a.plan_id,
            a.id,
            ceil(extract(epoch from a.start_offset) * 1000*1000) as start_offset_in_micros,
            a.type,
            a.arguments
          from activity as a ),
      json_activities as
        ( select
            a.plan_id,
            array_to_json(array_agg(json_build_object(
              'id', a.id,
              'start_offset_in_micros', a.start_offset_in_micros,
              'type', a.type,
              'arguments', a.arguments))
            ) as activities
          from full_activity as a
          group by a.plan_id )
    select
      p.id,
      p.name,
      p.model_id,
      to_char(p.start_time, 'YYYY-DDD"T"HH24:MI:SS.FF6') as start_time,
      to_char(p.start_time + p.duration, 'YYYY-DDD"T"HH24:MI:SS.FF6') as end_time,
      coalesce(activities.activities, '[]'::json) as activities
    from plan as p
    left join json_activities as activities
      on activities.plan_id = p.id;
    """;

  private final PreparedStatement statement;

  public GetAllPlansAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public Map<String, Plan> get() throws SQLException {
    try (final var results = this.statement.executeQuery()) {
      final var plans = new HashMap<String, Plan>();

      while (results.next()) {
        final var id = Long.toString(results.getLong(1));

        final var name = results.getString(2);
        final var adaptationId = Long.toString(results.getLong(3));
        final var startTimestamp = Timestamp.fromString(results.getString(4));
        final var endTimestamp = Timestamp.fromString(results.getString(5));
        final var activities = parseActivitiesJson(results.getString(6), startTimestamp);
        // @TODO trace this Map.of() through from db to data models objects to determine what is needed here.
        plans.put(id, new Plan(name, adaptationId, startTimestamp, endTimestamp, activities, Map.of()));
      }

      return plans;
    }
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
