package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.models.ActivityDirectiveForValidation;
import org.apache.commons.lang3.tuple.Pair;
import org.intellij.lang.annotations.Language;
import gov.nasa.jpl.aerie.merlin.server.services.MissionModelService.BulkArgumentValidationResponse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/*package-local*/ final class UpdateActivityDirectiveValidationsAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    update activity_directive_validations
    set validations = ?::jsonb
    where (directive_id, plan_id) = (?, ?)
  """;

  private final PreparedStatement statement;

  public UpdateActivityDirectiveValidationsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public void apply(List<Pair<ActivityDirectiveForValidation, BulkArgumentValidationResponse>> updates)
  throws SQLException, FailedUpdateException {
    for (final var update : updates) {
      final var directive = update.getLeft();
      final var validation = update.getRight();

      PreparedStatements.setValidationResponse(this.statement, 1, validation);
      this.statement.setLong(2, directive.id().id());
      this.statement.setLong(3, directive.planId().id());
      statement.addBatch();
    }
    statement.executeBatch();
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
