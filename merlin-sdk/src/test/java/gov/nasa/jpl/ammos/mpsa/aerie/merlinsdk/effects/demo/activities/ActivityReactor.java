package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Projection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events.Event;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.Querier;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Time;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

public final class ActivityReactor<T>
    implements Projection<SchedulingEvent<T>, Function<Time<T, Event>, Pair<Time<T, Event>, PMap<String, ScheduleItem<T, Event>>>>>
{
  private static final Map<String, Activity> activityMap = Map.of(
      "a", new ActivityA(),
      "b", new ActivityB()
  );

  private final Querier<T> querier;
  private final Projection<Event, Function<Time<T, Event>, Pair<Time<T, Event>, PMap<String, ScheduleItem<T, Event>>>>> reactor;
  private final Map<String, String> activityInstances = new HashMap<>();

  public ActivityReactor(
      final Querier<T> querier,
      final Projection<Event, Function<Time<T, Event>, Pair<Time<T, Event>, PMap<String, ScheduleItem<T, Event>>>>> reactor
  ) {
    this.querier = querier;
    this.reactor = reactor;
  }

  public Function<Time<T, Event>, Pair<Time<T, Event>, PMap<String, ScheduleItem<T, Event>>>> instantiateActivity(final String activityId, final String activityType) {
    if (this.activityInstances.containsKey(activityId)) {
      throw new RuntimeException("Activity ID already in use");
    }

    this.activityInstances.put(activityId, activityType);

    return time -> Pair.of(time, HashTreePMap.empty());
  }

  public Function<Time<T, Event>, Pair<Time<T, Event>, PMap<String, ScheduleItem<T, Event>>>> resumeActivity(
      final String activityId,
      final PVector<Time<T, Event>> milestones
  ) {
    return time -> {
      final var activityType = this.activityInstances.get(activityId);
      final var activity = activityMap.getOrDefault(activityType, new Activity() {});

      var scheduled = HashTreePMap.<String, ScheduleItem<T, Event>>empty();

      // TODO: avoid using exceptions for control flow by wrapping activities in a Thread
      final var context = new ReactionContext<>(this.querier, this.reactor, milestones.plus(time));
      try {
        ReactionContext.activeContext.setWithin(context, activity::modelEffects);
        scheduled = scheduled.plusAll(context.getScheduled());
        time = context.getCurrentTime();

        scheduled = scheduled.plus(activityId, new ScheduleItem.Complete<>());
      } catch (final ReactionContext.Defer request) {
        scheduled = scheduled.plusAll(context.getScheduled());
        time = context.getCurrentTime();

        scheduled = scheduled.plus(activityId, new ScheduleItem.Defer<>(request.duration, milestones.plus(time)));
      } catch (final ReactionContext.Call request) {
        scheduled = scheduled.plusAll(context.getScheduled());
        time = context.getCurrentTime();

        final var childId = UUID.randomUUID().toString();
        scheduled = scheduled.plus(activityId, new ScheduleItem.OnCompletion<>(childId, milestones.plus(time)));

        final var callGraph = EventGraph.sequentially(
            EventGraph.atom(new SchedulingEvent.InstantiateActivity<T>(childId, request.activityType)),
            EventGraph.atom(new SchedulingEvent.ResumeActivity<T>(childId, TreePVector.empty())));
        final var result = callGraph.evaluate(this).apply(time);
        scheduled = scheduled.plusAll(result.getRight());
        time = result.getLeft();
      }

      return Pair.of(time, scheduled);
    };
  }

  @Override
  public Function<Time<T, Event>, Pair<Time<T, Event>, PMap<String, ScheduleItem<T, Event>>>> atom(final SchedulingEvent<T> event) {
    if (event instanceof SchedulingEvent.InstantiateActivity) {
      final var instantiate = (SchedulingEvent.InstantiateActivity<T>) event;
      return this.instantiateActivity(instantiate.activityId, instantiate.activityType);
    } else if (event instanceof SchedulingEvent.ResumeActivity) {
      final var resume = (SchedulingEvent.ResumeActivity<T>) event;
      return this.resumeActivity(resume.activityId, resume.milestones);
    } else {
      throw new Error("Unexpected subclass of SchedulingEvent: " + event.getClass().getName());
    }
  }

  @Override
  public Function<Time<T, Event>, Pair<Time<T, Event>, PMap<String, ScheduleItem<T, Event>>>> empty() {
    return ctx -> Pair.of(ctx, HashTreePMap.empty());
  }

  @Override
  public Function<Time<T, Event>, Pair<Time<T, Event>, PMap<String, ScheduleItem<T, Event>>>> sequentially(
      final Function<Time<T, Event>, Pair<Time<T, Event>, PMap<String, ScheduleItem<T, Event>>>> prefix,
      final Function<Time<T, Event>, Pair<Time<T, Event>, PMap<String, ScheduleItem<T, Event>>>> suffix
  ) {
    return time -> {
      final var result1 = prefix.apply(time);
      final var result2 = suffix.apply(result1.getLeft());
      return Pair.of(
          result2.getLeft(),
          result1.getRight().plusAll(result2.getRight()));
    };
  }

  @Override
  public Function<Time<T, Event>, Pair<Time<T, Event>, PMap<String, ScheduleItem<T, Event>>>> concurrently(
      final Function<Time<T, Event>, Pair<Time<T, Event>, PMap<String, ScheduleItem<T, Event>>>> left,
      final Function<Time<T, Event>, Pair<Time<T, Event>, PMap<String, ScheduleItem<T, Event>>>> right
  ) {
    return time -> {
      final var fork = time.fork();
      final var result1 = left.apply(fork);
      final var result2 = right.apply(fork);
      return Pair.of(
          result1.getLeft().join(result2.getLeft()),
          result1.getRight().plusAll(result2.getRight()));
    };
  }
}
