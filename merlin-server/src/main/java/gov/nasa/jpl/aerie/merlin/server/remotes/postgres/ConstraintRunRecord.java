package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.constraints.model.Violation;

public record ConstraintRunRecord(
  long constraintId,
  Status status,
  Violation violation
) {
  public enum Status {
    CONSTRAINT_OUTDATED("constraint-outdated"),
    RESOLVED("resolved");

    public final String label;

    Status(final String label) {
      this.label = label;
    }

    public static Status fromString(final String label) throws InvalidRequestStatusException {
      return switch (label) {
        case "constraint-outdated" -> CONSTRAINT_OUTDATED;
        case "resolved" -> RESOLVED;
        default -> throw new InvalidRequestStatusException(label);
      };
    }

    public static final class InvalidRequestStatusException extends Exception {
      public final String invalidStatus;

      public InvalidRequestStatusException(final String invalidStatus) {
        super(String.format("Invalid status: %s", invalidStatus));
        this.invalidStatus = invalidStatus;
      }
    }
  }
}
