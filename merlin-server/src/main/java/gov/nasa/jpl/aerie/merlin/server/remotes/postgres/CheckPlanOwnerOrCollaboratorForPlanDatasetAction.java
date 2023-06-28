package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/*package local*/ final class CheckPlanOwnerOrCollaboratorForPlanDatasetAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    select
        ? in (select owner from plan join plan_dataset on plan_dataset.dataset_id=? and plan.id = plan_dataset.plan_id) as is_owner,
        ? in (select collaborator from plan_collaborators join plan_dataset on plan_dataset.dataset_id=? and plan_collaborators.plan_id = plan_dataset.plan_id) as is_collaborator;
    """;

  private final PreparedStatement statement;

  public CheckPlanOwnerOrCollaboratorForPlanDatasetAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public PlanOwnerOrCollaborator get(final long datasetId, final String username)
  throws SQLException {
    this.statement.setString(1, username);
    this.statement.setLong(2, datasetId);
    this.statement.setString(3, username);
    this.statement.setLong(4, datasetId);
    final var results = this.statement.executeQuery();

    if (!results.next()) throw new SQLException("Unexpected empty result");

    final var isOwner = results.getBoolean("is_owner");
    final var isCollaborator = results.getBoolean("is_collaborator");

    if (isOwner && isCollaborator) return PlanOwnerOrCollaborator.OWNER_AND_COLLABORATOR;
    if (isOwner) return PlanOwnerOrCollaborator.ONLY_OWNER;
    if (isCollaborator) return PlanOwnerOrCollaborator.ONLY_COLLABORATOR;
    return PlanOwnerOrCollaborator.NEITHER;
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
