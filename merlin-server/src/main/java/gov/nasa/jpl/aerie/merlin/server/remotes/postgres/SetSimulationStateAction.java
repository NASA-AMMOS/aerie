package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol.State;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/*package-local*/ final class SetSimulationStateAction implements AutoCloseable {
  private final @Language("SQL") String sql = """
        update dataset
          set
            state = ?,
            reason = ?
          where id = ?
        """;

  private final PreparedStatement statement;

  public SetSimulationStateAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public void apply(final long datasetId, final State simulationState) throws SQLException, NoSuchDatasetException {

    final String state;
    final String reason;
    if (simulationState instanceof State.Success) {
      state = "success";
      reason = null;
    } else if (simulationState instanceof State.Failed s) {
      state = "failed";
      reason = s.reason();
    } else if (simulationState instanceof State.Incomplete) {
      state = "incomplete";
      reason = null;
    } else {
      throw new Error("Unrecognized simulation state");
    }

    this.statement.setString(1, state);
    this.statement.setString(2, reason);
    this.statement.setLong(3, datasetId);

    final var count = this.statement.executeUpdate();
    if (count < 1) throw new NoSuchDatasetException(datasetId);
    if (count > 1) throw new Error("More than one row affected by dataset update by primary key. Is the database corrupted?");
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
