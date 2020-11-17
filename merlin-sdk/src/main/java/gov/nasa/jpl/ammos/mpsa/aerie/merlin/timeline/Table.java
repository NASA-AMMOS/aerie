package gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.effects.Applicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.effects.Projection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.SimulationTimeline.START_INDEX;

/* package-local */
final class Table<$Timeline, Event, Effect, Model> {
  private final SimulationTimeline<$Timeline, Event> database;
  private final Projection<Event, Effect> projection;
  private final Applicator<Effect, Model> applicator;
  private final int tableIndex;

  private final Map<Integer, Model> cache = new HashMap<>();
  private final ArrayList<Event> events = new ArrayList<>();

  public Table(
      final SimulationTimeline<$Timeline, Event> database,
      final Projection<Event, Effect> projection,
      final Applicator<Effect, Model> applicator,
      final int tableIndex)
  {
    this.database = database;
    this.projection = projection;
    this.applicator = applicator;
    this.tableIndex = tableIndex;
  }

  public void clearCache() {
    this.cache.clear();
  }

  public int emit(final Event event) {
    final var eventIndex = this.events.size();
    this.events.add(event);
    return eventIndex;
  }

  public Event getEvent(int index) {
    return this.events.get(index);
  }

  public Model getAt(final History<$Timeline, Event> history) {
    // If we already have a model cached for this time point, we can just bail now.
    if (history.getIndex() == START_INDEX) {
      return this.applicator.initial();
    } else if (this.cache.containsKey(history.getIndex())) {
      return this.cache.get(history.getIndex());
    }

    // Look for a cached model anytime back to our most recent branch point, if any.
    final var baseIndex = (history.getLastBranchBase() != null) ? history.getLastBranchBase().getIndex() : START_INDEX;
    var previousIndex = history.getIndex();
    while (previousIndex != baseIndex && !this.cache.containsKey(previousIndex)) {
      previousIndex = this.database.get(previousIndex).getPrevious();
    }

    final Model model;
    if (previousIndex == baseIndex) {
      // We didn't find anything. We'll have to query our previous segment for its model.
      if (history.getLastBranchBase() != null) {
        model = this.applicator.duplicate(this.getAt(history.getLastBranchBase()));
      } else {
        // Well, we don't have a previous segment. Start with the initial model.
        model = this.applicator.initial();
      }
    } else {
      // We found something! Since we have exclusive ownership over our branch segment, we'll remove it from the cache,
      // update it directly, and put it back in the cache at our current time point.
      model = this.cache.remove(previousIndex);
    }

    // Compute the effects that have occurred since our last update on this branch.
    final var effects = this.evaluate(previousIndex, history.getIndex());

    // Step this model up to the current point in time.
    for (final var effect : effects) {
      this.applicator.step(model, effect.getKey());
      this.applicator.apply(model, effect.getValue());
    }

    // Cache this model for future queries.
    this.cache.put(history.getIndex(), model);

    return model;
  }

  // PRECONDITION: `startTime` occurs-before `endTime`.
  //   This will enter an infinite loop if `startTime` and `endTime` are incomparable or occur in the opposite order.
  /* package-local */
  private Collection<Pair<Duration, Effect>> evaluate(final int startTime, final int endTime) {
    // NOTE: In principle, we can determine the maximum size of the path stack.
    //   Whenever two time points are joined, increment a counter on the resulting time point.
    //   This counter can then be used to allocate a stack of just the right size.
    final var pathStack = new ArrayDeque<ActivePath<Effect>>();
    var currentPath = (ActivePath<Effect>) new ActivePath.TopLevel<>(startTime, this.projection.empty());
    var pointIndex = endTime;

    // TERMINATION: In principle, we can bound this loop by determining the maximum number
    //   of time points we will visit. Whenever a new time point is generated from an old one,
    //   its count would be updated appropriately. (Emitting an event adds one; joining two branches
    //   adds the branches and subtracts the base.)
    while (true) {
      if (currentPath.basePoint() != pointIndex) {
        // There's still more path to follow!
        final var point = this.database.get(pointIndex);
        if (point instanceof EventPoint.Advancing) {
          // Accumulate the event into the currently open path.
          final var step = (EventPoint.Advancing) point;
          if (step.tableIndex == this.tableIndex) {
            final var event = this.getEvent(step.eventIndex);
            currentPath.accumulate(next -> this.projection.sequentially(this.projection.atom(event), next));
          }
          pointIndex = step.previous;
        } else if (point instanceof EventPoint.Joining) {
          // We've walked backwards into a join point between two branches.
          // Walk down the left side first, and stash the base and right side for later evaluation.
          final var join = (EventPoint.Joining) point;
          pathStack.push(currentPath);
          currentPath = new ActivePath.Left<>(join.base, this.projection.empty(), join.right);
          pointIndex = join.left;
        } else if (point instanceof EventPoint.Waiting) {
          // We've walked backwards into a delay.
          // SAFETY: Delays can only occur at the top-level.
          assert currentPath instanceof ActivePath.TopLevel;
          final var path = (ActivePath.TopLevel<Effect>) currentPath;
          final var wait = (EventPoint.Waiting) point;

          path.effects.addFirst(Pair.of(Duration.of(wait.microseconds, Duration.MICROSECONDS), path.effect));
          path.effect = this.projection.empty();
          pointIndex = wait.previous;
        }
      } else if (currentPath instanceof ActivePath.Left) {
        // We've just finished evaluating the left side of a concurrence.
        // Stash the result and switch to the right side.
        final var path = (ActivePath.Left<Effect>) currentPath;
        currentPath = new ActivePath.Right<>(path.base, path.left, this.projection.empty());
        pointIndex = path.right;
      } else if (currentPath instanceof ActivePath.Right) {
        // We've just finished evaluating the right side of a concurrence.
        // We already evaluated the left side, so bind them together and accumulate the result
        //   into the open path one level up. We'll continue from the given base point.
        final var path = (ActivePath.Right<Effect>) currentPath;
        currentPath = pathStack.pop();
        currentPath.accumulate(next -> this.projection.sequentially(this.projection.concurrently(path.left, path.right), next));
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
