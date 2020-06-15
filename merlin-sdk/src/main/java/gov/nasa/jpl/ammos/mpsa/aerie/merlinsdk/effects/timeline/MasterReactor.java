package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Projection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A {@link Projection} from events to {@link Time.Operator}s.
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
    extends Time.OperatorTrait<T, Event>
    implements Projection<Event, Time.Operator<T, Event>>
{
  private final List<Function<Event, Time.Operator<T, Event>>> reactors = new ArrayList<>();

  public void addReactor(final Function<Event, Time.Operator<T, Event>> reactor) {
    this.reactors.add(reactor);
  }

  @Override
  public Time.Operator<T, Event> atom(final Event event) {
    return time -> {
      // Re-emit the given event, or else it will disappear into the ether.
      time = time.emit(event);

      if (this.reactors.size() == 0) return time;

      // Build a stack of unmerged branches.
      final var stack = new ArrayDeque<Time<T, Event>>(this.reactors.size() - 1);
      for (final var reactor : this.reactors.subList(0, this.reactors.size() - 1)) {
        time = time.fork();
        stack.push(reactor.apply(event).apply(time));
      }

      // Merge the build stack of branches down into a single joined time point.
      time = this.reactors.get(this.reactors.size() - 1).apply(event).apply(time);
      while (!stack.isEmpty()) time = stack.pop().join(time);
      return time;
    };
  }
}
