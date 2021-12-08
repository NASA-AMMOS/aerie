package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.remotes.InMemoryResultsCellRepository.InMemoryCell;

public record UncachedSimulationService (SimulationAgent action) implements SimulationService {
  @Override
  public ResultsProtocol.State getSimulationResults(final String planId, final long planRevision) {
    final var cell = new InMemoryCell(planId, planRevision);

    try {
      this.action.simulate(planId, planRevision, cell);
    } catch (final InterruptedException ex) {
      // Do nothing. We'll get the current result (probably an Incomplete)
      // and throw away the cell anyway.

      // TODO: Fail back to the common supervisor of this Service and the Agent.
      //   We don't know if the simulation request was successfully received or not,
      //   so the interaction between the two is now in an undefined state.
    }

    final var result = cell.get();
    cell.cancel();

    return result;
  }
}
