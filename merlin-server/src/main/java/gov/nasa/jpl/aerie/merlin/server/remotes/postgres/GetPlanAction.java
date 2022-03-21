package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/*package-local*/ final class GetPlanAction implements AutoCloseable {
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
      p.name,
      p.revision,
      p.model_id,
      to_char(p.start_time, 'YYYY-DDD"T"HH24:MI:SS.FF6') as start_time,
      to_char(p.start_time + p.duration, 'YYYY-DDD"T"HH24:MI:SS.FF6') as end_time,
      coalesce(activities.activities, '[]'::json) as activities
    from plan as p
    left join json_activities as activities
      on activities.plan_id = p.id
    where p.id = ?
    """;

  private final PreparedStatement statement;

  public GetPlanAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public PlanRecord get(final PlanId planId) throws SQLException, NoSuchPlanException {
    this.statement.setLong(1, planId.id());

    try (final var results = this.statement.executeQuery()) {
      if (!results.next()) throw new NoSuchPlanException(planId);

      final var name = results.getString(1);
      final var revision = results.getLong(2);
      final var missionModelId = results.getLong(3);
      final var startTimestamp = Timestamp.fromString(results.getString(4));
      final var endTimestamp = Timestamp.fromString(results.getString(5));
      final var activities = PostgresPlanRepository.parseActivitiesJson(results.getString(6), startTimestamp);

      return new PlanRecord(planId, revision, name, missionModelId, startTimestamp, endTimestamp, activities);
    }
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
