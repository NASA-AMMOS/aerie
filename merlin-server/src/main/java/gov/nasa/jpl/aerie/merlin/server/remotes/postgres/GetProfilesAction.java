package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;
import org.intellij.lang.annotations.Language;

import javax.json.Json;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.profileTypeP;

/*package-local*/ final class GetProfilesAction implements AutoCloseable {
  private final @Language("SQL") String sql = """
      select
        p.id,
        p.name,
        p.type
      from profile as p
      where
        p.dataset_id = ?
    """;

  private final PreparedStatement statement;

  public GetProfilesAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public List<ProfileRecord> get(final long datasetId) throws SQLException {
    final var records = new ArrayList<ProfileRecord>();
    this.statement.setLong(1, datasetId);
    final var resultSet = statement.executeQuery();
    while (resultSet.next()) {
      final var profileId = resultSet.getLong(1);
      final var resourceName = resultSet.getString(2);
      final var type = parseProfileType(resultSet.getCharacterStream(3));

      records.add(new ProfileRecord(profileId, datasetId, resourceName, type));
    }

    return records;
  }

  private Pair<String, ValueSchema> parseProfileType(final Reader jsonStream) {
    try(final var reader = Json.createReader(jsonStream)) {
      final var json = reader.readValue();
      return profileTypeP
          .parse(json)
          .getSuccessOrThrow(
              failureReason -> new Error(
                  "Corrupt profile type: " + failureReason.reason()));
    }
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
