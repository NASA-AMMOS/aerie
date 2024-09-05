package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static gov.nasa.jpl.aerie.merlin.driver.json.SerializedValueJsonParser.serializedValueP;

/*package-local*/ final class InsertFinconsAction implements AutoCloseable {
  @Language("SQL") private static final String sql = """
      insert into merlin.simulation_fincons (dataset_id, fincons)
      values (?, ?::jsonb)
    """;

  private final PreparedStatement statement;

  public InsertFinconsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public void apply(
      final long datasetId,
      final SerializedValue fincons
      ) throws SQLException {
    statement.setLong(1, datasetId);
    statement.setString(2, serializedValueP.unparse(fincons).toString());
    statement.executeUpdate();
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
