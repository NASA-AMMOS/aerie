package gov.nasa.jpl.aerie.merlin.driver.timeline;

import java.util.Arrays;
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
    for (final var row : this.rows) {
      final var effect = row.select(event);
      if (effect.isPresent()) return effect;
    }
    return Optional.empty();
  }

  public record SelectorRow<EventType, Effect>(Topic<EventType> topic, Function<EventType, Effect> transform) {
    public Optional<Effect> select(final Event event$) {
      return event$.extract(this.topic, transform);
    }
  }
}
