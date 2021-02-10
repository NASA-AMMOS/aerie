package gov.nasa.jpl.aerie.merlin.timeline;

import gov.nasa.jpl.aerie.time.Duration;

import java.util.List;
import java.util.function.Function;

import static gov.nasa.jpl.aerie.merlin.timeline.SimulationTimeline.START_INDEX;

/**
 * A view on prior events in a {@link SimulationTimeline}.
 *
 * <p>
 * A <code>History</code> represents a point in time in a {@link SimulationTimeline}, tracking all of the events
 * that have occurred before that point in time. Use a {@link Query} to interpret these events over time as knowledge
 * in some domain.
 * </p>
 *
 * <p>
 * New events can be {@link #emit}ted after any point in time, establishing a <i>new</i> point in time that observes
 * the same timeline with a new event added at the end. Similarly, a duration of time may be a{@link #wait}ed,
 * producing a new point in time separated by the previous by the given span of time.
 * </p>
 *
 * <p>
 * Histories may also be {@link #fork}ed, allowing timelines diverging from the same point in time
 * to be re{@link #join}ed. The resulting join point is a point in time that observes the events on both branches.
 * </p>
 *
 * @param <$Timeline> The abstract type of the timeline owning this time point.
 * @see SimulationTimeline
 * @see Query
 */
public final class History<$Timeline> {
  /**
   * The database governing this time point.
   */
  private final SimulationTimeline<$Timeline> database;

  /**
   * The index of the current point in time in the SimulationTimeline database.
   */
  private final int index;

  /**
   * The point in time that this point in time was branched from.
   *
   * Only points in time sharing this scope point may be joined together.
   */
  private final History<$Timeline> lastBranchBase;

  /* package-local */
  History(final SimulationTimeline<$Timeline> database, final History<$Timeline> lastBranchBase, final int index) {
    this.database = database;
    this.lastBranchBase = lastBranchBase;
    this.index = index;
  }

  /**
   * Get the index of this point in time in the event database.
   *
   * @return The index of this point in time in the event database.
   */
  /* package-local */
  int getIndex() {
    return this.index;
  }

  /* package-local */
  History<$Timeline> getLastBranchBase() {
    return this.lastBranchBase;
  }

  /**
   * Append a new event to the timeline.
   *
   * @param event The event to perform after the current time point.
   * @return The time point when the event occurs.
   */
  public <Event> History<$Timeline> emit(final Event event, final Query<? super $Timeline, Event, ?> query) {
    return new History<>(this.database, this.lastBranchBase, this.database.advancing(this.index, query, event));
  }

  /**
   * Create a time point from which two divergent branches may be drawn and later {@link #join}ed.
   *
   * @return The base time point from which two branches may be drawn.
   */
  public History<$Timeline> fork() {
    return new History<>(this.database, this, this.index);
  }

  public <Event, Model> Model ask(final Query<? super $Timeline, Event, Model> query) {
    return this.database.<Event, Model>getTable(query.getTableIndex()).getAt(this);
  }

  /**
   * Fork and join two divergent branches in one fell swoop.
   *
   * <p>
   *   This method is equivalent to <code>left.apply(history.fork()).join(right.apply(history.fork()))</code>.
   * </p>
   *
   * @param left A function drawing out one branch of the timeline.
   * @param right A function drawing out the other branch of the timeline.
   * @return A time point observing both generated branches in its past.
   */
  public History<$Timeline> branching(
      final Function<History<$Timeline>, History<$Timeline>> left,
      final Function<History<$Timeline>, History<$Timeline>> right)
  {
    return left.apply(this.fork()).join(right.apply(this.fork()));
  }

  /**
   * Create a time point by joining this time point with a sibling {@link #fork}ed from the same base.
   *
   * <p>
   * Timelines that have not been forked, or which are not forked from the same base, cannot be joined.
   * </p>
   *
   * @param other The sibling branch of time to join with this branch.
   * @return A time point observing both branches in its past.
   */
  public History<$Timeline> join(final History<$Timeline> other) {
    if (this.lastBranchBase == null || this.lastBranchBase != other.lastBranchBase) {
      throw new RuntimeException("Cannot join branches that did not fork from the same point");
    } else if (this.index == this.lastBranchBase.index) {
      return new History<>(other.database, other.lastBranchBase.lastBranchBase, other.index);
    } else if (other.index == other.lastBranchBase.index) {
      return new History<>(this.database, this.lastBranchBase.lastBranchBase, this.index);
    }

    return new History<>(this.database, this.lastBranchBase.lastBranchBase, this.database.joining(this.lastBranchBase.index, this.index, other.index));
  }

  /**
   * Wait a span of time.
   *
   * @param duration The amount of time to wait.
   * @return A time point observing this time point followed by a span of time.
   */
  public History<$Timeline> wait(final Duration duration) {
    if (this.lastBranchBase != null) {
      throw new RuntimeException("Cannot wait on an unmerged branch");
    } else if (duration.isNegative()) {
      throw new RuntimeException("Cannot wait for a negative amount of time");
    } else if (duration.isZero()) {
      return this;
    }

    return new History<>(this.database, this.lastBranchBase, this.database.waiting(this.index, duration.in(Duration.MICROSECONDS)));
  }

  /**
   * Wait a span of time.
   *
   * @param quantity The number of units of time to wait.
   * @param unit The unit of time to measure out.
   * @return A time point observing this time point followed by a span of time.
   */
  public History<$Timeline> wait(final long quantity, final Duration unit) {
    return this.wait(Duration.of(quantity, unit));
  }

  public String getDebugTrace() {
    final var builder = new StringBuilder();

    var durationFromStart = Duration.ZERO;
    for (final var point : this.database.evaluate(new EventGraphProjection<>(), START_INDEX, this.index)) {
      durationFromStart = durationFromStart.plus(point.getKey());
      builder.append(String.format("%10s: %s\n", durationFromStart, point.getValue()));
    }

    return builder.toString();
  }

  public
  boolean isStrictlyAheadOfOn(final History<$Timeline> past, final List<Query<? super $Timeline, ?, ?>> queries) {
    return this.database.isStrictlyAheadOfOn(this.index, past.index, queries);
  }
}
