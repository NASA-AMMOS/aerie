package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;

public final record SimulationStateRecord(String state, String reason) {
  public static SimulationStateRecord fromSimulationState(final ResultsProtocol.State simulationState) {
    if (simulationState instanceof ResultsProtocol.State.Success) {
      return new SimulationStateRecord("success", null);
    } else if (simulationState instanceof ResultsProtocol.State.Failed s) {
      return new SimulationStateRecord("failed", s.reason());
    } else if (simulationState instanceof ResultsProtocol.State.Incomplete) {
      return new SimulationStateRecord("incomplete", null);
    } else {
      throw new Error("Unrecognized simulation state");
    }
  }
}
