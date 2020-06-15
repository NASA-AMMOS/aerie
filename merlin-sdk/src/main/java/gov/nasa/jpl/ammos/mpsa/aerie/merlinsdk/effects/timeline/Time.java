package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EffectTrait;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Projection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.function.Function;

/**
 * A point in time in a {@link SimulationTimeline}.
 *
 * <p>
 * A <code>Time</code> represents a point in time in a <code>SimulationTimeline</code>, tracking all of the events
 * that have occurred before that point in time. Use a {@link Query} to interpret these events over time
 * as knowledge in some domain.
 * </p>
 *
 * <p>
 * New events can be {@link #emit}ted after any point in time, establishing a <i>new</i> point in time that observes
 * the same timeline with a new event added at the end. Similarly, a duration of time may be a{@link #wait}ed,
 * producing a new point in time separated by the previous by the given span of time.
 * </p>
 *
 * <p>
 * Times may also be {@link #fork}ed, allowing timelines diverging from the same point in time
 * to be re{@link #join}ed. The resulting join point is a point in time that observes the events on both branches.
 * </p>
 *
 * @param <Scope> The abstract type of the timeline owning this time point.
 * @param <Event> The type of events that may occur over the timeline.
 * @see SimulationTimeline
 * @see Query
 */
public final class Time<Scope, Event> {
  /**
   * The database governing this time point.
   */
  private final SimulationTimeline<Scope, Event> database;

  /**
   * The index of the current point in time in the SimulationTimeline database.
   */
  private final int index;

  /**
   * The point in time that this point in time was branched from.
   *
   * Only points in time sharing this scope point may be joined together.
   */
  private final Time<Scope, Event> lastBranchBase;

  /* package-local */
  Time(final SimulationTimeline<Scope, Event> database, final Time<Scope, Event> lastBranchBase, final int index) {
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

  /**
   * Append a new event to the timeline.
   *
   * @param event The event to perform after the current time point.
   * @return The time point when the event occurs.
   */
  public Time<Scope, Event> emit(final Event event) {
    return new Time<>(this.database, this.lastBranchBase, database.advancing(this.index, event));
  }

  /**
   * Create a time point from which two divergent branches may be drawn and later {@link #join}ed.
   *
   * @return The base time point from which two branches may be drawn.
   */
  public Time<Scope, Event> fork() {
    return new Time<>(this.database, this, this.index);
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
  public Time<Scope, Event> join(final Time<Scope, Event> other) {
    if (this.lastBranchBase == null || this.lastBranchBase != other.lastBranchBase) {
      throw new RuntimeException("Cannot join branches that did not fork from the same point");
    } else if (this.index == this.lastBranchBase.index) {
      return new Time<>(other.database, other.lastBranchBase.lastBranchBase, other.index);
    } else if (other.index == other.lastBranchBase.index) {
      return new Time<>(this.database, this.lastBranchBase.lastBranchBase, this.index);
    }

    return new Time<>(this.database, this.lastBranchBase.lastBranchBase, this.database.joining(this.lastBranchBase.index, this.index, other.index));
  }

  public <Effect> Collection<Pair<Duration, Effect>> evaluate(final EffectTrait<Effect> trait, final Function<Event, Effect> substitution) {
    return this.database.evaluate(trait, substitution, this.index);
  }

  public <Effect> Collection<Pair<Duration, Effect>> evaluate(final Projection<Event, Effect> projection) {
    return this.database.evaluate(projection, projection::atom, this.index);
  }

  /**
   * Wait a span of time.
   *
   * @param duration The amount of time to wait.
   * @return A time point observing this time point followed by a span of time.
   */
  public Time<Scope, Event> wait(final Duration duration) {
    if (this.lastBranchBase != null) {
      throw new RuntimeException("Cannot wait on an unmerged branch");
    } else if (duration.compareTo(Duration.ZERO) < 0) {
      throw new RuntimeException("Cannot wait for a negative amount of time");
    } else if (duration.compareTo(Duration.ZERO) == 0) {
      return this;
    }

    return new Time<>(this.database, this.lastBranchBase, this.database.waiting(this.index, duration.durationInMicroseconds));
  }

  /**
   * A function that steps time forward.
   *
   * <p>
   * An <code>Operator</code> is a function that takes a time point, performs operations on it (e.g. {@code Time#emit},
   * {@code Time#wait}), and returns a time point capturing its effects.
   * </p>
   *
   * @param <T> The abstract type of the timeline owning the input and output time points.
   * @param <Event> The type of events that may occur over the timeline.
   */
  public interface Operator<T, Event> extends Function<Time<T, Event>, Time<T, Event>> {
    /**
     * Perform two <code>Operator</code>s sequentially.
     *
     * @param other The <code>Operator</code> to perform after this one.
     * @return An <code>Operator</code> with the combined effects of both input <code>Operator</code>s.
     */
    default Operator<T, Event> then(final Operator<T, Event> other) {
      return time -> other.apply(this.apply(time));
    }

    /**
     * Perform two <code>Operator</code>s concurrently.
     *
     * @param other The <code>Operator</code> to perform alongside this one.
     * @return An <code>Operator</code> with the combined effects of both input <code>Operator</code>s.
     */
    default Operator<T, Event> join(final Operator<T, Event> other) {
      return time -> {
        final var fork = time.fork();
        return this.apply(fork).join(other.apply(fork));
      };
    }
  }

  /**
   * An {@link EffectTrait} on {@link Operator}s.
   *
   * @param <T> The abstract type of the timeline owning the times to be operated over.
   * @param <Event> The type of events that may occur over the timeline.
   */
  public static class OperatorTrait<T, Event> implements EffectTrait<Operator<T, Event>> {
    @Override
    public Operator<T, Event> empty() {
      return time -> time;
    }

    @Override
    public Operator<T, Event> sequentially(final Operator<T, Event> prefix, final Operator<T, Event> suffix) {
      return prefix.then(suffix);
    }

    @Override
    public Operator<T, Event> concurrently(final Operator<T, Event> left, final Operator<T, Event> right) {
      return left.join(right);
    }
  }
}
