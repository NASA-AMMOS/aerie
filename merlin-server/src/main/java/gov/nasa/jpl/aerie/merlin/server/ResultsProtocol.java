package gov.nasa.jpl.aerie.merlin.server;

import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;

public final class ResultsProtocol {
  private ResultsProtocol() {}

  public /*sealed*/ interface State {
    /** Simulation in enqueued. */
    record Pending() implements State {}

    /** Simulation in progress, but no results to share yet. */
    record Incomplete() implements State {}

    /** Simulation complete -- results now available. */
    record Success(SimulationResults results) implements State {}

    /** Simulation failed -- don't try to re-run without changing some of the inputs. */
    record Failed(String reason) implements State {}
  }

  public interface ReaderRole {
    State get();

    /** After calling cancel, `get` is no longer legal to invoke. */
    void cancel();
  }

  public interface WriterRole {
    boolean isCanceled();

    // If the writer aborts because it observes `isCanceled()`,
    //   it must still complete with `failWith()`.
    //   Otherwise, the reader would not be able to reclaim unique ownership
    //   of the underlying resource in order to deallocate it.
    void succeedWith(SimulationResults results);
    void failWith(String reason);
  }

  public interface OwnerRole extends ReaderRole, WriterRole {}
}
