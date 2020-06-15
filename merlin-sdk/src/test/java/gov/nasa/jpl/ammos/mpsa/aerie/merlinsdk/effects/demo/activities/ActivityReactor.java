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
    // TODO: The graph (instantiate("xyz", "a") | resume("xyz")) will behave non-deterministically
    //   depending on which branch is taken first. `resume` should only be able to see names defined
    //   by previous `instantiate` events.
    //   Investigate augmenting the event graph structure with existentialized identifiers, so that we can model
    //   this situation as e.g. (∃x. instantiate(x, "a"); resume(x)). Scope is automatically enforced by the quantifier.
    final var activityType = this.activityInstances.get(activityId);

    return time -> {
      final var activity = activityMap.getOrDefault(activityType, new Activity() {});

      // TODO: avoid using exceptions for control flow by wrapping activities in a Thread
      final var context = new ReactionContext<>(this.querier, this.reactor, List.of(time));
      try {
        ReactionContext.activeContext.setWithin(context, activity::modelEffects);
        time = context.getCurrentTime();
      } catch (final ReactionContext.Defer request) {
        time = context.getCurrentTime();
        // TODO: schedule a resumption at a future time
      } catch (final ReactionContext.Call request) {
        time = context.getCurrentTime();

        final var id = UUID.randomUUID().toString();
        time = this.reactor.atom(Event.instantiateActivity(id, request.activityType)).apply(time);
        time = this.reactor.atom(Event.resumeActivity(id)).apply(time);
        // TODO: schedule a resumption at a future time
      }

      return time;
    };
  }

  @Override
  public Function<Time<T, Event>, Time<T, Event>> unhandled() {
    return ctx -> ctx;
  }
}
