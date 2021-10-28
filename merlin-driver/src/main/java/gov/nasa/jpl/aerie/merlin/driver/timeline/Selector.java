package gov.nasa.jpl.aerie.merlin.driver.timeline;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public record Selector<Effect>(List<SelectorRow<?, Effect>> rows) {
  public <EventType> Selector(final Topic<EventType> topic, final Function<EventType, Effect> transform) {
    this(List.of(new SelectorRow<>(topic, transform)));
  }

  @SafeVarargs
  public Selector(final SelectorRow<?, Effect>... selectors) {
    this(Arrays.asList(selectors));
  }

  public Effect select(final Event event$, final Supplier<Effect> orElse) {
    for (final var row : this.rows) {
      final var event = row.select(event$);
      if (event != null) return event;
    }
    return orElse.get();
  }

  public boolean matchesAny(final Collection<Topic<?>> topics) {
    for (final var row : this.rows) {
      if (topics.contains(row.topic)) return true;
    }
    return false;
  }

  public record SelectorRow<EventType, Effect>(Topic<EventType> topic, Function<EventType, Effect> transform) {
    public Effect select(final Event event$) {
      final var event = event$.extract(this.topic);
      if (event == null) return null;

      return this.transform.apply(event);
    }
  }
}
