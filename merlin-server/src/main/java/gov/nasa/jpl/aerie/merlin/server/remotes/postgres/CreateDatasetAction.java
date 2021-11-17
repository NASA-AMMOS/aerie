package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECONDS;
import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PreparedStatements.setTimestamp;

/*package-local*/ final class CreateDatasetAction implements AutoCloseable {
  private final @Language("SQL") String sql = """
    insert into dataset (plan_id, offset_from_plan_start)
    values(?, ?::timestamptz - ?::timestamptz)
    returning id, revision
  """;

  private final PreparedStatement statement;

  public CreateDatasetAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public DatasetRecord apply(
      final long planId,
      final Timestamp planStart,
      final Timestamp simulationStart
  ) throws SQLException {
    final var offsetFromPlanStart = Duration.of(planStart.microsUntil(simulationStart), MICROSECONDS);

    this.statement.setLong(1, planId);
    setTimestamp(this.statement, 2, simulationStart);
    setTimestamp(this.statement, 3, planStart);

    final var results = statement.executeQuery();
    if (!results.next()) throw new FailedInsertException("dataset");
    final var datasetId = results.getLong(1);
    final var revision = results.getLong(2);

    return new DatasetRecord(
        datasetId,
        revision,
        planId,
        offsetFromPlanStart
    );
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
