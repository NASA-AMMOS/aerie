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
    final var response = this.action.run(planId, planRevision);

    if (response instanceof RunSimulationAction.Response.Failed res) {
      return new ResultsProtocol.State.Failed(res.reason());
    } else if (response instanceof RunSimulationAction.Response.Success res) {
      return new ResultsProtocol.State.Success(res.results());
    } else {
      throw new UnexpectedSubtypeError(RunSimulationAction.Response.class, response);
    }
  }
}
