package gov.nasa.jpl.aerie.scheduler.server;

import gov.nasa.jpl.aerie.scheduler.server.services.ScheduleResults;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * interfaces used to coordinate parties interested in the scheduling results
 *
 * (following example of merlin.server.ResultsProtocol)
 */
//TODO: see if this could reduce to some flavor of a java.util.concurrent.CompletableFuture<>
public final class ResultsProtocol {
  private ResultsProtocol() {}

  /**
   * common interface for different possible results of scheduling
   */
  //TODO: unify with ScheduleAction.Response which has overlapping options/data
  public interface State {
    /**
     * scheduling in progress, full results not yet available
     */
    //TODO: could probably provide some partial results
    record Incomplete() implements State {}

    /**
     * scheduling completed successfully, contains the full results
     *
     * @param results the results of the scheduling run
     */
    record Success(ScheduleResults results) implements State {}

    /**
     * scheduling failed; likely need to change inputs before re-running
     *
     * @param reason description of why the scheduling operation failed
     */
    record Failed(String reason) implements State {}
  }

  /**
   * observer for a schedule result
   */
  public interface ReaderRole {

    /**
     * retrieve the current status of the scheduling run, including if it is still in progress
     *
     * the scheduling run must not have been previously cancel()ed
     *
     * @return the status of the scheduling run
     */
    State get();

    /**
     * cancel the interest in the result of the scheduling run
     *
     * it is illegal to call get() after a cancel() call
     */
    //TODO: determine if this also kills the actual run itself (ie should plan possibly mutate after a cancel?)
    void cancel();
  }

  /**
   * producer for a scheduling result
   */
  public interface WriterRole {

    /**
     * flag to check to see if all interest in the result has been cancelled
     *
     * the producer should still call failWith() after it notices a cancellation
     *
     * @return true iff a cancel has been invoked on the corresponding reader
     */
    //TODO: determine if this also kills the actual run itself (ie should plan possibly mutate after a cancel?)
    boolean isCanceled();

    /**
     * mark the scheduling run as fully complete and attach the given results
     *
     * @param results the summary results of the scheduling run, including satisfaction metrics etc
     */
    void succeedWith(ScheduleResults results);

    /**
     * mark the scheduling run as having failed with the given reason
     *
     * @param reason the reason that the scheduling run failed
     */
    void failWith(String reason);

    /**
     * convenience method for reporting an unhandled exception
     *
     * @param throwable the exception that caused the scheduling run to fail
     */
    default void failWith(final Throwable throwable) {
      final var stringWriter = new StringWriter();
      throwable.printStackTrace(new PrintWriter(stringWriter));
      this.failWith(stringWriter.toString());
    }
  }

  /**
   * interface that can both read and write to a scheduling result object
   */
  public interface OwnerRole extends ReaderRole, WriterRole {}
}
