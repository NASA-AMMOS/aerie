package gov.nasa.jpl.aerie.merlin.server.http;

import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.DatasetId;
import gov.nasa.jpl.aerie.merlin.server.models.HasuraAction;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.remotes.PermissionType;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PermissionsRepository;

public class PermissionsService {

  private final PermissionsRepository permissionsRepository;

  public PermissionsService(final PermissionsRepository permissionsRepository) {
    this.permissionsRepository = permissionsRepository;
  }

  public void check(final String action, final HasuraAction.Session session, final PlanId planId) throws Unauthorized, NoSuchPlanException {
    final var role = session.hasuraRole();
    final var username = session.hasuraUserId();

    final var permissionType = permissionsRepository.lookupPermissionType(action, role);
    if (permissionType == PermissionType.ALWAYS_UNAUTHORIZED) throw new Unauthorized(role, action);

    final var authorized = permissionsRepository.canPerformAction(permissionType, username, action, planId);
    if (!authorized) throw new Unauthorized(action, role, username, permissionType, planId);
  }

  public void check(final String action, final HasuraAction.Session session, final DatasetId datasetId) throws Unauthorized {
    final var role = session.hasuraRole();
    final var username = session.hasuraUserId();

    final var permissionType = permissionsRepository.lookupPermissionType(action, role);
    if (permissionType == PermissionType.ALWAYS_UNAUTHORIZED) throw new Unauthorized(role, action);

    final var authorized = permissionsRepository.canPerformAction(permissionType, username, action, datasetId);
    if (!authorized) throw new Unauthorized(action, role, username, permissionType, datasetId);
  }
}
