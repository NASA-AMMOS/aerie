package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.constraints.model.Violation;

public record ConstraintRunRecord(
  long constraintId,
  Status status,
  Violation violation
) {
  public enum Status {
    PENDING("pending"),
    INCOMPLETE("incomplete"),
    FAILED("failed"),
    SUCCESS("success");

    public final String label;

    Status(final String label) {
      this.label = label;
    }

    public static Status fromString(final String label) throws InvalidRequestStatusException {
      return switch (label) {
        case "pending" -> PENDING;
        case "incomplete" -> INCOMPLETE;
        case "failed" -> FAILED;
        case "success" -> SUCCESS;
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
