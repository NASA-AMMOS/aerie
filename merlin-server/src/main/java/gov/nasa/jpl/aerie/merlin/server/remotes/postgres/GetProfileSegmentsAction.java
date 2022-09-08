package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;
import org.intellij.lang.annotations.Language;

import javax.json.Json;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.getJsonColumn;
import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.parseOffset;

/*package-local*/ final class GetProfileSegmentsAction implements AutoCloseable {
  private final @Language("SQL") String sql = """
      select
        seg.start_offset,
        seg.dynamics
      from profile_segment as seg
      where
        seg.dataset_id = ? and
        seg.profile_id = ?
      order by seg.start_offset asc
    """;
  private final PreparedStatement statement;

  public GetProfileSegmentsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public <Dynamics> List<Pair<Duration, Dynamics>> get(
      final long datasetId,
      final long profileId,
      final Window simulationWindow,
      final JsonParser<Dynamics> dynamicsP
  ) throws SQLException {
    final var segments = new ArrayList<Pair<Duration, Dynamics>>();
    this.statement.setLong(1, datasetId);
    this.statement.setLong(2, profileId);
    final var resultSet = statement.executeQuery();

    // Profile segments are stored with their start offset relative to simulation start
    // We must convert these to durations describing how long each segment lasts
    final var simulationStart = simulationWindow.start();
    final var simulationDuration = simulationWindow.duration();
    if (resultSet.next()) {
      var offset = parseOffset(resultSet, 1, simulationStart);
      var dynamics = getJsonColumn(resultSet, "dynamics", dynamicsP)
          .getSuccessOrThrow(failureReason -> new Error("Corrupt profile dynamics: " + failureReason.reason()));

      while (resultSet.next()) {
        final var nextOffset = parseOffset(resultSet, 1, simulationStart);
        final var duration = nextOffset.minus(offset);
        segments.add(Pair.of(duration, dynamics));
        offset = nextOffset;
        dynamics = getJsonColumn(resultSet, "dynamics", dynamicsP)
            .getSuccessOrThrow(
                failureReason -> new Error("Corrupt profile dynamics: " + failureReason.reason()));
      }

      final var duration = simulationDuration.minus(offset);
      segments.add(Pair.of(duration, dynamics));
    }

    return segments;
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
