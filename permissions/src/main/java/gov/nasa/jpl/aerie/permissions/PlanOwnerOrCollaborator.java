package gov.nasa.jpl.aerie.permissions;

public enum PlanOwnerOrCollaborator {
  NEITHER,
  ONLY_OWNER,
  ONLY_COLLABORATOR,
  OWNER_AND_COLLABORATOR;

  boolean isPlanOwner() {
    return switch(this) {
      case NEITHER, ONLY_COLLABORATOR -> false;
      case ONLY_OWNER, OWNER_AND_COLLABORATOR -> true;
    };
  }

  boolean isPlanCollaborator() {
    return switch(this) {
      case NEITHER, ONLY_OWNER -> false;
      case ONLY_COLLABORATOR, OWNER_AND_COLLABORATOR -> true;
    };
  }

  boolean isPlanOwnerOrCollaborator() {
    return switch(this) {
      case NEITHER -> false;
      case ONLY_OWNER, ONLY_COLLABORATOR, OWNER_AND_COLLABORATOR -> true;
    };
  }
}
