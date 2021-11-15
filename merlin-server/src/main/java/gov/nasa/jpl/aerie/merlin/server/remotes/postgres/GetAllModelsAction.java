package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.models.MissionModelJar;
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

  public Map<String, MissionModelJar> get() throws SQLException {
    try (final var results = this.statement.executeQuery()) {
      final var missionModels = new HashMap<String, MissionModelJar>();

      while (results.next()) {
        final var id = Long.toString(results.getLong(1));

        final var missionModel = new MissionModelJar();
        missionModel.mission = results.getString(2);
        missionModel.name = results.getString(3);
        missionModel.version = results.getString(4);
        missionModel.owner = results.getString(5);
        missionModel.path = Path.of(results.getString(6));

        missionModels.put(id, missionModel);
      }

      return missionModels;
    }
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
