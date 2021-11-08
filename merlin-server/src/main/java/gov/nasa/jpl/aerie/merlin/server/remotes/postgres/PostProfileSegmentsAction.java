package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.apache.commons.lang3.tuple.Pair;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PreparedStatements.setTimestamp;

public final class PostProfileSegmentsAction implements AutoCloseable {
  private final @Language("SQL") String sql = """
      insert into profile_segment (dataset_id, profile_id, start_offset, dynamics)
      values (?, ?, ?::timestamptz - ?::timestamptz, ?)
    """;
  private final PreparedStatement statement;

  public PostProfileSegmentsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public <Dynamics> void apply(
      final long datasetId,
      final ProfileRecord profileRecord,
      final List<Pair<Duration, Dynamics>> segments,
      final Timestamp simulationStart,
      final JsonParser<Dynamics> dynamicsP
      ) throws SQLException {

    // Each profile segment's duration part is the offset from the previous segment's end
    // To preserve ordering in the database, we convert to absolute offset from simulation start
    // Then when pulling from the database, order by the start_offset column
    var accumulatedOffset = Duration.ZERO;
    for (final var pair : segments) {
      final var offset = pair.getLeft();
      accumulatedOffset = Duration.add(accumulatedOffset, offset);
      final var timestamp = simulationStart.plusMicros(accumulatedOffset.dividedBy(Duration.MICROSECOND));
      final var dynamics = pair.getRight();

      this.statement.setLong(1, datasetId);
      this.statement.setLong(2, profileRecord.id());
      setTimestamp(this.statement, 3, timestamp);
      setTimestamp(this.statement, 4, simulationStart);
      this.statement.setString(5, serializeDynamics(dynamics, dynamicsP));

      this.statement.addBatch();
    }

    final var results = this.statement.executeBatch();
    for (final var result : results) {
      if (result == Statement.EXECUTE_FAILED) throw new FailedInsertException("profile_segment");
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
