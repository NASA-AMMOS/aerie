package gov.nasa.jpl.aerie.merlin.driver.timeline;

import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;

import java.util.Optional;
import java.util.function.Function;

public interface EventGraphEvaluator {
  <EventType, Effect> Optional<Effect> evaluateOptional(
      EffectTrait<Effect> trait,
      Function<EventType, Optional<Effect>> substitution,
      EventGraph<EventType> graph);

  default <EventType, Effect> Effect evaluate(
      final EffectTrait<Effect> trait,
      final Function<EventType, Effect> substitution,
      final EventGraph<EventType> graph
  ) {
    return this
        .evaluateOptional(trait, substitution.andThen(Optional::of), graph)
        .orElseGet(trait::empty);
  }
}
