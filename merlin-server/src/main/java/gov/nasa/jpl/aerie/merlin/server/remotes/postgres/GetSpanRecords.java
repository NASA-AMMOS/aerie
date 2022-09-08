package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
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
import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.getJsonColumn;
import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.parseOffset;

/*package-local*/ final class GetSpanRecords implements AutoCloseable {
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

  public GetSpanRecords(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public Map<Long, SpanRecord> get(final long datasetId, final Timestamp simulationStart) throws SQLException {
    final var spans = new HashMap<Long, SpanRecord>();

    this.statement.setLong(1, datasetId);
    try (final var resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        final var id = resultSet.getLong(1);
        final var type = resultSet.getString(2);
        final var parentId = readOptionalLong(resultSet, 3);
        final var startOffset = parseOffset(resultSet, 4, simulationStart);
        final var start = simulationStart.toInstant().plus(startOffset.in(MICROSECONDS), ChronoUnit.MICROS);
        final var duration = isNull(resultSet, 5) ? Optional.<Duration>empty() : Optional.of(parseOffset(
            resultSet,
            5,
            start));
        final var attributes = getJsonColumn(resultSet, "attributes", activityAttributesP)
            .getSuccessOrThrow(
              failureReason -> new Error("Corrupt activity arguments cannot be parsed: " + failureReason.reason()));
        final var initialChildIds = new ArrayList<Long>();

        spans.put(id, new SpanRecord(
            type,
            start,
            duration,
            parentId,
            initialChildIds,
            attributes
        ));
      }
    }

    // Since child IDs are not stored, we assign them by examining the parent ID of each activity
    spans.forEach(
        (id, activity) -> activity
            .parentId()
            .ifPresent(parentId -> spans.get(parentId).childIds().add(id)));

    return spans;
  }

  private static boolean isNull(final ResultSet resultSet, final int index) throws SQLException {
    resultSet.getObject(index);
    return resultSet.wasNull();
  }

  private static Optional<Long> readOptionalLong(final ResultSet resultSet, final int index) throws SQLException {
    final var value = resultSet.getLong(index);
    return resultSet.wasNull() ? Optional.empty() : Optional.of(value);
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
