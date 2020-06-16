package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Projection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Time;
import org.apache.commons.lang3.tuple.Pair;
import org.pcollections.HashTreePMap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

/**
 * A {@link Projection} from events to operators over {@link Time} points.
 *
 * <p>
 * A <i>reactor</i> is an entity which responds to an event at a point in time by producing more events.
 * The <code>MasterReaactor</code> is a collection of reactors which will be run concurrently in response to an event.s
 * The original event is preserved, and all reaction products occur after it.
 * </p>
 *
 * @param <T> The abstract type of the timeline owning the time points to step over.
 * @param <Event> The type of events that may occur over the timeline.
 */
public final class MasterReactor<T, Event>
    implements Projection<Event, Task<T, Event>>
{
  private final List<Function<Event, Task<T, Event>>> reactors = new ArrayList<>();

  public void addReactor(final Function<Event, Task<T, Event>> reactor) {
    this.reactors.add(reactor);
  }

  @Override
  public Task<T, Event> empty() {
    return time -> Pair.of(time, HashTreePMap.empty());
  }

  @Override
  public Task<T, Event> sequentially(final Task<T, Event> prefix, final Task<T, Event> suffix) {
    return time -> {
      final var result1 = prefix.apply(time);
      final var result2 = suffix.apply(result1.getLeft());
      return Pair.of(
          result2.getLeft(),
          result1.getRight().plusAll(result2.getRight()));
    };
  }

  @Override
  public Task<T, Event> concurrently(final Task<T, Event> left, final Task<T, Event> right) {
    return time -> {
      final var fork = time.fork();
      final var result1 = left.apply(fork);
      final var result2 = right.apply(fork);
      return Pair.of(
          result1.getLeft().join(result2.getLeft()),
          result1.getRight().plusAll(result2.getRight()));
    };
  }

  @Override
  public Task<T, Event> atom(final Event event) {
    return time -> {
      // Re-emit the given event, or else it will disappear into the ether.
      time = time.emit(event);

      // Build a stack of unmerged branches.
      final var stack = new ArrayDeque<Time<T, Event>>(this.reactors.size());
      final var scheduled = new HashMap<String, ScheduleItem<T, Event>>();
      for (final var reactor : this.reactors.subList(0, this.reactors.size())) {
        time = time.fork();
        final var result = reactor.apply(event).apply(time);

        stack.push(result.getLeft());
        scheduled.putAll(result.getRight());
      }

      // Merge the built stack of branches down into a single joined time point.
      while (!stack.isEmpty()) time = stack.pop().join(time);

      return Pair.of(time, HashTreePMap.from(scheduled));
    };
  }
}
