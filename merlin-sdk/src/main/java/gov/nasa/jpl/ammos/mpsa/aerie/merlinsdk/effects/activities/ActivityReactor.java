package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Projection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Time;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.pcollections.ConsPStack;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;
import org.pcollections.PStack;
import org.pcollections.PVector;

public final class ActivityReactor<T, Activity, Event> implements Projection<SchedulingEvent<T, Activity, Event>, Task<T, Activity, Event>> {
  private final BiConsumer<ReactionContext<T, Activity, Event>, Activity> executor;

  public ActivityReactor(final BiConsumer<ReactionContext<T, Activity, Event>, Activity> executor) {
    this.executor = executor;
  }

  private final class Frame {
    public Time<T, Event> tip;
    public PStack<Triple<String, Activity, PVector<ActivityBreadcrumb<T, Event>>>> branches;

    public Frame(final Time<T, Event> tip, final PStack<Triple<String, Activity, PVector<ActivityBreadcrumb<T, Event>>>> branches) {
      this.tip = tip;
      this.branches = branches;
    }
  }

  private Pair<Time<T, Event>, PMap<String, ScheduleItem<T, Activity, Event>>> runActivity(final Frame initialFrame) {
    var frames = new ArrayDeque<Frame>();
    var scheduled = HashTreePMap.<String, ScheduleItem<T, Activity, Event>>empty();

    frames.add(initialFrame);
    while (true) {
      {
        final var frame = frames.peek();
        final var task = frame.branches.get(0);
        frame.branches = frame.branches.minus(0);

        final var taskId = task.getLeft();
        final var taskType = task.getMiddle();
        final var taskBreadcrumbs = task.getRight();

        final var context = new ReactionContextImpl<T, Activity, Event>(taskBreadcrumbs);

        // TODO: avoid using exceptions for control flow by wrapping the executor in a Thread
        ScheduleItem<T, Activity, Event> continuation;
        try {
          this.executor.accept(context, taskType);

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
  }

  @Override
  public Task<T, Activity, Event> atom(final SchedulingEvent<T, Activity, Event> event) {
    if (event instanceof SchedulingEvent.ResumeActivity) {
      final var resume = (SchedulingEvent.ResumeActivity<T, Activity, Event>) event;
      Objects.requireNonNull(resume.activityId);
      Objects.requireNonNull(resume.activityType);

      return time -> {
        time = time.fork();
        final var task = Triple.of(resume.activityId, resume.activityType, resume.milestones.plus(new ActivityBreadcrumb.Advance<>(time)));
        final var frame = new Frame(time, ConsPStack.singleton(task));
        return this.runActivity(frame);
      };
    } else {
      throw new Error("Unexpected subclass of SchedulingEvent: " + event.getClass().getName());
    }
  }

  @Override
  public Task<T, Activity, Event> empty() {
    return ctx -> Pair.of(ctx, HashTreePMap.empty());
  }

  @Override
  public Task<T, Activity, Event> sequentially(final Task<T, Activity, Event> prefix, final Task<T, Activity, Event> suffix) {
    return time -> {
      final var result1 = prefix.apply(time);
      final var result2 = suffix.apply(result1.getLeft());
      return Pair.of(
          result2.getLeft(),
          result1.getRight().plusAll(result2.getRight()));
    };
  }

  @Override
  public Task<T, Activity, Event> concurrently(final Task<T, Activity, Event> left, final Task<T, Activity, Event> right) {
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
