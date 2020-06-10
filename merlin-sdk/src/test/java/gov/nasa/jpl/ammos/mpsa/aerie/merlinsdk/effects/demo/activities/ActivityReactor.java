package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Projection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.ReactionContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events.DefaultEventHandler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events.Event;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.Querier;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Time;

import java.util.Map;

public final class ActivityReactor<T> implements DefaultEventHandler<Time.Operator<T, Event>> {
  private static final Map<String, Activity> activityMap = Map.of(
      "a", new ActivityA(),
      "b", new ActivityB()
  );

  private final Querier<T> querier;
  private final Projection<Event, Time.Operator<T, Event>> reactor;

  public ActivityReactor(final Querier<T> querier, final Projection<Event, Time.Operator<T, Event>> reactor) {
    this.querier = querier;
    this.reactor = reactor;
  }

  @Override
  public Time.Operator<T, Event> run(final String activityType) {
    final var activity = activityMap.getOrDefault(activityType, new Activity() {});
    return time -> {
      final var context = new ReactionContext<>(this.querier, this.reactor, time);
      ReactionContext.activeContext.setWithin(context, activity::modelEffects);
      return context.getCurrentTime();
    };
  }

  @Override
  public Time.Operator<T, Event> unhandled() {
    return ctx -> ctx;
  }
}
