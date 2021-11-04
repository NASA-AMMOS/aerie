package gov.nasa.jpl.aerie.merlin.driver.timeline;

import java.util.ArrayList;
import java.util.List;

public record CausalEventSource(List<EventGraph<Event>> points) implements EventSource {
  public CausalEventSource() {
    this(new ArrayList<>());
  }

  public void add(final EventGraph<Event> point) {
    this.points.add(point);
  }

  public EventGraph<Event> commit() {
    return EventGraph.sequentially(this.points);
  }

  @Override
  public Cursor cursor() {
    return new Cursor() {
      private int index = 0;

      @Override
      public boolean hasNext() {
        return (this.index < points.size());
      }

      @Override
      public void step(final Cell<?> cell) {
        if (!hasNext()) return;

        cell.apply(points.get(this.index++));
      }
    };
  }
}
