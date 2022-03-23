package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

public record SimulationStateRecord(Status status, String reason) {
  public static SimulationStateRecord pending() {
    return new SimulationStateRecord(Status.PENDING, "");
  }

  public static SimulationStateRecord incomplete() {
    return new SimulationStateRecord(Status.INCOMPLETE, "");
  }

  public static SimulationStateRecord failed(final String reason) {
    return new SimulationStateRecord(Status.FAILED, reason);
  }

  public static SimulationStateRecord success() {
    return new SimulationStateRecord(Status.SUCCESS, "");
  }

  public enum Status {
    PENDING("pending"),
    INCOMPLETE("incomplete"),
    FAILED("failed"),
    SUCCESS("success");

    public final String label;
    Status(final String label) {
      this.label = label;
    }

    public static Status fromString(final String label) throws InvalidSimulationStatusException {
      return switch(label) {
        case "pending" -> PENDING;
        case "incomplete" -> INCOMPLETE;
        case "failed" -> FAILED;
        case "success" -> SUCCESS;
        default -> throw new InvalidSimulationStatusException(label);
      };
    }

    public static final class InvalidSimulationStatusException extends Exception {
      public final String invalidStatus;

      public InvalidSimulationStatusException(final String invalidStatus) {
        super(String.format("Invalid Simulation Status: %s", invalidStatus));
        this.invalidStatus = invalidStatus;
      }
    }
  }
}
