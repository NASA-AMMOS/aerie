package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/*package local*/ final class GetSimulationAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    select
          s.id,
          s.revision
      from simulation as s
      where s.plan_id = ?
    """;

  private final PreparedStatement statement;

  public GetSimulationAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public Optional<SimulationRecord> get(final long planId)
  throws SQLException {
      this.statement.setLong(1, planId);
      final ResultSet results = this.statement.executeQuery();

      if (!results.next()) return Optional.empty();

      final var simulationId = results.getLong(1);
      final var simulationRevision = results.getLong(2);
      return Optional.of(new SimulationRecord(simulationId, simulationRevision, planId));
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
