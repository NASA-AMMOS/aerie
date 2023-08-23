package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.constraints.model.ConstraintResult;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.SimulationDatasetId;
import gov.nasa.jpl.aerie.merlin.server.remotes.ConstraintRepository;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostgresConstraintRepository implements ConstraintRepository {
  private final DataSource dataSource;

  public PostgresConstraintRepository(final DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public void insertConstraintRuns(
      final Map<Long, Constraint> constraintMap,
      final Map<Long, ConstraintResult> results,
      final Long simulationDatasetId
  ) {
    try (final var connection = this.dataSource.getConnection()) {
      try (final var insertConstraintRunsAction = new InsertConstraintRunsAction(connection)) {
        insertConstraintRunsAction.apply(constraintMap, results, simulationDatasetId);
      }
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to save constraint run", ex);
    }
  }

  @Override
  public Map<Long, ConstraintRunRecord> getValidConstraintRuns(List<Long> constraintIds, SimulationDatasetId simulationDatasetId) {
    try (final var connection = this.dataSource.getConnection()) {
      final var constraintRuns = new GetValidConstraintRunsAction(connection, constraintIds, simulationDatasetId).get();
      final var validConstraintRuns = new HashMap<Long, ConstraintRunRecord>();

      for (final var constraintRun : constraintRuns) {
        validConstraintRuns.put(constraintRun.constraintId(), constraintRun);
      }

      return validConstraintRuns;
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to get constraint runs", ex);
    }
  }
}
