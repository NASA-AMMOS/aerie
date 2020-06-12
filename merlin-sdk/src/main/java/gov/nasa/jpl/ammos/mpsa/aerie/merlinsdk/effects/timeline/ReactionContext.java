package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EffectExpression;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Projection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class ReactionContext<T, Event, Model> {
  private final Model model;
  private final Projection<Event, Function<Time<T, Event>, Time<T, Event>>> reactor;
  private Time<T, Event> currentTime;

  public ReactionContext(
      final Model model,
      final Projection<Event, Function<Time<T, Event>, Time<T, Event>>> reactor,
      final Time<T, Event> currentTime
  ) {
    this.model = model;
    this.reactor = reactor;
    this.currentTime = currentTime;
  }

  public final <Result> Result as(final BiFunction<Model, Time<T, Event>, Result> interpreter) {
    return interpreter.apply(this.getModel(), this.currentTime);
  }

  public final Model getModel() {
    return this.model;
  }

  public final Time<T, Event> getCurrentTime() {
    return this.currentTime;
  }

  public final void react(final Event event) {
    this.currentTime = this.reactor.atom(event).apply(this.currentTime);
  }

  public final void react(final EffectExpression<Event> graph) {
    this.currentTime = graph.evaluate(this.reactor).apply(this.currentTime);
  }

  public final void wait(final Duration duration) {
    this.currentTime = this.currentTime.wait(duration);
  }
}
