package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.remotes.ResultsCellRepository;

public record CachedSimulationService (
    ResultsCellRepository store,
    SimulationAgent agent
) implements SimulationService {
  @Override
  public ResultsProtocol.State getSimulationResults(final String planId, final RevisionData revisionData) {
    final var cell$ = this.store.lookup(planId);
    if (cell$.isPresent()) {
      return cell$.get().get();
    } else {
      // Allocate a fresh cell.
      final var cell = this.store.allocate(planId);

      // Split the cell into its two concurrent roles, and delegate the writer role to another process.
      final ResultsProtocol.ReaderRole reader;
      try {
        final ResultsProtocol.WriterRole writer = cell;
        reader = cell;

        this.agent.simulate(planId, revisionData, writer);
      } catch (final InterruptedException ex) {
        // If we couldn't delegate, clean up the cell and return an Incomplete.
        this.store.deallocate(cell);
        return new ResultsProtocol.State.Incomplete();
      }

      // Return the current value of the reader; if it's incomplete, the caller can check it again later.
      return reader.get();
    }
  }
}
