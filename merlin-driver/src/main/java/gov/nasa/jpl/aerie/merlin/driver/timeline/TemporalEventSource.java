package gov.nasa.jpl.aerie.merlin.driver.timeline;

import gov.nasa.jpl.aerie.merlin.driver.engine.SlabList;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

public record TemporalEventSource(SlabList<TimePoint> points) implements EventSource {
  public TemporalEventSource() {
    this(new SlabList<>());
  }

  public void add(final Duration delta) {
    this.points.append(new TimePoint.Delta(delta));
  }

  public void add(final EventGraph<Event> graph) {
    this.points.append(new TimePoint.Commit(graph));
  }

  @Override
  public Cursor cursor() {
    final var iterator = this.points.iterator();

    return new Cursor() {
      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public void step(final Cell<?> cell) {
        final var point = iterator.next();

        if (point instanceof TimePoint.Delta p) {
          cell.step(p.delta());
        } else if (point instanceof TimePoint.Commit p) {
          cell.apply(p.events());
        } else {
          throw new IllegalStateException();
        }
      }
    };
  }

  public sealed interface TimePoint {
    record Delta(Duration delta) implements TimePoint {}
    record Commit(EventGraph<Event> events) implements TimePoint {}
  }
}
