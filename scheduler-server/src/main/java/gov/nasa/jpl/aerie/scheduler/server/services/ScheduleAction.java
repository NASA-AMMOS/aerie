package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.services.UnexpectedSubtypeError;
import gov.nasa.jpl.aerie.scheduler.server.ResultsProtocol;

import java.io.IOException;
import java.util.Objects;

/**
 * represents the query for the results of a scheduling run
 *
 * the query can be run using the scheduling service configured at construction, possibly resulting in the activation of
 * a scheduling agent to produce the results (depending on caching etc)
 *
 * @param merlinService interface to merlin for any necessary plan details
 * @param schedulerService scheduling service that handles activity scheduling requests
 */
//TODO: how to handle successive scheduling requests (probably synchronous per-plan, but not service-wide)
//TODO: how to fetch in-progress status without risking mutating the plan again (since scheduling is NOT idempotent)
public record ScheduleAction(MerlinService merlinService, SchedulerService schedulerService) {
  public ScheduleAction {
    Objects.requireNonNull(merlinService);
    Objects.requireNonNull(schedulerService);
  }

  /**
   * common interface for different possible results of the query
   */
  //tempting to unify with the overlapping ResultProtocol.State, but kept to parallel merlin...GetSimulationResultsAction
  public interface Response {
    /**
     * scheduler has not completed running
     */
    record Incomplete() implements Response {}

    /**
     * scheduler completed unsuccessfully, eg in an error or canceled
     */
    record Failed(String reason) implements Response {}

    /**
     * scheduler completed successfully; contains the requested results
     */
    record Complete(ScheduleResults results) implements Response {}
  }

  /**
   * execute the scheduling operation on the target plan (or retrieve existing scheduling results)
   *
   * @param planId identifier of the plan to start scheduling from and to store scheduled output into
   * @return a response object wrapping summary results of the run (either successful or not)
   * @throws NoSuchPlanException if the target plan could not be found
   */
  public Response run(final String planId) throws NoSuchPlanException, IOException {
    //record the plan revision as of the scheduling request time (in case work commences much later eg in worker thread)
    //TODO may also need to verify the model revision / other volatile metadata matches one from request
    final long planRev = this.merlinService.getPlanMetadata(planId).planRev();

    //submit request to run scheduler (possibly asynchronously or even cached depending on service)
    final var response = this.schedulerService.scheduleActivities(new ScheduleRequest(planId, planRev));

    return repackResponse(response);
  }

  /**
   * convert the response object from the scheduler service into a response for this action
   *
   * @param response the bare scheduling result contents
   * @return scheduling results augmented with any extra post-analysis
   */
  private Response repackResponse(ResultsProtocol.State response) {
    //the two responses are identical for now, but kept in order to remain parallel to merlin...GetSimulationResultsAction
    if (response instanceof ResultsProtocol.State.Incomplete) {
      return new Response.Incomplete();
    } else if (response instanceof ResultsProtocol.State.Failed r) {
      return new Response.Failed(r.reason());
    } else if (response instanceof ResultsProtocol.State.Success r) {
      final var results = r.results();
      //NB: could elaborate with more response content here, like merlin...SimResults adds in violation analytics
      return new Response.Complete(results);
    } else {
      throw new UnexpectedSubtypeError(ResultsProtocol.State.class, response);
    }
  }

}
