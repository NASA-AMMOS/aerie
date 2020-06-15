package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Projection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Time;

import java.util.ArrayDeque;
import java.util.ArrayList;
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
    implements Projection<Event, Function<Time<T, Event>, Time<T, Event>>>
{
  private final List<Function<Event, Function<Time<T, Event>, Time<T, Event>>>> reactors = new ArrayList<>();

  public void addReactor(final Function<Event, Function<Time<T, Event>, Time<T, Event>>> reactor) {
    this.reactors.add(reactor);
  }

  @Override
  public Function<Time<T, Event>, Time<T, Event>> empty() {
    return time -> time;
  }

  @Override
  public Function<Time<T, Event>, Time<T, Event>> sequentially(
      final Function<Time<T, Event>, Time<T, Event>> prefix,
      final Function<Time<T, Event>, Time<T, Event>> suffix
  ) {
    return time -> suffix.apply(prefix.apply(time));
  }

  @Override
  public Function<Time<T, Event>, Time<T, Event>> concurrently(
      final Function<Time<T, Event>, Time<T, Event>> left,
      final Function<Time<T, Event>, Time<T, Event>> right
  ) {
    return time -> {
      final var fork = time.fork();
      return left.apply(fork).join(right.apply(fork));
    };
  }

  @Override
  public Function<Time<T, Event>, Time<T, Event>> atom(final Event event) {
    return time -> {
      // Re-emit the given event, or else it will disappear into the ether.
      time = time.emit(event);

      // Build a stack of unmerged branches.
      final var stack = new ArrayDeque<Time<T, Event>>(this.reactors.size());
      for (final var reactor : this.reactors.subList(0, this.reactors.size())) {
        time = time.fork();
        stack.push(reactor.apply(event).apply(time));
      }

      // Merge the build stack of branches down into a single joined time point.
      while (!stack.isEmpty()) time = stack.pop().join(time);

      return time;
    };
  }
}
