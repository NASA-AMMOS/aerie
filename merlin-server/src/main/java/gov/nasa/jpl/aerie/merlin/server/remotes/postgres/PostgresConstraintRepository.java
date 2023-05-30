package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.merlin.server.remotes.ConstraintRepository;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

public class PostgresConstraintRepository implements ConstraintRepository {
  private final DataSource dataSource;

  public PostgresConstraintRepository(final DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public void insertConstraintRuns(final List<Violation> violations) {
  }

  @Override
  public ConstraintRunRecord createConstraintRun(final long constraintId) {
    try (final var connection = this.dataSource.getConnection()) {
      try (final var createConstraintRunAction = new CreateConstraintRunAction(connection)) {
        return createConstraintRunAction.apply(constraintId);
      }
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to save constraint run", ex);
    }
  }

  @Override
  public List<ConstraintRunRecord> getConstraintRuns(List<Long> constraintIds) {
    try (final var connection = this.dataSource.getConnection()) {
      try (final var getConstraintRuns = new GetConstraintRunsAction(connection)) {
        return getConstraintRuns.get();
      } catch (ConstraintRunRecord.Status.InvalidRequestStatusException ex) {
        throw new Error("Constraint run had an invalid status", ex);
      }
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to get constraint runs", ex);
    }
  }
}
