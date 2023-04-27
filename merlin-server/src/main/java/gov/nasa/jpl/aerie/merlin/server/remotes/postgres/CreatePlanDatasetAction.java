package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECONDS;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.intellij.lang.annotations.Language;

/*package-local*/ final class CreatePlanDatasetAction implements AutoCloseable {
  private final @Language("SQL") String sql =
      """
      insert into plan_dataset (plan_id, offset_from_plan_start)
      values (?, ?::timestamptz - ?::timestamptz)
      returning dataset_id
      """;

  private final PreparedStatement statement;

  public CreatePlanDatasetAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public PlanDatasetRecord apply(
      final long planId, final Timestamp planStart, final Timestamp datasetStart)
      throws SQLException {
    final var offsetFromPlanStart = Duration.of(planStart.microsUntil(datasetStart), MICROSECONDS);

    this.statement.setLong(1, planId);
    PreparedStatements.setTimestamp(this.statement, 2, datasetStart);
    PreparedStatements.setTimestamp(this.statement, 3, planStart);

    final var results = this.statement.executeQuery();
    if (!results.next()) throw new FailedInsertException("plan_dataset");
    final var datasetId = results.getLong(1);

    return new PlanDatasetRecord(planId, datasetId, offsetFromPlanStart);
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
