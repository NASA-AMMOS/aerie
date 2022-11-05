package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.apache.commons.lang3.tuple.Pair;
import org.intellij.lang.annotations.Language;

import java.sql.SQLException;
import java.util.List;

import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PreparedStatements.setTimestamp;

public final class PostProfileSegmentsAction implements AutoCloseable {
  public static final @Language("SQL") String sql = """
      insert into profile_segment (dataset_id, profile_id, start_offset, dynamics)
      values (?, ?, ?::timestamptz - ?::timestamptz, ?)
    """;
//  private final PreparedStatement statement;
  public PostProfileSegmentsAction() {

  }

//  public PostProfileSegmentsAction(final Connection connection) throws SQLException {
//    this.statement = connection.prepareStatement(sql);
//  }

  public <Dynamics> void apply(
      final ParallelInserter parallelInserter,
      final long datasetId,
      final ProfileRecord profileRecord,
      final List<Pair<Duration, Dynamics>> segments,
      final Timestamp simulationStart,
      final JsonParser<Dynamics> dynamicsP
      ) throws SQLException {

    // Each profile segment's duration part is the duration for which the dynamics hold
    // before the next one begins. Since order in the database is not guaranteed
    // we need to convert to offsets from the simulation start so order can be preserved
    var accumulatedOffset = Duration.ZERO;
    for (final var pair : segments) {
      final var duration = pair.getLeft();
      final var dynamics = pair.getRight();
      final var timestamp = simulationStart.plusMicros(accumulatedOffset.dividedBy(Duration.MICROSECOND));

      parallelInserter.declareInsert(sql, statement -> {
        statement.setLong(1, datasetId);
        statement.setLong(2, profileRecord.id());
        setTimestamp(statement, 3, timestamp);
        setTimestamp(statement, 4, simulationStart);
        statement.setString(5, serializeDynamics(dynamics, dynamicsP));
      });

      accumulatedOffset = Duration.add(accumulatedOffset, duration);
    }

//    final var results = this.statement.executeBatch();
//    for (final var result : results) {
//      if (result == Statement.EXECUTE_FAILED) throw new FailedInsertException("profile_segment");
//    }
  }

  private <Dynamics> String serializeDynamics(final Dynamics dynamics, final JsonParser<Dynamics> dynamicsP) {
    return dynamicsP.unparse(dynamics).toString();
  }

  @Override
  public void close() throws SQLException {
//    this.statement.close();
  }
}
