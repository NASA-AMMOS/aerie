package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Projection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events.DefaultEventHandler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events.Event;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.Querier;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Time;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.pcollections.HashTreePMap;

public final class ActivityReactor<T>
    implements DefaultEventHandler<Function<Time<T, Event>, Time<T, Event>>>
{
  private static final Map<String, Activity> activityMap = Map.of(
      "a", new ActivityA(),
      "b", new ActivityB()
  );

  private final Querier<T> querier;
  private final Projection<Event, Function<Time<T, Event>, Time<T, Event>>> reactor;
  private final Map<String, String> activityInstances = new HashMap<>();

  public ActivityReactor(
      final Querier<T> querier,
      final Projection<Event, Function<Time<T, Event>, Time<T, Event>>> reactor
  ) {
    this.querier = querier;
    this.reactor = reactor;
  }

  @Override
  public Function<Time<T, Event>, Time<T, Event>> instantiateActivity(final String activityId, final String activityType) {
    if (this.activityInstances.containsKey(activityId)) {
      throw new RuntimeException("Activity ID already in use");
    }

    this.activityInstances.put(activityId, activityType);

    return time -> time;
  }

  @Override
  public Function<Time<T, Event>, Time<T, Event>> resumeActivity(final String activityId) {
    return time -> {
      final var activityType = this.activityInstances.get(activityId);
      final var activity = activityMap.getOrDefault(activityType, new Activity() {});

      var scheduled = HashTreePMap.<String, ScheduleItem>empty();

      // TODO: avoid using exceptions for control flow by wrapping activities in a Thread
      final var context = new ReactionContext<>(this.querier, this.reactor, List.of(time));
      try {
        ReactionContext.activeContext.setWithin(context, activity::modelEffects);
        time = context.getCurrentTime();

        scheduled = scheduled.plus(activityId, new ScheduleItem.Complete());
      } catch (final ReactionContext.Defer request) {
        scheduled = scheduled.plus(activityId, new ScheduleItem.Defer(request.duration));
        time = context.getCurrentTime();
      } catch (final ReactionContext.Call request) {
        final var childId = UUID.randomUUID().toString();
        scheduled = scheduled.plus(activityId, new ScheduleItem.OnCompletion(childId));

        time = context.getCurrentTime();
        time = this.reactor.atom(Event.instantiateActivity(childId, request.activityType)).apply(time);
        time = this.reactor.atom(Event.resumeActivity(childId)).apply(time);
      }

      return time;
    };
  }

  @Override
  public Function<Time<T, Event>, Time<T, Event>> unhandled() {
    return ctx -> ctx;
  }
}
