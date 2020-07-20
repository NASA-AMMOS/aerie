package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.TaskFactory;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.TaskScheduler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.HashSet;
import java.util.Set;

public final class ReplayingReactionContext<T, Event, Activity> implements ReactionContext<T, Event, Activity> {
  private ReplayingTask<T, Event, Activity> continuation;
  private int nextBreadcrumbIndex;

  private final TaskFactory<T, Event, Activity> factory;
  private final TaskScheduler<T, Event> scheduler;
  private History<T, Event> currentHistory;
  private final Set<String> children = new HashSet<>();

  public ReplayingReactionContext(
      final TaskFactory<T, Event, Activity> factory,
      final TaskScheduler<T, Event> scheduler,
      final ReplayingTask<T, Event, Activity> task)
  {
    this.factory = factory;
    this.scheduler = scheduler;
    this.continuation = task;

    this.nextBreadcrumbIndex = 0;
    this.currentHistory = ((ActivityBreadcrumb.Advance<T, Event>) task.getBreadcrumb(this.nextBreadcrumbIndex++)).next;
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

      this.scheduler.spawn(this.currentHistory, continuation);
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

      this.scheduler.defer(delay, continuation);
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
