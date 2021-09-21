package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.remotes.AdaptationRepository;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/*package-local*/ final class DeleteModelConstraintAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    delete from mission_model_condition
    where model_id = ?
      and name = ?
    """;

  private final PreparedStatement statement;

  public DeleteModelConstraintAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  /** Returns true if the constraint was successfully deleted. */
  public boolean apply(final long modelId, final String name)
  throws SQLException, AdaptationRepository.NoSuchAdaptationException
  {
    this.statement.setLong(1, modelId);
    this.statement.setString(2, name);

    final var count = this.statement.executeUpdate();

    return (count != 0);
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
