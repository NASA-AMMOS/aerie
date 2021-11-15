package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.models.MissionModelJar;
import gov.nasa.jpl.aerie.merlin.server.remotes.MissionModelRepository;
import org.intellij.lang.annotations.Language;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/*package-local*/ final class GetModelAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    select m.mission, m.name, m.version, m.owner, f.path
    from mission_model AS m
    inner join uploaded_file AS f
      on m.jar_id = f.id
    where m.id = ?
    """;

  private final PreparedStatement statement;

  public GetModelAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public MissionModelJar get(final long modelId) throws SQLException, MissionModelRepository.NoSuchMissionModelException {
    this.statement.setLong(1, modelId);

    try (final var results = this.statement.executeQuery()) {
      if (!results.next()) throw new MissionModelRepository.NoSuchMissionModelException();

      final var missionModel = new MissionModelJar();
      missionModel.mission = results.getString(1);
      missionModel.name = results.getString(2);
      missionModel.version = results.getString(3);
      missionModel.owner = results.getString(4);
      missionModel.path = Path.of(results.getString(5));

      return missionModel;
    }
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
