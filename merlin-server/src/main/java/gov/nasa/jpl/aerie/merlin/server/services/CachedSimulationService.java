package gov.nasa.jpl.aerie.merlin.server.services;

import java.util.Optional;

import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.remotes.ResultsCellRepository;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresResultsCellRepository;

public record CachedSimulationService (
    SimulationAgent agent,
    ResultsCellRepository store
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

  public String testSegment() {
    if (this.store instanceof PostgresResultsCellRepository s) {
      return s.testSegment();
    }
    return "fail, not PostgresResultsCellRepository";
  }

  @Override
  public Optional<SimulationResults> get(final PlanId planId, final RevisionData revisionData) {
    return this.store.lookup(planId) // Only return results that have already been cached
        .map(ResultsProtocol.ReaderRole::get)
        .map(state -> state instanceof final ResultsProtocol.State.Success s ?
            s.results() :
            null);
  }
}
