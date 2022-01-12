package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityInstance;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

/*package-local*/ final class GetActivitiesAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    with
      json_activity_arguments as
        ( select
            arg.activity_id,
            json_object_agg(arg.name, arg.value) as arguments
          from activity_argument as arg
          group by activity_id ),
      full_activity as
        ( select
            a.plan_id,
            a.id,
            ceil(extract(epoch from a.start_offset) * 1000*1000) as start_offset_in_micros,
            a.type,
            coalesce(arguments.arguments, '[]'::json) as arguments
          from activity as a
          left join json_activity_arguments as arguments
            on a.id = arguments.activity_id ),
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
      to_char(p.start_time, 'YYYY-DDD"T"HH24:MI:SS.FF6') as start_time,
      coalesce(activities.activities, '[]'::json) as activities
    from plan as p
    left join json_activities as activities
      on activities.plan_id = p.id
    where p.id = ?
    """;

  private final PreparedStatement statement;

  public GetActivitiesAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public Map<ActivityInstanceId, ActivityInstance> get(final PlanId planId) throws SQLException, NoSuchPlanException {
    this.statement.setLong(1, planId.id());

    try (final var results = this.statement.executeQuery()) {
      if (!results.next()) throw new NoSuchPlanException(planId);

      final var startTimestamp = Timestamp.fromString(results.getString(1));
      final var activitiesJson = results.getString(2);

      return PostgresPlanRepository.parseActivitiesJson(activitiesJson, startTimestamp);
    }
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
