package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Projection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events.Event;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.Querier;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Time;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.pcollections.HashTreePMap;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

public final class ActivityReactor<T>
    implements Projection<SchedulingEvent<T>, Task<T, Event>>
{
  private static final Map<String, Activity> activityMap = Map.of(
      "a", new ActivityA(),
      "b", new ActivityB()
  );

  private final Querier<T> querier;

  public ActivityReactor(final Querier<T> querier) {
    this.querier = querier;
  }

  public Task<T, Event> resumeActivity(final String activityId, final String activityType, final PVector<Time<T, Event>> milestones) {
    Objects.requireNonNull(activityId);
    Objects.requireNonNull(activityType);
    return time -> {
      var tasks = new ArrayDeque<Triple<String, String, PVector<Time<T, Event>>>>();
      var branches = new ArrayDeque<Time<T, Event>>();

      time = time.fork();
      branches.push(time);
      tasks.add(Triple.of(activityId, activityType, milestones.plus(time)));

      var scheduled = HashTreePMap.<String, ScheduleItem<T, Event>>empty();
      while (!tasks.isEmpty()) {
        final var x = tasks.pop();
        final var activity = activityMap.getOrDefault(x.getMiddle(), new Activity() {});

        // TODO: avoid using exceptions for control flow by wrapping activities in a Thread
        final var context = new ReactionContext<>(this.querier, tasks, x.getRight());
        try {
          ReactionContext.activeContext.setWithin(context, activity::modelEffects);

          branches.push(context.getCurrentTime());
          scheduled = scheduled.plus(x.getLeft(), new ScheduleItem.Complete<>());
        } catch (final ReactionContext.Defer request) {
          branches.push(context.getCurrentTime());
          scheduled = scheduled.plus(x.getLeft(), new ScheduleItem.Defer<>(request.duration, x.getMiddle(), x.getRight()));
        } catch (final ReactionContext.Call request) {
          final var childId = UUID.randomUUID().toString();

          scheduled = scheduled.plus(x.getLeft(), new ScheduleItem.OnCompletion<>(childId, x.getMiddle(), x.getRight()));
          tasks.push(Triple.of(childId, request.activityType, TreePVector.singleton(context.getCurrentTime())));
        }
      }

      time = branches.pop();
      while (!branches.isEmpty()) time = branches.pop().join(time);

      return Pair.of(time, scheduled);
    };
  }

  @Override
  public Task<T, Event> atom(final SchedulingEvent<T> event) {
    if (event instanceof SchedulingEvent.ResumeActivity) {
      final var resume = (SchedulingEvent.ResumeActivity<T>) event;
      return this.resumeActivity(resume.activityId, resume.activityType, resume.milestones);
    } else {
      throw new Error("Unexpected subclass of SchedulingEvent: " + event.getClass().getName());
    }
  }

  @Override
  public Task<T, Event> empty() {
    return ctx -> Pair.of(ctx, HashTreePMap.empty());
  }

  @Override
  public Task<T, Event> sequentially(final Task<T, Event> prefix, final Task<T, Event> suffix) {
    return time -> {
      final var result1 = prefix.apply(time);
      final var result2 = suffix.apply(result1.getLeft());
      return Pair.of(
          result2.getLeft(),
          result1.getRight().plusAll(result2.getRight()));
    };
  }

  @Override
  public Task<T, Event> concurrently(final Task<T, Event> left, final Task<T, Event> right) {
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
