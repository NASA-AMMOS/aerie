package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.models.DatasetId;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.remotes.PermissionType;

import javax.sql.DataSource;
import java.sql.SQLException;

public final class PermissionsRepository {
  private final DataSource dataSource;

  public PermissionsRepository(final DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public PermissionType lookupPermissionType(final String action, final String role) {
    try (
        final var connection = dataSource.getConnection();
        final var lookupPermissionTypeAction = new LookupPermissionTypeAction(connection)
    ) {
      if (role.equals("admin")) {
        return PermissionType.NO_CHECK;
      }
      return lookupPermissionTypeAction.get(role, action);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean canPerformAction(
      final PermissionType permissionType,
      final String username,
      final String action,
      final PlanId planId) {
    return switch (permissionType) {
      case NO_CHECK -> true;
      case OWNER -> throw new RuntimeException("OWNER permission cannot be applied to action \"" + action + "\"");
      case MISSION_MODEL_OWNER -> throw new RuntimeException("MISSION_MODEL_OWNER permission cannot be applied to action \"" + action + "\"");
      case PLAN_OWNER -> getPlanPermissions(username, planId).isPlanOwner();
      case PLAN_COLLABORATOR -> getPlanPermissions(username, planId).isPlanCollaborator();
      case PLAN_OWNER_COLLABORATOR -> getPlanPermissions(username, planId).isPlanOwnerOrCollaborator();
      case ALWAYS_UNAUTHORIZED -> false;
    };
  }

  public boolean canPerformAction(
      final PermissionType permissionType,
      final String username,
      final String action,
      final DatasetId datasetId) {
    return switch (permissionType) {
      case NO_CHECK -> true;
      case OWNER -> throw new RuntimeException("OWNER permission cannot be applied to action \"" + action + "\"");
      case MISSION_MODEL_OWNER -> throw new RuntimeException("MISSION_MODEL_OWNER permission cannot be applied to action \"" + action + "\"");
      case PLAN_OWNER -> getPlanDatasetPermissions(username, datasetId).isPlanOwner();
      case PLAN_COLLABORATOR -> getPlanDatasetPermissions(username, datasetId).isPlanCollaborator();
      case PLAN_OWNER_COLLABORATOR -> getPlanDatasetPermissions(username, datasetId).isPlanOwnerOrCollaborator();
      case ALWAYS_UNAUTHORIZED -> false;
    };
  }

  private PlanOwnerOrCollaborator getPlanPermissions(final String username, final PlanId planId) {
    try (
        final var connection = dataSource.getConnection();
        final var action = new CheckPlanOwnerOrCollaboratorAction(connection)
    ) {
      return action.get(planId.id(), username);
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }

  private PlanOwnerOrCollaborator getPlanDatasetPermissions(final String username, final DatasetId datasetId) {
    try (
        final var connection = dataSource.getConnection();
        final var action = new CheckPlanOwnerOrCollaboratorForPlanDatasetAction(connection)
    ) {
      return action.get(datasetId.id(), username);
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }
}
