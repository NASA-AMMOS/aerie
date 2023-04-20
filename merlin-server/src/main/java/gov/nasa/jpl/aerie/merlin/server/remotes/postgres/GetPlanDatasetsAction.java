package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.parseOffset;

/*package-local*/ final class GetPlanDatasetsAction implements AutoCloseable {
  private final @Language("SQL") String sql = """
      select
        pdsd.simulation_dataset_id,
        p.dataset_id,
        p.offset_from_plan_start
      from plan_dataset as p
      left join plan_dataset_to_simulation_dataset pdsd
      on p.plan_id = pdsd.plan_id
      and p.dataset_id = pdsd.dataset_id
      where
        p.plan_id = ? and (pdsd.simulation_dataset_id is null or pdsd.simulation_dataset_id = ?)
    """;

  private final PreparedStatement statement;

  public GetPlanDatasetsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public List<PlanDatasetRecord> get(final long planId, final Optional<Long> simulationDatasetId) throws SQLException {
    final var records = new ArrayList<PlanDatasetRecord>();
    this.statement.setLong(1, planId);
    if (simulationDatasetId.isPresent()) {
      this.statement.setLong(2, simulationDatasetId.get());
    } else {
      this.statement.setNull(2, Types.INTEGER);
    }
    final var resultSet = statement.executeQuery();
    while (resultSet.next()) {
      final var associatedSimulationDatasetId = Optional.ofNullable(resultSet.getString(1)).map(Long::valueOf);
      final var datasetId = resultSet.getLong(2);
      final var offsetFromPlanStart = parseOffset(resultSet, 3);
      records.add(new PlanDatasetRecord(planId, associatedSimulationDatasetId, datasetId, offsetFromPlanStart));
    }

    return records;
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
