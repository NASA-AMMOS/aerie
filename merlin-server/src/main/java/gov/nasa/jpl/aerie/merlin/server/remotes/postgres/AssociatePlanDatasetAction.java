package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/*package-local*/ final class AssociatePlanDatasetAction implements AutoCloseable {
  private final @Language("SQL") String sql = """
      insert into plan_dataset (plan_id, dataset_id)
      values (?, ?)
      returning offset_from_plan_start
      """;

  private final PreparedStatement statement;

  public AssociatePlanDatasetAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public PlanDatasetRecord apply(
      final long planId,
      final long datasetId,
      final Timestamp planStart
  ) throws SQLException {
    this.statement.setLong(1, planId);
    this.statement.setLong(2, datasetId);

    final var results = this.statement.executeQuery();
    if (!results.next()) throw new FailedInsertException("plan_dataset");
    final var offsetFromPlanStart = PostgresParsers.parseOffset(results, 1, planStart);

    return new PlanDatasetRecord(planId, datasetId, offsetFromPlanStart);
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
