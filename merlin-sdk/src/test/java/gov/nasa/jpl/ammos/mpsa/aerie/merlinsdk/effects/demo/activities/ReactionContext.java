package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EffectExpression;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Projection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.Querier;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events.Event;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Time;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.DynamicCell;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class ReactionContext<T> {
  private final Querier<T> querier;
  private final Projection<Event, Function<Time<T, Event>, Time<T, Event>>> reactor;
  private Time<T, Event> currentTime;
  private List<Time<T, Event>> nextTimes;

  public static final DynamicCell<ReactionContext<?>> activeContext = DynamicCell.create();

  public ReactionContext(
      final Querier<T> querier,
      final Projection<Event, Function<Time<T, Event>, Time<T, Event>>> reactor,
      final List<Time<T, Event>> times
  ) {
    this.querier = querier;
    this.reactor = reactor;
    this.currentTime = times.get(0);
    this.nextTimes = times.subList(1, times.size());
  }

  public final <Result> Result as(final BiFunction<Querier<T>, Time<T, Event>, Result> interpreter) {
    return interpreter.apply(this.querier, this.currentTime);
  }

  public final Time<T, Event> getCurrentTime() {
    return this.currentTime;
  }

  public final ReactionContext<T> react(final Event event) {
    this.currentTime = this.reactor.atom(event).apply(this.currentTime);
    return this;
  }

  public final ReactionContext<T> react(final EffectExpression<Event> graph) {
    this.currentTime = graph.evaluate(this.reactor).apply(this.currentTime);
    return this;
  }

  public final ReactionContext<T> delay(final Duration duration) {
    if (nextTimes.size() == 0) {
      throw new Defer(duration);
    } else {
      this.currentTime = this.nextTimes.get(0);
      this.nextTimes = this.nextTimes.subList(1, this.nextTimes.size());
      return this;
    }
  }

  public final ReactionContext<T> call(final String activity) {
    if (nextTimes.size() == 0) {
      throw new Call(activity);
    } else {
      this.currentTime = this.nextTimes.get(0);
      this.nextTimes = this.nextTimes.subList(1, this.nextTimes.size());
      return this;
    }
  }

  public static final class Defer extends RuntimeException {
    public final Duration duration;

    private Defer(final Duration duration) {
      this.duration = duration;
    }
  }

  public static final class Call extends RuntimeException {
    public final String activityType;

    private Call(final String activityType) {
      this.activityType = activityType;
    }
  }
}
