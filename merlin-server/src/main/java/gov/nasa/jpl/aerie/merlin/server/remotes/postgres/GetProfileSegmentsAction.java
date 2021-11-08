package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.apache.commons.lang3.tuple.Pair;
import org.intellij.lang.annotations.Language;

import javax.json.Json;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
      final Timestamp simulationStart,
      final JsonParser<Dynamics> dynamicsP
      ) throws SQLException {
    final var segments = new ArrayList<Pair<Duration, Dynamics>>();
    this.statement.setLong(1, datasetId);
    this.statement.setLong(2, profileId);
    final var resultSet = statement.executeQuery();

    // Profile segments are stored with their start offset relative to simulation start
    // We must convert these offsets to be relative to their previous segment's start
    var previousOffset = Duration.ZERO;
    while (resultSet.next()) {
      final var absoluteOffset = parseOffset(resultSet, 1, simulationStart);
      final var offset = absoluteOffset.minus(previousOffset);
      final var dynamics = parseDynamics(resultSet.getCharacterStream(2), dynamicsP);
      segments.add(Pair.of(offset, dynamics));
      previousOffset = absoluteOffset;
    }

    return segments;
  }

  private <Dynamics> Dynamics parseDynamics(final Reader jsonStream, final JsonParser<Dynamics> dynamicsP) {
    final var json = Json.createReader(jsonStream).readValue();
    return dynamicsP
        .parse(json)
        .getSuccessOrThrow(
            failureReason -> new Error(
                "Corrupt profile dynamics: " + failureReason.reason()));
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
