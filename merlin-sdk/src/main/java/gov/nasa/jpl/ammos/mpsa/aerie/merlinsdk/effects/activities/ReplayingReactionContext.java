package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import org.apache.commons.lang3.tuple.Pair;
import org.pcollections.ConsPStack;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;
import org.pcollections.PStack;
import org.pcollections.PVector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ReplayingReactionContext<T, Activity, Event> implements ReactionContext<T, Activity, Event> {
  private PStack<Pair<String, ActivityContinuation<T, Event, Activity>>> spawns = ConsPStack.empty();
  private PMap<String, ScheduleItem<ActivityContinuation<T, Event, Activity>>> deferred = HashTreePMap.empty();
  private PVector<ActivityBreadcrumb<T, Event>> breadcrumbs;
  private int nextBreadcrumbIndex;

  private History<T, Event> currentHistory;
  private final Set<String> children = new HashSet<>();

  public ReplayingReactionContext(final PVector<ActivityBreadcrumb<T, Event>> breadcrumbs) {
    this.currentHistory = ((ActivityBreadcrumb.Advance<T, Event>) breadcrumbs.get(0)).next;
    this.breadcrumbs = breadcrumbs;
    this.nextBreadcrumbIndex = 1;
  }

  public final History<T, Event> getCurrentHistory() {
    return this.currentHistory;
  }

  public final PVector<ActivityBreadcrumb<T, Event>> getBreadcrumbs() {
    return this.breadcrumbs;
  }

  public final PStack<Pair<String, ActivityContinuation<T, Event, Activity>>> getSpawns() {
    return this.spawns;
  }

  public final PMap<String, ScheduleItem<ActivityContinuation<T, Event, Activity>>> getDeferred() {
    return this.deferred;
  }

  @Override
  public History<T, Event> now() {
    return this.currentHistory;
  }

  @Override
  public final void emit(final Event event) {
    this.currentHistory = this.currentHistory.emit(event);
  }

  public final void delay(final Duration duration) {
    if (this.nextBreadcrumbIndex >= breadcrumbs.size()) {
      throw new Defer(duration);
    } else {
      final var breadcrumb = this.breadcrumbs.get(this.nextBreadcrumbIndex++);
      if (!(breadcrumb instanceof ActivityBreadcrumb.Advance)) {
        throw new RuntimeException("Unexpected breadcrumb on delay(): " + breadcrumb.getClass().getName());
      }

      this.currentHistory = ((ActivityBreadcrumb.Advance<T, Event>) breadcrumb).next;
    }
  }

  @Override
  public final void waitForActivity(final String activityId) {
    if (this.nextBreadcrumbIndex >= breadcrumbs.size()) {
      throw new Await(activityId);
    } else {
      final var breadcrumb = this.breadcrumbs.get(this.nextBreadcrumbIndex++);
      if (!(breadcrumb instanceof ActivityBreadcrumb.Advance)) {
        throw new RuntimeException("Unexpected breadcrumb on waitForActivity(): " + breadcrumb.getClass().getName());
      }

      this.currentHistory = ((ActivityBreadcrumb.Advance<T, Event>) breadcrumb).next;
    }
  }

  public final void waitForChildren() {
    for (final var child : this.children) this.waitForActivity(child);
  }

  @Override
  public final String spawn(final Activity child) {
    final String childId;
    if (this.nextBreadcrumbIndex >= breadcrumbs.size()) {
      this.currentHistory = this.currentHistory.fork();

      // TODO: It is somewhat a code smell that we have to conjure our IDs randomly from the ether.
      //   Figure out a better way to identify activity instances.
      //   Make sure we handle the case in `ReplayingSimulationEngine`, too.
      childId = UUID.randomUUID().toString();
      this.spawns = this.spawns.plus(Pair.of(childId, new ActivityContinuation<>(child, this.currentHistory)));

      this.breadcrumbs = this.breadcrumbs.plus(new ActivityBreadcrumb.Spawn<>(childId));
      this.nextBreadcrumbIndex += 1;
    } else {
      final var breadcrumb = this.breadcrumbs.get(this.nextBreadcrumbIndex++);
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
    if (this.nextBreadcrumbIndex >= breadcrumbs.size()) {
      // TODO: It is somewhat a code smell that we have to conjure our IDs randomly from the ether.
      //   Figure out a better way to identify activity instances.
      //   Make sure we handle the case in `ReplayingSimulationEngine`, too.
      childId = UUID.randomUUID().toString();
      this.deferred = this.deferred.plus(childId, new ScheduleItem.Defer<>(delay, new ActivityContinuation<>(child)));

      this.breadcrumbs = this.breadcrumbs.plus(new ActivityBreadcrumb.Spawn<>(childId));
      this.nextBreadcrumbIndex += 1;
    } else {
      final var breadcrumb = this.breadcrumbs.get(this.nextBreadcrumbIndex++);
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
