package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.pcollections.ConsPStack;
import org.pcollections.PStack;

public final class ReplayingActivityReactor<T, Event, Activity> {
  private final ActivityExecutor<T, Activity, Event> executor;

  public ReplayingActivityReactor(final ActivityExecutor<T, Activity, Event> executor) {
    this.executor = executor;
  }

  private final class Frame {
    public History<T, Event> tip;
    public PStack<Pair<String, ActivityContinuation<T, Event, Activity>>> branches;

    public Frame(
        final History<T, Event> tip,
        final PStack<Pair<String, ActivityContinuation<T, Event, Activity>>> branches)
    {
      this.tip = tip;
      this.branches = branches;
    }
  }

  private Pair<History<T, Event>, Map<String, ScheduleItem<T, Event>>> runActivity(
      final Frame initialFrame)
  {
    final var frames = new ArrayDeque<Frame>();
    final var scheduled = new HashMap<String, ScheduleItem<T, Event>>();

    frames.add(initialFrame);
    while (true) {
      {
        final var frame = frames.peek();
        final var task = frame.branches.get(0);
        frame.branches = frame.branches.minus(0);

        final var taskId = task.getLeft();
        final var taskType = task.getRight().activity;
        final var taskBreadcrumbs = task.getRight().breadcrumbs;

        final var context = new ReplayingReactionContext<>(this, taskBreadcrumbs);

        // TODO: avoid using exceptions for control flow by wrapping the executor in a Thread
        ScheduleItem<T, Event> continuation;
        try {
          this.executor.execute(context, taskId, taskType);

          frames.push(new Frame(context.getCurrentHistory(), context.getSpawns()));
          continuation = new ScheduleItem.Complete<>(taskId);
        } catch (final ReplayingReactionContext.Defer request) {
          frames.push(new Frame(context.getCurrentHistory(), context.getSpawns()));
          continuation = new ScheduleItem.Defer<>(
              request.duration,
              new ActivityContinuation<>(this, taskId, taskType, context.getBreadcrumbs()));
        } catch (final ReplayingReactionContext.Await request) {
          frames.push(new Frame(context.getCurrentHistory(), context.getSpawns()));
          continuation = new ScheduleItem.OnCompletion<>(
              request.activityId,
              new ActivityContinuation<>(this, taskId, taskType, context.getBreadcrumbs()));
        }

        scheduled.putAll(context.getDeferred());
        scheduled.put(taskId, continuation);
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

  public ActivityContinuation<T, Event, Activity> createSimulationTask(final Activity activity) {
    return new ActivityContinuation<>(this, UUID.randomUUID().toString(), activity);
  }

  public ActivityContinuation<T, Event, Activity> createSimulationTask(final String id, final Activity activity) {
    return new ActivityContinuation<>(this, id, activity);
  }

  public Pair<History<T, Event>, Map<String, ScheduleItem<T, Event>>> react(
      final History<T, Event> history,
      final ActivityContinuation<T, Event, Activity> activity)
  {
    final var time = history.fork();
    final var task = Pair.of(activity.getId(), activity.plus(new ActivityBreadcrumb.Advance<>(time)));
    final var frame = new Frame(time, ConsPStack.singleton(task));
    return this.runActivity(frame);
  }
}
