package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;

public final record SimulationStateRecord(Status status, String reason) {
  public static SimulationStateRecord fromSimulationState(final ResultsProtocol.State simulationState) {
    if (simulationState instanceof ResultsProtocol.State.Success) {
      return new SimulationStateRecord(Status.SUCCESS, null);
    } else if (simulationState instanceof ResultsProtocol.State.Failed s) {
      return new SimulationStateRecord(Status.FAILED, s.reason());
    } else if (simulationState instanceof ResultsProtocol.State.Incomplete) {
      return new SimulationStateRecord(Status.INCOMPLETE, null);
    } else {
      throw new Error("Unrecognized simulation state");
    }
  }

  public enum Status {
    INCOMPLETE("incomplete"),
    FAILED("failed"),
    SUCCESS("success");

    public final String label;
    Status(final String label) {
      this.label = label;
    }

    public static Status fromString(final String label) throws InvalidSimulationStatusException {
      return switch(label) {
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
