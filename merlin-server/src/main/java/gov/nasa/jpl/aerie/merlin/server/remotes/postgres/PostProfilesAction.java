package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.discreteProfileTypeP;
import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.realProfileTypeP;
/*package-local*/ final class PostProfilesAction implements AutoCloseable {
  private final @Language("SQL") String sql = """
      insert into profile (dataset_id, name, type)
      values (?, ?, ?)
    """;
  private final PreparedStatement statement;

  public PostProfilesAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
  }

  public Map<String, ProfileRecord> apply(
      final long datasetId,
      final Map<String, Pair<ValueSchema, List<Pair<Duration, Optional<RealDynamics>>>>> realProfiles,
      final Map<String, Pair<ValueSchema, List<Pair<Duration, Optional<SerializedValue>>>>> discreteProfiles
  ) throws SQLException {
    final var resourceNames = new ArrayList<String>();
    final var resourceTypes = new ArrayList<Pair<String, ValueSchema>>();
    for (final var entry : realProfiles.entrySet()) {
      final var resource = entry.getKey();
      final var schema = entry.getValue().getLeft();
      final var realResourceType = Pair.of("real", schema);
      resourceNames.add(resource);
      resourceTypes.add(realResourceType);
      this.statement.setLong(1, datasetId);
      this.statement.setString(2, resource);
      this.statement.setString(3, realProfileTypeP.unparse(realResourceType).toString());
      this.statement.addBatch();
    }

    for (final var entry : discreteProfiles.entrySet()) {
      final var resource = entry.getKey();
      final var schema = entry.getValue().getLeft();
      final var resourceType = Pair.of("discrete", schema);
      resourceNames.add(resource);
      resourceTypes.add(resourceType);
      this.statement.setLong(1, datasetId);
      this.statement.setString(2, resource);
      this.statement.setString(3, discreteProfileTypeP.unparse(resourceType).toString());
      this.statement.addBatch();
    }

    statement.executeBatch();
    final var resultSet = statement.getGeneratedKeys();

    final var profileRecords = new HashMap<String, ProfileRecord>(resourceNames.size());
    for (int i = 0; i < resourceNames.size(); i++) {
      final var resource = resourceNames.get(i);
      final var type = resourceTypes.get(i);
      if (!resultSet.next()) throw new Error("Not enough generated IDs returned from batch insertion.");
      final long id = resultSet.getLong(1);
      profileRecords.put(resource, new ProfileRecord(
          id,
          datasetId,
          resource,
          type
      ));
    }

    return profileRecords;
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
