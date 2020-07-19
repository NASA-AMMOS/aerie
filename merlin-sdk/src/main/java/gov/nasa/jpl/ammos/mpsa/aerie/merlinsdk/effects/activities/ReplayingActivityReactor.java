package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;

import java.util.ArrayDeque;
import java.util.UUID;
import java.util.function.Consumer;

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
    public PStack<Pair<History<T, Event>, ActivityContinuation<T, Event, Activity>>> branches;

    public Frame(
        final History<T, Event> tip,
        final PStack<Pair<History<T, Event>, ActivityContinuation<T, Event, Activity>>> branches)
    {
      this.tip = tip;
      this.branches = branches;
    }
  }

  private History<T, Event> runActivity(final Frame initialFrame, final Consumer<ScheduleItem<T, Event>> scheduler) {
    final var frames = new ArrayDeque<Frame>();
    frames.add(initialFrame);

    while (true) {
      {
        final var frame = frames.peek();
        final var branch = frame.branches.get(0);
        frame.branches = frame.branches.minus(0);

        final var branchTip = branch.getKey();
        final var task = branch.getValue().advancedTo(branchTip);
        final var taskId = task.getId();
        final var taskType = task.activity;
        final var taskBreadcrumbs = task.breadcrumbs;

        final var context = new ReplayingReactionContext<>(this, scheduler, taskBreadcrumbs);

        // TODO: avoid using exceptions for control flow by wrapping the executor in a Thread
        try {
          this.executor.execute(context, taskId, taskType);

          scheduler.accept(new ScheduleItem.Complete<>(taskId));
        } catch (final ReplayingReactionContext.Defer request) {
          scheduler.accept(new ScheduleItem.Defer<>(
              request.duration,
              new ActivityContinuation<>(this, taskId, taskType, context.getBreadcrumbs())));
        } catch (final ReplayingReactionContext.Await request) {
          scheduler.accept(new ScheduleItem.OnCompletion<>(
              request.activityId,
              new ActivityContinuation<>(this, taskId, taskType, context.getBreadcrumbs())));
        }

        frames.push(new Frame(context.getCurrentHistory(), context.getSpawns()));
      }

      while (frames.peek().branches.isEmpty()) {
        final var frame = frames.pop();
        if (!frames.isEmpty()) {
          final var parent = frames.peek();
          parent.tip = parent.tip.join(frame.tip);
        } else {
          return frame.tip;
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

  public History<T, Event> react(
      final History<T, Event> history,
      final Consumer<ScheduleItem<T, Event>> scheduler,
      final ActivityContinuation<T, Event, Activity> activity)
  {
    final var time = history.fork();
    final var frame = new Frame(time, ConsPStack.singleton(Pair.of(time, activity)));
    return this.runActivity(frame, scheduler);
  }
}
