package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.parseOffset;

/*package-local*/ final class GetDatasetAction implements AutoCloseable {
  private final @Language("SQL") String sql = """
  select
      d.revision,
      d.plan_id,
      to_char(p.start_time, 'YYYY-DDD"T"HH24:MI:SS.FF6') as start_time,
      d.offset_from_plan_start,
      d.profile_segment_partition_table,
      d.span_partition_table
  from dataset as d
  left join plan as p
    on p.id = d.plan_id
  where d.id = ?
  """;

  private final PreparedStatement statement;

  public GetDatasetAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public Optional<DatasetRecord> get(final long datasetId) throws SQLException {
    this.statement.setLong(1, datasetId);

    final var results = this.statement.executeQuery();
    if (!results.next()) return Optional.empty();

    final var revision = results.getLong(1);
    final var planId = results.getLong(2);
    final var planStart = Timestamp.fromString(results.getString(3));
    final var offsetFromPlanStart = parseOffset(results, 4, planStart);
    final var profileSegmentPartitionTable = results.getString(5);
    final var spanPartitionTable = results.getString(6);

    return Optional.of(new DatasetRecord(
        datasetId,
        revision,
        planId,
        offsetFromPlanStart,
        profileSegmentPartitionTable,
        spanPartitionTable));
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
