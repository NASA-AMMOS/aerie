package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.models.SimulationDatasetId;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.parseOffset;

/*package-local*/ final class GetPlanDatasetsAction implements AutoCloseable {
  private final @Language("SQL") String sql = """
      select
        p.dataset_id,
        p.offset_from_plan_start,
        p.simulation_dataset_id
      from plan_dataset as p
      where
        p.plan_id = ?
    """;

  private final PreparedStatement statement;

  public GetPlanDatasetsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public List<PlanDatasetRecord> get(final PlanId planId) throws SQLException {
    final var records = new ArrayList<PlanDatasetRecord>();
    this.statement.setLong(1, planId.id());
    final var resultSet = statement.executeQuery();
    while (resultSet.next()) {
      final var datasetId = resultSet.getLong(1);
      final var offsetFromPlanStart = parseOffset(resultSet, 2);
      final Optional<SimulationDatasetId> simulationDatasetId = resultSet.getObject(3) == null
          ? Optional.empty()
          : Optional.of(new SimulationDatasetId(resultSet.getLong(3)));
      records.add(new PlanDatasetRecord(planId.id(), datasetId, simulationDatasetId, offsetFromPlanStart));
    }

    return records;
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
