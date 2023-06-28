package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/*package local*/ final class CheckPlanOwnerOrCollaboratorAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    select
        ? in (select owner from plan where id=?) as is_owner,
        ? in (select collaborator from plan_collaborators where plan_id=?) as is_collaborator;
    """;

  private final PreparedStatement statement;

  public CheckPlanOwnerOrCollaboratorAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public PlanOwnerOrCollaborator get(final long planId, final String username)
  throws SQLException {
    this.statement.setString(1, username);
    this.statement.setLong(2, planId);
    this.statement.setString(3, username);
    this.statement.setLong(4, planId);
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
