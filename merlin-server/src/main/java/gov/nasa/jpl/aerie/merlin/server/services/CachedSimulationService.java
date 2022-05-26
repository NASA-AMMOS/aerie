package gov.nasa.jpl.aerie.merlin.server.services;

import java.util.List;
import java.util.Map;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.remotes.ResultsCellRepository;
import org.apache.commons.lang3.tuple.Pair;

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

  @Override
  public Map<String, List<Pair<Duration, SerializedValue>>> getResourceSamples(final PlanId planId, final RevisionData revisionData) {
    return this.store.lookup(planId) // Only return results that have already been cached
        .map(ResultsProtocol.ReaderRole::get)
        .map(state -> {
            if (!(state instanceof final ResultsProtocol.State.Success s)) return null;
            return s.results().resourceSamples;
        })
        .orElseGet(Map::of);
  }
}
