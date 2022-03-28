package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.SimulationStateRecord.Status;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

/*package-local*/ final class LookupSimulationDatasetAction implements AutoCloseable {
  private static final @Language("Sql") String sql = """
    with
      revisions as
        ( select
           s.id as sim_id,
           s.revision as sim_revision,
           t.revision as template_revision,
           p.revision as plan_revision,
           m.revision as model_revision
         from simulation as s
         left join simulation_template as t
           on s.simulation_template_id = t.id
         left join plan as p
           on p.id = s.plan_id
         left join mission_model as m
           on m.id = p.model_id)
    select
          d.dataset_id,
          d.status,
          d.reason,
          d.canceled,
          d.offset_from_plan_start
      from simulation_dataset as d
      left join revisions as r
        on d.simulation_id = r.sim_id
      where
        d.simulation_id = ? and
        d.simulation_revision = r.sim_revision and
        d.plan_revision = r.plan_revision and
        d.model_revision = r.model_revision and
        (
          d.simulation_template_revision = r.template_revision or
          (
            d.simulation_template_revision is null and
            r.template_revision is null
          )
        )
    """;

  private final PreparedStatement statement;

  public LookupSimulationDatasetAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public Optional<SimulationDatasetRecord> get(
      final long simulationId,
      final Timestamp planStart
  ) throws SQLException {
    this.statement.setLong(1, simulationId);

    final var results = this.statement.executeQuery();
    if (!results.next()) return Optional.empty();

    final Status status;
    try {
      status = Status.fromString(results.getString(2));
    } catch (final Status.InvalidSimulationStatusException ex) {
      throw new Error("Simulation Dataset initialized with invalid state.");
    }

    final var datasetId = results.getLong(1);
    final var reason = results.getString(3);
    final var canceled = results.getBoolean(4);
    final var offsetFromPlanStart = PostgresParsers.parseOffset(results, 5, planStart);
    final var state = new SimulationStateRecord(
        status,
        reason);

    return Optional.of(
        new SimulationDatasetRecord(
            simulationId,
            datasetId,
            state,
            canceled,
            offsetFromPlanStart));
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
