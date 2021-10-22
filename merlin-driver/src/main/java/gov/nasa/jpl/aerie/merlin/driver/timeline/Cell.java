package gov.nasa.jpl.aerie.merlin.driver.timeline;

import gov.nasa.jpl.aerie.merlin.protocol.model.Applicator;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.timeline.effects.EventGraph;

import java.util.Optional;

/** Binds the state of a cell together with its dynamical behavior. */
public final class Cell<State> {
  private final GenericCell<?, State> inner;
  private final State state;

  private <Effect> Cell(final GenericCell<Effect, State> inner, final State state) {
    this.inner = inner;
    this.state = state;
  }

  public <Effect> Cell(
      final Applicator<Effect, State> applicator,
      final EffectTrait<Effect> algebra,
      final Selector<Effect> selector,
      final EventGraphEvaluator evaluator,
      final State state
  ) {
    this(new GenericCell<>(applicator, algebra, selector, evaluator), state);
  }

  public Cell<State> duplicate() {
    return new Cell<>(this.inner, this.inner.applicator.duplicate(this.state));
  }

  public void step(final Duration delta) {
    this.inner.applicator.step(this.state, delta);
  }

  public void apply(final EventGraph<Event> events) {
    this.inner.apply(this.state, events);
  }

  public Optional<Duration> getExpiry() {
    return this.inner.applicator.getExpiry(this.state);
  }

  public State getState() {
    return this.inner.applicator.duplicate(this.state);
  }

  @Override
  public String toString() {
    return this.state.toString();
  }

  private record GenericCell<Effect, State> (
      Applicator<Effect, State> applicator,
      EffectTrait<Effect> algebra,
      Selector<Effect> selector,
      EventGraphEvaluator evaluator
  ) {
    public void apply(final State state, final EventGraph<Event> events) {
      this.applicator.apply(state, this.evaluator
          .evaluateOptional(this.algebra, this.selector::select, events)
          .orElseGet(this.algebra::empty));
    }
  }
}
