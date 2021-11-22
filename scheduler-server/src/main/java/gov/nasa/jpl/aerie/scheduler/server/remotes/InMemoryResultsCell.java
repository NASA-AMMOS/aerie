package gov.nasa.jpl.aerie.scheduler.server.remotes;

import gov.nasa.jpl.aerie.scheduler.server.ResultsProtocol;
import gov.nasa.jpl.aerie.scheduler.server.services.ScheduleResults;

/**
 * container for storing scheduling result state locally in memory
 */
//TODO: unify (via type parameters?) with merlin.server.remotes.InMemoryResultsCellRepository.InMemoryCell
public class InMemoryResultsCell implements ResultsProtocol.OwnerRole {

  /**
   * indicates that all interest in the result has been canceled
   */
  private volatile boolean canceled = false;

  /**
   * the stored result of the scheduling request
   */
  private volatile ResultsProtocol.State state = new ResultsProtocol.State.Incomplete();

  /**
   * {@inheritDoc}
   */
  @Override
  public ResultsProtocol.State get() {
    return this.state;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void cancel() {
    this.canceled = true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isCanceled() {
    return this.canceled;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void succeedWith(final ScheduleResults results) {
    if (this.state instanceof ResultsProtocol.State.Incomplete) {
      this.state = new ResultsProtocol.State.Success(results);
    } else {
      throw new IllegalStateException("Cannot transition to success state from state %s".formatted(
          this.state.getClass().getCanonicalName()));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void failWith(final String reason) {
    if (this.state instanceof ResultsProtocol.State.Incomplete) {
      this.state = new ResultsProtocol.State.Failed(reason);
    } else {
      throw new IllegalStateException("Cannot transition to failed state from state %s".formatted(
          this.state.getClass().getCanonicalName()));
    }
  }

}
