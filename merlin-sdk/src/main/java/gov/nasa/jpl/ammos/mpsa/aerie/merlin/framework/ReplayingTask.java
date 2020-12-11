package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Scheduler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskStatus;

import java.util.ArrayList;
import java.util.List;

public final class ReplayingTask<$Schema, $Timeline extends $Schema>
    implements Task<$Timeline>
{
  private final ProxyContext<$Schema> rootContext;
  private final Runnable task;
  private final List<ActivityBreadcrumb<$Timeline>> breadcrumbs = new ArrayList<>();

  public ReplayingTask(final ProxyContext<$Schema> rootContext, final Runnable task) {
    this.rootContext = rootContext;
    this.task = task;
  }

  @Override
  public TaskStatus<$Timeline> step(final Scheduler<$Timeline> scheduler) {
    this.breadcrumbs.add(new ActivityBreadcrumb.Advance<>(scheduler.now()));

    final var context = new ReactionContext<$Schema, $Timeline>(0, this.breadcrumbs, scheduler);

    {
      final var oldContext = this.rootContext.getTarget();
      this.rootContext.setTarget(context);

      try {
        this.task.run();
        // If we get here, the activity has completed normally.
      } catch (final Yield ignored) {
        // If we get here, the activity has suspended.
      } finally {
        this.rootContext.setTarget(oldContext);
      }
    }

    return context.getStatus();
  }

  // Since this exception is just used to transfer control out of an activity,
  //   we can pre-allocate a single instance as a unique token
  //   to avoid some of the overhead of exceptions
  //   (most notably the call stack snapshotting).
  /* package-local */ static final class Yield extends RuntimeException {}
  /* package-local */ public static final Yield Yield = new Yield();
}
