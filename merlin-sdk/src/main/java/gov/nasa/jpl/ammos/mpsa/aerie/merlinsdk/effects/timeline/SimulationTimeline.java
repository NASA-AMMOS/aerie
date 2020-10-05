package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EffectExpression;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EffectTrait;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Projection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * A persistent representation of {@link EffectExpression}s.
 *
 * <p>
 * {@code SimulationTimeline} is an efficient implementation of {@link EffectExpression} that supports multiple
 * simultaneous unterminated timelines. The equivalent of an {@link EventGraph} for <code>SimulationTimeline</code>s
 * is a {@link History}, which may be extended with events and evaluated against {@link Projection}s.
 * </p>
 *
 * <p>
 * For efficiency, {@link Projection}s may be registered with a {@code SimulationTimeline} so their results
 * are cached and reused for later queries.
 * </p>
 *
 * <p>
 * The <a href="https://kean.blog/post/phantom-types">phantom type parameter</a> {@code T} distinguishes
 * one timeline from another, ensuring that time points and projectors related to one timeline cannot accidentally
 * be used with another timeline. {@code SimulationTimeline} instances are instantiated with an unknown type
 * for this type parameter, represented by the wildcard symbol {@code <?>}.
 * </p>
 *
 * @param <T> A phantom type parameter that distinguishes individual timeline instances.
 * @param <Event> The type of events that may occur over the timeline.
 * @see <a href="https://en.wikipedia.org/wiki/Persistent_data_structure">Wikipedia: Persistent data structure</a>
 * @see History
 * @see Query
 * @see EventGraph
 */
public final class SimulationTimeline<T, Event> {
  private SimulationTimeline() {}

  private final List<EventPoint<Event>> times = new ArrayList<>();
  private int nextTime = 0;

  // SAFETY: -1 is not a legal index for a time point.
  /*package-local*/ static final int START_INDEX = -1;

  // Use a wildcard as an existential quantifier. The caller cannot know what concrete type this is instantiated over,
  //   so two separate SimulationTimeline instances cannot interfere with each other.
  public static <Event> SimulationTimeline<?, Event> create() {
    return new SimulationTimeline<>();
  }

  public History<T, Event> origin() {
    return new History<>(this, null, START_INDEX);
  }

  public <Model, Effect> Query<T, Event, Model> register(
      final Projection<Event, Effect> projection,
      final Applicator<Effect, Model> applicator
  ) {
    return new Query<>(this, projection, applicator);
  }

  /* package-local */
  int advancing(final int previous, final Event event) {
    final var nextTime = this.nextTime++;
    this.times.add(nextTime, new EventPoint.Advancing<>(previous, event));
    return nextTime;
  }

  /* package-local */
  int joining(final int base, final int left, final int right) {
    final var nextTime = this.nextTime++;
    this.times.add(nextTime, new EventPoint.Joining<>(base, left, right));
    return nextTime;
  }

  /* package-local */
  int waiting(final int previous, final long microseconds) {
    final var nextTime = this.nextTime++;
    this.times.add(nextTime, new EventPoint.Waiting<>(previous, microseconds));
    return nextTime;
  }

  /* package-local */
  EventPoint<Event> get(final int index) {
    return this.times.get(index);
  }

  // PRECONDITION: `startTime` occurs-before `endTime`.
  //   This will enter an infinite loop if `startTime` and `endTime` are incomparable or occur in the opposite order.
  /* package-local */
  <Effect> Collection<Pair<Duration, Effect>> evaluate(
      final EffectTrait<Effect> trait,
      final Function<Event, Effect> substitution,
      final int startTime,
      final int endTime
  ) {
    // NOTE: In principle, we can determine the maximum size of the path stack.
    //   Whenever two time points are joined, increment a counter on the resulting time point.
    //   This counter can then be used to allocate a stack of just the right size.
    final var pathStack = new ArrayDeque<ActivePath<Effect>>();
    var currentPath = (ActivePath<Effect>) new ActivePath.TopLevel<>(startTime, trait.empty());
    var pointIndex = endTime;

    // TERMINATION: In principle, we can bound this loop by determining the maximum number
    //   of time points we will visit. Whenever a new time point is generated from an old one,
    //   its count would be updated appropriately. (Emitting an event adds one; joining two branches
    //   adds the branches and subtracts the base.)
    while (true) {
      if (currentPath.basePoint() != pointIndex) {
        // There's still more path to follow!
        final var point = this.times.get(pointIndex);
        if (point instanceof EventPoint.Advancing) {
          // Accumulate the event into the currently open path.
          final var step = (EventPoint.Advancing<Event>) point;
          currentPath.accumulate(next -> trait.sequentially(substitution.apply(step.event), next));
          pointIndex = step.previous;
        } else if (point instanceof EventPoint.Joining) {
          // We've walked backwards into a join point between two branches.
          // Walk down the left side first, and stash the base and right side for later evaluation.
          final var join = (EventPoint.Joining<Event>) point;
          pathStack.push(currentPath);
          currentPath = new ActivePath.Left<>(join.base, trait.empty(), join.right);
          pointIndex = join.left;
        } else if (point instanceof EventPoint.Waiting) {
          // We've walked backwards into a delay.
          // SAFETY: Delays can only occur at the top-level.
          assert currentPath instanceof ActivePath.TopLevel;
          final var path = (ActivePath.TopLevel<Effect>) currentPath;
          final var wait = (EventPoint.Waiting<Event>) point;

          path.effects.addFirst(Pair.of(Duration.of(wait.microseconds, Duration.MICROSECONDS), path.effect));
          path.effect = trait.empty();
          pointIndex = wait.previous;
        }
      } else if (currentPath instanceof ActivePath.Left) {
        // We've just finished evaluating the left side of a concurrence.
        // Stash the result and switch to the right side.
        final var path = (ActivePath.Left<Effect>) currentPath;
        currentPath = new ActivePath.Right<>(path.base, path.left, trait.empty());
        pointIndex = path.right;
      } else if (currentPath instanceof ActivePath.Right) {
        // We've just finished evaluating the right side of a concurrence.
        // We already evaluated the left side, so bind them together and accumulate the result
        //   into the open path one level up. We'll continue from the given base point.
        final var path = (ActivePath.Right<Effect>) currentPath;
        currentPath = pathStack.pop();
        currentPath.accumulate(next -> trait.sequentially(trait.concurrently(path.left, path.right), next));
        pointIndex = path.base;
      } else if (currentPath instanceof ActivePath.TopLevel) {
        // We've just finished the top-level path -- we're done!
        final var path = (ActivePath.TopLevel<Effect>) currentPath;
        path.effects.addFirst(Pair.of(Duration.ZERO, path.effect));
        return path.effects;
      }
    }
  }
}
