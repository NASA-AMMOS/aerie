package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.scheduler.server.ResultsProtocol;
import gov.nasa.jpl.aerie.scheduler.server.remotes.ResultsCellRepository;

public record CachedSchedulerService(
    ResultsCellRepository store,
    SchedulerAgent agent
) implements SchedulerService {
  @Override
  public ResultsProtocol.State scheduleActivities(final ScheduleRequest request) {
    final var cell$ = this.store.lookup(request.specificationId());
    if (cell$.isPresent()) {
      return cell$.get().get();
    } else {
      // Allocate a fresh cell.
      final var cell = this.store.allocate(request.specificationId());

      // Split the cell into its two concurrent roles, and delegate the writer role to another process.
      final ResultsProtocol.ReaderRole reader;
      try {
        final ResultsProtocol.WriterRole writer = cell;
        reader = cell;

        this.agent.schedule(request, writer);
      } catch (final InterruptedException ex) {
        // If we couldn't delegate, clean up the cell and return an Incomplete.
        this.store.deallocate(cell);
        return new ResultsProtocol.State.Incomplete(cell.get().analysisId());
      }

      // Return the current value of the reader; if it's incomplete, the caller can check it again later.
      return reader.get();
    }
  }
}
