package gov.nasa.jpl.aerie.merlin.server.http;

import gov.nasa.jpl.aerie.merlin.server.models.DatasetId;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.remotes.PermissionType;

public class Unauthorized extends Exception {
  public Unauthorized(final String role, final String action) {
    super("Role \"" + role + "\" is not allowed to perform action \"" + action + "\"");
  }

  public Unauthorized(
      final String action,
      final String role,
      final String username,
      final PermissionType permissionType,
      final PlanId planId) {
    super("User \"%s\" with role \"%s\" cannot perform \"%s\" because they are not a \"%s\" for plan with id \"%d\"".formatted(
        username,
        role,
        action,
        permissionType,
        planId.id()));
  }

  public Unauthorized(
      final String action,
      final String role,
      final String username,
      final PermissionType permissionType,
      final DatasetId datasetId) {
    super("User \"%s\" with role \"%s\" cannot perform \"%s\" because they are not a \"%s\" for dataset with id \"%d\"".formatted(
        username,
        role,
        action,
        permissionType,
        datasetId.id()));
  }
}
