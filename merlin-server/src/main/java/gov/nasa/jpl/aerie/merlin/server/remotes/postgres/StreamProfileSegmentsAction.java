package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

final class StreamProfileSegmentsAction implements AutoCloseable {
  private final @Language("SQL") String sql = """
      insert into profile_segment_stream (dataset_id, profile_id, timeline_segment_id, dynamics)
      values (?, ?, ?, ?)
    """;
  private final PreparedStatement statement;

  public StreamProfileSegmentsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public <Dynamics> void apply(
      final long datasetId,
      final ProfileRecord profileRecord,
      final List<Pair<Duration, Dynamics>> segments,
      final JsonParser<Dynamics> dynamicsP,
      final long timelineSegmentId
      ) throws SQLException {

    for (final var pair : segments) {
      final var dynamics = pair.getRight();

      this.statement.setLong(1, datasetId);
      this.statement.setLong(2, profileRecord.id());
      this.statement.setLong(3, timelineSegmentId);
      this.statement.setString(4, serializeDynamics(dynamics, dynamicsP));

      this.statement.addBatch();
    }

    final var results = this.statement.executeBatch();
    for (final var result : results) {
      if (result == Statement.EXECUTE_FAILED) throw new FailedInsertException("profile_segment_stream");
    }
  }

  private <Dynamics> String serializeDynamics(final Dynamics dynamics, final JsonParser<Dynamics> dynamicsP) {
    return dynamicsP.unparse(dynamics).toString();
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
