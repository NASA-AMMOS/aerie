package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.remotes.ConstraintRepository;

import javax.sql.DataSource;
import java.sql.SQLException;
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
      final Map<Long, Violation> violations,
      final Long simulationDatasetId
  ) {
    try (final var connection = this.dataSource.getConnection()) {
      try (final var insertConstraintRunsAction = new InsertConstraintRunsAction(connection)) {
        insertConstraintRunsAction.apply(constraintMap, violations, simulationDatasetId);
      }
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to save constraint run", ex);
    }
  }

  @Override
  public List<ConstraintRunRecord> getSuccessfulConstraintRuns(List<Long> constraintIds) {
    try (final var connection = this.dataSource.getConnection()) {
      try (final var getConstraintRuns = new GetSuccessfulConstraintRunsAction(connection, constraintIds)) {
        return getConstraintRuns.get();
      } catch (ConstraintRunRecord.Status.InvalidRequestStatusException ex) {
        throw new Error("Constraint run had an invalid status", ex);
      }
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to get constraint runs", ex);
    }
  }
}
