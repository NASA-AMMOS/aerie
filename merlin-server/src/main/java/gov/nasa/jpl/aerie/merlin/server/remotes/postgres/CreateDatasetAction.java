package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECONDS;
import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PreparedStatements.setTimestamp;

/*package-local*/ final class CreateDatasetAction implements AutoCloseable {
  private final @Language("SQL") String sql = """
    insert into dataset
      (
        state,
        canceled,
        plan_id,
        offset_from_plan_start,
        profile_segment_partition_table,
        span_partition_table
      )
    values(?, ?, ?, ?::timestamptz - ?::timestamptz, ?, ?)
    returning id, revision
  """;

  private final PreparedStatement statement;

  public CreateDatasetAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public DatasetRecord apply(
      final long planId,
      final Timestamp planStart,
      final Timestamp simulationStart,
      final String profileSegmentPartitionTable,
      final String spanPartitionTable
  ) throws SQLException {
    final var state = "incomplete";
    final String reason = null;
    final var canceled = false;
    final var offsetFromPlanStart = Duration.of(planStart.microsUntil(simulationStart), MICROSECONDS);

    this.statement.setString(1, state);
    this.statement.setBoolean(2, canceled);
    this.statement.setLong(3, planId);
    setTimestamp(this.statement, 4, simulationStart);
    setTimestamp(this.statement, 5, planStart);
    this.statement.setString(6, profileSegmentPartitionTable);
    this.statement.setString(7, spanPartitionTable);

    final var results = statement.executeQuery();
    if (!results.next()) throw new FailedInsertException("dataset");
    final var datasetId = results.getLong(1);
    final var revision = results.getLong(2);

    return new DatasetRecord(
        datasetId,
        revision,
        state,
        reason,
        canceled,
        planId,
        offsetFromPlanStart,
        profileSegmentPartitionTable,
        spanPartitionTable
    );
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
