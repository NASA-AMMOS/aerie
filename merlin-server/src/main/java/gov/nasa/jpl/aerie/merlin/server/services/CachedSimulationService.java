package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.remotes.ResultsCellRepository;

public record CachedSimulationService (
    ResultsCellRepository store,
    SimulationAgent agent
) implements SimulationService {
  @Override
  public ResultsProtocol.State getSimulationResults(final PlanId planId, final RevisionData revisionData) {
    final var cell$ = this.store.lookup(planId);
    if (cell$.isPresent()) {
      return cell$.get().get();
    } else {
      // Allocate a fresh cell.
      final var cell = this.store.allocate(planId);

      // Return the current value of the reader; if it's incomplete, the caller can check it again later.
      return cell.get();
    }
  }
}
