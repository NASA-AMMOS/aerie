package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.remotes.AdaptationRepository;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/*package-local*/ final class DeleteModelAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    delete from mission_model
    where id = ?
    """;

  private final PreparedStatement statement;

  public DeleteModelAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public void apply(final long modelId) throws SQLException, AdaptationRepository.NoSuchAdaptationException {
    this.statement.setLong(1, modelId);

    final var count = this.statement.executeUpdate();

    if (count == 0) throw new AdaptationRepository.NoSuchAdaptationException();
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
