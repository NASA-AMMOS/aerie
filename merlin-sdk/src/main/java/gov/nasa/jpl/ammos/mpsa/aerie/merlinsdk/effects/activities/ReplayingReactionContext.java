package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import org.apache.commons.lang3.tuple.Pair;
import org.pcollections.ConsPStack;
import org.pcollections.PStack;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public final class ReplayingReactionContext<T, Event, Activity> implements ReactionContext<T, Event, Activity> {
  private PStack<Pair<History<T, Event>, SimulationTask<T, Event>>> spawns = ConsPStack.empty();
  private ReplayingTask<T, Event, Activity> continuation;
  private int nextBreadcrumbIndex;

  private final TaskFactory<T, Event, Activity> factory;
  private final Consumer<ScheduleItem<T, Event>> scheduler;
  private History<T, Event> currentHistory;
  private final Set<String> children = new HashSet<>();

  public ReplayingReactionContext(
      final TaskFactory<T, Event, Activity> factory,
      final Consumer<ScheduleItem<T, Event>> scheduler,
      final ReplayingTask<T, Event, Activity> task)
  {
    this.factory = factory;
    this.scheduler = scheduler;
    this.continuation = task;

    this.nextBreadcrumbIndex = 0;
    this.currentHistory = ((ActivityBreadcrumb.Advance<T, Event>) task.getBreadcrumb(this.nextBreadcrumbIndex++)).next;
  }

  public TaskFrame<T, Event> getResultFrame() {
    return new TaskFrame<>(this.currentHistory, this.spawns);
  }

  public ReplayingTask<T, Event, Activity> getContinuation() {
    return this.continuation;
  }

  @Override
  public History<T, Event> now() {
    return this.currentHistory;
  }

  @Override
  public void emit(final Event event) {
    this.currentHistory = this.currentHistory.emit(event);
  }

  public void delay(final Duration duration) {
    if (this.nextBreadcrumbIndex >= this.continuation.getBreadcrumbCount()) {
      throw new Defer(duration);
    } else {
      final var breadcrumb = this.continuation.getBreadcrumb(this.nextBreadcrumbIndex++);
      if (!(breadcrumb instanceof ActivityBreadcrumb.Advance)) {
        throw new RuntimeException("Unexpected breadcrumb on delay(): " + breadcrumb.getClass().getName());
      }

      this.currentHistory = ((ActivityBreadcrumb.Advance<T, Event>) breadcrumb).next;
    }
  }

  @Override
  public void waitForActivity(final String activityId) {
    if (this.nextBreadcrumbIndex >= this.continuation.getBreadcrumbCount()) {
      throw new Await(activityId);
    } else {
      final var breadcrumb = this.continuation.getBreadcrumb(this.nextBreadcrumbIndex++);
      if (!(breadcrumb instanceof ActivityBreadcrumb.Advance)) {
        throw new RuntimeException("Unexpected breadcrumb on waitForActivity(): " + breadcrumb.getClass().getName());
      }

      this.currentHistory = ((ActivityBreadcrumb.Advance<T, Event>) breadcrumb).next;
    }
  }

  public void waitForChildren() {
    for (final var child : this.children) this.waitForActivity(child);
  }

  @Override
  public String spawn(final Activity child) {
    final String childId;
    if (this.nextBreadcrumbIndex >= this.continuation.getBreadcrumbCount()) {
      final var continuation = this.factory.createReplayingTask(child);
      childId = continuation.getId();

      this.currentHistory = this.currentHistory.fork();

      this.spawns = this.spawns.plus(Pair.of(this.currentHistory, continuation));
      this.continuation = this.continuation.spawned(continuation.getId());
      this.nextBreadcrumbIndex += 1;
    } else {
      final var breadcrumb = this.continuation.getBreadcrumb(this.nextBreadcrumbIndex++);
      if (!(breadcrumb instanceof ActivityBreadcrumb.Spawn)) {
        throw new RuntimeException("Unexpected breadcrumb; expected spawn, got " + breadcrumb.getClass().getName());
      }

      childId = ((ActivityBreadcrumb.Spawn<T, Event>) breadcrumb).activityId;
    }

    this.children.add(childId);
    return childId;
  }

  @Override
  public String spawnAfter(final Duration delay, final Activity child) {
    final String childId;
    if (this.nextBreadcrumbIndex >= this.continuation.getBreadcrumbCount()) {
      final var continuation = this.factory.createReplayingTask(child);
      childId = continuation.getId();

      this.scheduler.accept(new ScheduleItem.Defer<>(delay, continuation));
      this.continuation = this.continuation.spawned(continuation.getId());
      this.nextBreadcrumbIndex += 1;
    } else {
      final var breadcrumb = this.continuation.getBreadcrumb(this.nextBreadcrumbIndex++);
      if (!(breadcrumb instanceof ActivityBreadcrumb.Spawn)) {
        throw new RuntimeException("Unexpected breadcrumb; expected spawn, got " + breadcrumb.getClass().getName());
      }

      childId = ((ActivityBreadcrumb.Spawn<T, Event>) breadcrumb).activityId;
    }

    this.children.add(childId);
    return childId;
  }

  public static final class Defer extends RuntimeException {
    public final Duration duration;

    private Defer(final Duration duration) {
      this.duration = duration;
    }
  }

  public static final class Await extends RuntimeException {
    public final String activityId;

    private Await(final String activityId) {
      this.activityId = activityId;
    }
  }
}
