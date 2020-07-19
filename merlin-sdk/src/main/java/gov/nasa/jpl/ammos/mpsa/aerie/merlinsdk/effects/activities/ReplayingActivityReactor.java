package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Projection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;

import java.util.ArrayDeque;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.pcollections.ConsPStack;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;
import org.pcollections.PStack;

public final class ReplayingActivityReactor<T, Event, Activity>
    //@formatter:off  /* It is a little silly that IntelliJ can't format long generics. */
    implements Projection<
      ActivityContinuation<T, Event, Activity>,
      Task<T, Event, ActivityContinuation<T, Event, Activity>>>
    //@formatter:on
{
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

  private Pair<History<T, Event>, PMap<String, ScheduleItem<ActivityContinuation<T, Event, Activity>>>> runActivity(
      final Frame initialFrame)
  {
    final var frames = new ArrayDeque<Frame>();
    var scheduled = HashTreePMap.<String, ScheduleItem<ActivityContinuation<T, Event, Activity>>>empty();

    frames.add(initialFrame);
    while (true) {
      {
        final var frame = frames.peek();
        final var task = frame.branches.get(0);
        frame.branches = frame.branches.minus(0);

        final var taskId = task.getLeft();
        final var taskType = task.getRight().activity;
        final var taskBreadcrumbs = task.getRight().breadcrumbs;

        final var context = new ReplayingReactionContext<T, Activity, Event>(taskBreadcrumbs);

        // TODO: avoid using exceptions for control flow by wrapping the executor in a Thread
        ScheduleItem<ActivityContinuation<T, Event, Activity>> continuation;
        try {
          this.executor.execute(context, taskId, taskType);

          frames.push(new Frame(context.getCurrentHistory(), context.getSpawns()));
          continuation = new ScheduleItem.Complete<>(taskId);
        } catch (final ReplayingReactionContext.Defer request) {
          frames.push(new Frame(context.getCurrentHistory(), context.getSpawns()));
          continuation = new ScheduleItem.Defer<>(
              request.duration,
              new ActivityContinuation<>(taskId, taskType, context.getBreadcrumbs()));
        } catch (final ReplayingReactionContext.Await request) {
          frames.push(new Frame(context.getCurrentHistory(), context.getSpawns()));
          continuation = new ScheduleItem.OnCompletion<>(
              request.activityId,
              new ActivityContinuation<>(taskId, taskType, context.getBreadcrumbs()));
        }

        scheduled = scheduled.plusAll(context.getDeferred()).plus(taskId, continuation);
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
    return new ActivityContinuation<>(UUID.randomUUID().toString(), activity);
  }

  public ActivityContinuation<T, Event, Activity> createSimulationTask(final String id, final Activity activity) {
    return new ActivityContinuation<>(id, activity);
  }

  @Override
  public Task<T, Event, ActivityContinuation<T, Event, Activity>> atom(
      final ActivityContinuation<T, Event, Activity> activity)
  {
    return time -> {
      time = time.fork();
      final var task = Pair.of(activity.activityId, activity.plus(new ActivityBreadcrumb.Advance<>(time)));
      final var frame = new Frame(time, ConsPStack.singleton(task));
      return this.runActivity(frame);
    };
  }

  @Override
  public Task<T, Event, ActivityContinuation<T, Event, Activity>> empty() {
    return ctx -> Pair.of(ctx, HashTreePMap.empty());
  }

  @Override
  public Task<T, Event, ActivityContinuation<T, Event, Activity>> sequentially(
      final Task<T, Event, ActivityContinuation<T, Event, Activity>> prefix,
      final Task<T, Event, ActivityContinuation<T, Event, Activity>> suffix)
  {
    return time -> {
      final var result1 = prefix.apply(time);
      final var result2 = suffix.apply(result1.getLeft());
      return Pair.of(
          result2.getLeft(),
          result1.getRight().plusAll(result2.getRight()));
    };
  }

  @Override
  public Task<T, Event, ActivityContinuation<T, Event, Activity>> concurrently(
      final Task<T, Event, ActivityContinuation<T, Event, Activity>> left,
      final Task<T, Event, ActivityContinuation<T, Event, Activity>> right)
  {
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
