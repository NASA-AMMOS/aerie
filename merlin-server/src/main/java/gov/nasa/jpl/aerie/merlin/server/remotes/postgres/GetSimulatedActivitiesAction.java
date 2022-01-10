package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.apache.commons.lang3.tuple.Pair;
import org.intellij.lang.annotations.Language;

import javax.json.Json;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECONDS;
import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.activityAttributesP;
import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.parseOffset;

/*package-local*/ final class GetSimulatedActivitiesAction implements AutoCloseable {
  private final @Language("SQL") String sql = """
      select
        a.id,
        a.type,
        a.parent_id,
        a.start_offset,
        a.duration,
        a.attributes
      from span as a
      where
        a.dataset_id = ?
    """;

  private final PreparedStatement statement;

  public GetSimulatedActivitiesAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public Map<Long, SimulatedActivityRecord> get(final long datasetId, final Timestamp simulationStart) throws SQLException {
    final var activities = new HashMap<Long, SimulatedActivityRecord>();

    this.statement.setLong(1, datasetId);
    final var resultSet = statement.executeQuery();
    while (resultSet.next()) {
      final var id = resultSet.getLong(1);
      final var type = resultSet.getString(2);
      final var parentId = readOptionalLong(resultSet, 3);
      final var startOffset = parseOffset(resultSet, 4, simulationStart);
      final var start = simulationStart.toInstant().plus(startOffset.in(MICROSECONDS), ChronoUnit.MICROS);
      final var duration = parseOffset(resultSet, 5, start);
      final var attributes = parseActivityAttributes(resultSet.getCharacterStream(6));
      final var directiveId = attributes.getLeft();
      final var arguments = attributes.getRight();
      final var initialChildIds = new ArrayList<Long>();

      activities.put(id, new SimulatedActivityRecord(
          type,
          arguments,
          start,
          duration,
          parentId,
          initialChildIds,
          directiveId));
    }

    // Since child IDs are not stored, we assign them by examining the parent ID of each activity
    activities.forEach(
        (id, activity) -> activity
            .parentId()
            .ifPresent(parentId -> activities.get(parentId).childIds().add(id)));

    return activities;
  }

  private Optional<Long> readOptionalLong(final ResultSet resultSet, final int index) throws SQLException {
    final var value = resultSet.getLong(index);
    if (resultSet.wasNull()) return Optional.empty();
    return Optional.of(value);
  }

  private Pair<Optional<ActivityInstanceId>, Map<String, SerializedValue>> parseActivityAttributes(final Reader jsonStream) {
    final var json = Json.createReader(jsonStream).readValue();
    return activityAttributesP
        .parse(json)
        .getSuccessOrThrow(
            failureReason -> new Error("Corrupt activity arguments cannot be parsed: " + failureReason.reason()));
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
