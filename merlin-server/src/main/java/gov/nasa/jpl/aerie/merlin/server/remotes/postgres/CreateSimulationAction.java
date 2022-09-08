package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.simulationArgumentsP;

/*package-local*/ final class CreateSimulationAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    insert into simulation (plan_id, arguments)
    values (?, ?)
    returning id, revision
    """;

  private final PreparedStatement statement;

  public CreateSimulationAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public SimulationRecord apply(
      final long planId,
      final Map<String, SerializedValue> arguments
  ) throws SQLException, FailedInsertException {
    this.statement.setLong(1, planId);
    this.statement.setString(2, simulationArgumentsP.unparse(arguments).toString());

    final var results = statement.executeQuery();
    if (!results.next()) throw new FailedInsertException("simulation");

    final var simulationId = results.getLong(1);
    final var simulationRevision = results.getLong(2);
    return new SimulationRecord(simulationId, simulationRevision, planId, Optional.empty(), arguments);
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
