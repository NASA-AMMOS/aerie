package gov.nasa.jpl.aerie.merlin.driver.timeline;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Arrays;
import java.util.Optional;

public final class CausalEventSource implements EventSource {
  private Event[] points = new Event[2];
  private int size = 0;

  public void add(final Event point) {
    if (this.size == this.points.length) {
      this.points = Arrays.copyOf(this.points, 3 * this.size / 2);
    }

    this.points[this.size++] = point;
  }

  public boolean isEmpty() {
    return (this.size == 0);
  }

  // By committing events backward from an endpoint, we can massage the resulting EventGraph
  // into a very linear form that is easy to evaluate: (ev1 ; (ev2 ; (ev3 ; andThen)))
  public EventGraph<Event> commit(EventGraph<Event> andThen) {
    for (var i = this.size; i > 0; i -= 1) {
      andThen = EventGraph.sequentially(EventGraph.atom(this.points[i-1]), andThen);
    }
    return andThen;
  }

  @Override
  public CausalCursor cursor() {
    return new CausalCursor();
  }

  public final class CausalCursor implements Cursor {
    private int index = 0;

    @Override
    public void stepUp(final Cell<?> cell) {
      cell.apply(points, this.index, size);
      this.index = size;
    }

    @Override
    public void stepUp(final Cell<?> cell, final Duration maxTime, final boolean includeMaxTime) {
      throw new UnsupportedOperationException("Can't step through time with CausalCursor");
    }

    @Override
    public void stepUp(final Cell<?> cell, final EventGraph<Event> events, final Optional<Event> lastEvent,
                       final boolean includeLast) {
      // Find the position of lastEvent after the index,
      // which is the position before which the events have already been applied.
      int pos = index;
      while (pos < size) {
        if (points[pos].equals(lastEvent)) break;
        ++pos;
      }
      // Use the position as the end range to apply events to the cell, adjusting to include the event if specified
      if (includeLast) {
        pos = Math.min(pos + 1, size);
      }
      cell.apply(points, this.index, pos);
      this.index = pos;
    }
  }
}
