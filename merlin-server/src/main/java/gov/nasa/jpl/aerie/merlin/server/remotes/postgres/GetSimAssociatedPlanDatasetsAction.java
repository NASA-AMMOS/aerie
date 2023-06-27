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

/*package-local*/ final class GetSimAssociatedPlanDatasetsAction implements AutoCloseable {
  private final @Language("SQL") String sql = """
      select
        p.dataset_id,
        p.offset_from_plan_start
      from plan_dataset as p
      where
        p.plan_id = ? and p.simulation_dataset_id = ?
    """;

  private final PreparedStatement statement;

  public GetSimAssociatedPlanDatasetsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public List<PlanDatasetRecord> get(final PlanId planId, final SimulationDatasetId simulationDatasetId) throws SQLException {
    final var records = new ArrayList<PlanDatasetRecord>();
    this.statement.setLong(1, planId.id());
    this.statement.setLong(2, simulationDatasetId.id());
    final var resultSet = statement.executeQuery();
    while (resultSet.next()) {
      final var datasetId = resultSet.getLong(1);
      final var offsetFromPlanStart = parseOffset(resultSet, 2);
      records.add(new PlanDatasetRecord(planId.id(), datasetId, Optional.of(simulationDatasetId), offsetFromPlanStart));
    }

    return records;
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
