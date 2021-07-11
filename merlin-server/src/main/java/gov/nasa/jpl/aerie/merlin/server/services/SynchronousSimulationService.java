package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;

import java.util.Objects;

public final class SynchronousSimulationService implements SimulationService {
  private final RunSimulationAction action;

  public SynchronousSimulationService(final RunSimulationAction action) {
    this.action = Objects.requireNonNull(action);
  }

  @Override
  public ResultsProtocol.State getSimulationResults(final String planId, final long planRevision) {
    final var cell = new ResultsProtocol.InMemoryCell();
    this.action.simulate(planId, planRevision, cell);
    return cell.get();
  }
}
