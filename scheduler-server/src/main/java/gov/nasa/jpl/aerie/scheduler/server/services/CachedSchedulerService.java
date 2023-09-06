package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.scheduler.server.ResultsProtocol;
import gov.nasa.jpl.aerie.scheduler.server.remotes.ResultsCellRepository;

public record CachedSchedulerService(
    ResultsCellRepository store
) implements SchedulerService {

  @Override
  public ResultsProtocol.State getScheduleResults(final ScheduleRequest request, final String requestedBy) {
    final var specificationId = request.specificationId();
    final var cell$ = this.store.lookup(specificationId);
    if (cell$.isPresent()) {
      return cell$.get().get();
    } else {
      // Allocate a fresh cell.
      final var cell = this.store.allocate(specificationId, requestedBy);

      // Return the current value of the reader; if it's incomplete, the caller can check it again later.
      return cell.get();
    }
  }
}
