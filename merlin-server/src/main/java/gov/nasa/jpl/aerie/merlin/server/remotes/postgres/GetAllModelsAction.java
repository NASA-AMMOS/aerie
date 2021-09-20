package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.models.AdaptationJar;
import org.intellij.lang.annotations.Language;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/*package-local*/ final class GetAllModelsAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    select m.id, m.mission, m.name, m.version, m.owner, f.path
    from mission_model as m
    inner join uploaded_file as f on m.jar_id = f.id
    """;

  private final PreparedStatement statement;

  public GetAllModelsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public Map<String, AdaptationJar> get() throws SQLException {
    try (final var results = this.statement.executeQuery()) {
      final var adaptations = new HashMap<String, AdaptationJar>();

      while (results.next()) {
        final var id = Long.toString(results.getLong(1));

        final var adaptation = new AdaptationJar();
        adaptation.mission = results.getString(2);
        adaptation.name = results.getString(3);
        adaptation.version = results.getString(4);
        adaptation.owner = results.getString(5);
        adaptation.path = Path.of(results.getString(6));

        adaptations.put(id, adaptation);
      }

      return adaptations;
    }
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
