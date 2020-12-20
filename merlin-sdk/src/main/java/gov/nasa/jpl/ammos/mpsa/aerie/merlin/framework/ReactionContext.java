package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Condition;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Scheduler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskStatus;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/* package-local */
final class ReactionContext<$Schema, $Timeline extends $Schema>
    implements Context<$Schema>
{
  private final TaskHandle handle;
  private final Scheduler<$Timeline> scheduler;
  private TaskStatus<$Timeline> status = TaskStatus.completed();

  private Optional<History<$Timeline>> history;
  private final List<ActivityBreadcrumb<$Timeline>> breadcrumbs;
  private int nextBreadcrumbIndex;

  public ReactionContext(
      final int initialBreadcrumbIndex,
      final List<ActivityBreadcrumb<$Timeline>> breadcrumbs,
      final Scheduler<$Timeline> scheduler,
      final TaskHandle handle)
  {
    this.nextBreadcrumbIndex = initialBreadcrumbIndex;
    this.history = Optional.empty();
    this.breadcrumbs = breadcrumbs;
    this.scheduler = scheduler;
    this.handle = handle;

    readvance();
  }

  /* package-local */
  TaskStatus<$Timeline> getStatus() {
    return this.status;
  }

  @Override
  public History<$Timeline> now() {
    return this.history.orElseGet(this.scheduler::now);
  }

  @Override
  public <Event> void emit(final Event event, final Query<? super $Schema, Event, ?> query) {
    if (this.history.isEmpty()) {
      this.scheduler.emit(event, query);
    } else {
      // TODO: Avoid leaving garbage behind -- find some way to remove regenerated events
      //   on dead-end branches when references to it disappear.
      this.history = this.history.map(now -> now.emit(event, query));
    }
  }

  @Override
  public <Spec> String spawn(final Spec spec, final TaskSpecType<? super $Schema, Spec> type) {
    if (this.history.isEmpty()) {
      // We're running normally.
      final var id = this.scheduler.spawn(spec, type);

      this.breadcrumbs.add(new ActivityBreadcrumb.Spawn<>(id));
      this.nextBreadcrumbIndex += 1;

      return id;
    } else {
      return respawn();
    }
  }

  @Override
  public <Spec> String defer(final Duration duration, final Spec spec, final TaskSpecType<? super $Schema, Spec> type) {
    if (this.history.isEmpty()) {
      // We're running normally.
      final var id = this.scheduler.defer(duration, spec, type);

      this.breadcrumbs.add(new ActivityBreadcrumb.Spawn<>(id));
      this.nextBreadcrumbIndex += 1;

      return id;
    } else {
      return respawn();
    }
  }

  @Override
  public String spawn(final String type, final Map<String, SerializedValue> arguments) {
    if (this.history.isEmpty()) {
      // We're running normally.
      final var id = this.scheduler.spawn(type, arguments);

      this.breadcrumbs.add(new ActivityBreadcrumb.Spawn<>(id));
      this.nextBreadcrumbIndex += 1;

      return id;
    } else {
      return respawn();
    }
  }

  @Override
  public void delay(final Duration duration) {
    if (this.history.isEmpty()) {
      // We're running normally.
      this.status = TaskStatus.delayed(duration);

      this.nextBreadcrumbIndex += 1;
      this.handle.yieldTask();
    } else {
      readvance();
    }
  }

  @Override
  public void waitFor(final String id) {
    if (this.history.isEmpty()) {
      // We're running normally.
      this.status = TaskStatus.awaiting(id);

      this.nextBreadcrumbIndex += 1;
      this.handle.yieldTask();
    } else {
      readvance();
    }
  }

  @Override
  public void waitUntil(final Condition<$Schema> condition) {
    if (this.history.isEmpty()) {
      // We're running normally.
      this.status = TaskStatus.awaiting(condition);

      this.nextBreadcrumbIndex += 1;
      this.handle.yieldTask();
    } else {
      readvance();
    }
  }

  private void readvance() {
    if (this.nextBreadcrumbIndex >= this.breadcrumbs.size() - 1) {
      // We've just now caught up.
      this.history = Optional.empty();
      this.nextBreadcrumbIndex += 1;
    } else {
      // We're still behind -- jump to the next breadcrumb.
      final var breadcrumb = this.breadcrumbs.get(this.nextBreadcrumbIndex);
      this.nextBreadcrumbIndex += 1;

      if (!(breadcrumb instanceof ActivityBreadcrumb.Advance)) {
        throw new Error("Expected Advance breadcrumb; got " + breadcrumb.getClass().getName());
      }

      this.history = Optional.of(((ActivityBreadcrumb.Advance<$Timeline>) breadcrumb).next);
    }
  }

  private String respawn() {
    if (this.nextBreadcrumbIndex >= this.breadcrumbs.size() - 1) {
      // We've just now caught up.
      throw new Error("Expected a Spawn breadcrumb while replaying; found none.");
    } else {
      // We're still behind -- jump to the next breadcrumb.
      final var breadcrumb = this.breadcrumbs.get(this.nextBreadcrumbIndex);
      this.nextBreadcrumbIndex += 1;

      if (!(breadcrumb instanceof ActivityBreadcrumb.Spawn)) {
        throw new Error("Expected Spawn breadcrumb; got " + breadcrumb.getClass().getName());
      }

      return ((ActivityBreadcrumb.Spawn<$Timeline>) breadcrumb).activityId;
    }
  }
}
