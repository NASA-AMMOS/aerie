package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Projection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events.Event;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.Querier;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.states.States;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Time;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.pcollections.ConsPStack;
import org.pcollections.HashTreePMap;
import org.pcollections.PStack;
import org.pcollections.PVector;

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

  private final class Frame {
    public Time<T, Event> tip;
    public PStack<Triple<String, String, PVector<ActivityBreadcrumb<T, Event>>>> branches;

    public Frame(final Time<T, Event> tip, final PStack<Triple<String, String, PVector<ActivityBreadcrumb<T, Event>>>> branches) {
      this.tip = tip;
      this.branches = branches;
    }
  }

  public Task<T, Event> resumeActivity(final String activityId, final String activityType, final PVector<ActivityBreadcrumb<T, Event>> breadcrumbs) {
    Objects.requireNonNull(activityId);
    Objects.requireNonNull(activityType);
    return time -> {
      var frames = new ArrayDeque<Frame>();
      var scheduled = HashTreePMap.<String, ScheduleItem<T, Event>>empty();

      {
        var eh = time.fork();
        frames.add(new Frame(eh, ConsPStack.singleton(Triple.of(activityId, activityType, breadcrumbs.plus(new ActivityBreadcrumb.Advance<>(eh))))));
      }

      while (true) {
        {
          final var frame = frames.peek();
          final var task = frame.branches.get(0);
          frame.branches = frame.branches.minus(0);

          final var taskId = task.getLeft();
          final var taskType = task.getMiddle();
          final var taskBreadcrumbs = task.getRight();

          final var context = new ReactionContextImpl<>(this.querier, taskBreadcrumbs);

          // TODO: avoid using exceptions for control flow by wrapping activities in a Thread
          ScheduleItem<T, Event> continuation;
          try {
            final var activity = activityMap.getOrDefault(taskType, new Activity() {});
            States.activeContext.setWithin(context, activity::modelEffects);

            frames.push(new Frame(context.getCurrentTime(), context.getSpawns()));
            continuation = new ScheduleItem.Complete<>();
          } catch (final ReactionContextImpl.Defer request) {
            frames.push(new Frame(context.getCurrentTime(), context.getSpawns()));
            continuation = new ScheduleItem.Defer<>(request.duration, taskType, context.getBreadcrumbs());
          } catch (final ReactionContextImpl.Await request) {
            frames.push(new Frame(context.getCurrentTime(), context.getSpawns()));
            continuation = new ScheduleItem.OnCompletion<>(request.activityId, taskType, context.getBreadcrumbs());
          }

          scheduled = scheduled.plus(taskId, continuation);
        }

        while (frames.peek().branches.isEmpty()) {
          final var frame = frames.pop();
          if (!frames.isEmpty()) {
            final var parent = frames.peek();
            parent.tip = parent.tip.join(frame.tip);
          } else {
            return Pair.of(frame.tip, scheduled);
          }
        }
      }
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
