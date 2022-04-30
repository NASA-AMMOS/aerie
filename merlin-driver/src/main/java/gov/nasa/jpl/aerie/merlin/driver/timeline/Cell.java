package gov.nasa.jpl.aerie.merlin.driver.timeline;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.Applicator;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Optional;
import java.util.Set;

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

  public void apply(final Event event) {
    this.inner.apply(this.state, event);
  }

  public void apply(final Event[] events, final int from, final int to) {
    this.inner.apply(this.state, events, from, to);
  }

  public Optional<Duration> getExpiry() {
    return this.inner.applicator.getExpiry(this.state);
  }

  public State getState() {
    return this.inner.applicator.duplicate(this.state);
  }

  public boolean isInterestedIn(final Set<Topic<?>> topics) {
    return this.inner.selector.matchesAny(topics);
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
      final var effect$ = this.evaluator.evaluate(this.algebra, this.selector, events);
      if (effect$.isPresent()) this.applicator.apply(state, effect$.get());
    }

    public void apply(final State state, final Event event) {
      final var effect$ = this.selector.select(this.algebra, event);
      if (effect$.isPresent()) this.applicator.apply(state, effect$.get());
    }

    public void apply(final State state, final Event[] events, int from, final int to) {
      while (from < to) apply(state, events[from++]);
    }
  }
}
