package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivity;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.apache.commons.lang3.tuple.Pair;
import org.intellij.lang.annotations.Language;

import javax.json.Json;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
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

  public Map<String, SimulatedActivity> get(final long datasetId, final Timestamp simulationStart) throws SQLException {
    final var activities = new HashMap<String, SimulatedActivity>();

    this.statement.setLong(1, datasetId);
    final var resultSet = statement.executeQuery();
    while (resultSet.next()) {
      final var id = resultSet.getString(1);
      final var type = resultSet.getString(2);
      final var parentId = resultSet.getString(3);
      final var startOffset = parseOffset(resultSet, 4, simulationStart);
      final var start = simulationStart.toInstant().plus(startOffset.in(MICROSECONDS), ChronoUnit.MICROS);
      final var duration = parseOffset(resultSet, 5, start);
      final var attributes = parseActivityAttributes(resultSet.getCharacterStream(6));
      final var directiveId = attributes.getLeft();
      final var arguments = attributes.getRight();
      final var initialChildIds = new ArrayList<String>();

      activities.put(id, new SimulatedActivity(
          type,
          arguments,
          start,
          duration,
          parentId,
          initialChildIds,
          directiveId));
    }

    // Since child IDs are not stored, we assign them by examining the parent ID of each activity
    activities.forEach((id, activity) -> {
      if (activity.parentId == null) return;
      activities.get(activity.parentId).childIds.add(id);
    });

    return activities;
  }

  private Pair<Optional<String>, Map<String, SerializedValue>> parseActivityAttributes(final Reader jsonStream) {
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
