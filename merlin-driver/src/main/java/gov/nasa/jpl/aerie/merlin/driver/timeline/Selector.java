package gov.nasa.jpl.aerie.merlin.driver.timeline;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public record Selector<Effect>(List<SelectorRow<?, Effect>> rows) {
  public <EventType> Selector(final Topic<EventType> topic, final Function<EventType, Effect> transform) {
    this(List.of(new SelectorRow<>(topic, transform)));
  }

  @SafeVarargs
  public Selector(final SelectorRow<?, Effect>... selectors) {
    this(Arrays.asList(selectors));
  }

  public Optional<Effect> select(final Event event) {
    // Bail out as fast as possible if we're in a trivial (and incredibly common) case.
    if (this.rows.size() == 1) return this.rows.get(0).select(event);

    for (final var row : this.rows) {
      final var effect = row.select(event);
      if (effect.isPresent()) return effect;
    }
    return Optional.empty();
  }

  public boolean matchesAny(final Collection<Topic<?>> topics) {
    // Bail out as fast as possible if we're in a trivial (and incredibly common) case.
    if (this.rows.size() == 1) return topics.contains(this.rows.get(0).topic());

    for (final var row : this.rows) {
      if (topics.contains(row.topic)) return true;
    }
    return false;
  }

  public record SelectorRow<EventType, Effect>(Topic<EventType> topic, Function<EventType, Effect> transform) {
    public Optional<Effect> select(final Event event$) {
      return event$.extract(this.topic, this.transform);
    }
  }
}
