package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;

import java.util.Objects;

public final class UncachedSimulationService implements SimulationService {
  private final SynchronousSimulationAgent action;

  public UncachedSimulationService(final SynchronousSimulationAgent action) {
    this.action = Objects.requireNonNull(action);
  }

  @Override
  public ResultsProtocol.State getSimulationResults(final String planId, final long planRevision) {
    final var cell = new ResultsProtocol.InMemoryCell();
    this.action.simulate(planId, planRevision, cell);
    return cell.get();
  }
}
