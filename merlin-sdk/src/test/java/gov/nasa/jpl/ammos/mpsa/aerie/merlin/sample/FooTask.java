package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ActivityStatus;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Scheduler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealSolver;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

// TODO: Port the ReplayingTask machinery here, and move this into the framework module.
public final class FooTask<$Schema, $Timeline extends $Schema>
    implements Task<$Timeline, FooEvent, FooActivityInstance<$Schema>>
{
  private final FooResources<$Schema> resources;
  private final FooActivityInstance<$Schema> activity;
  private int state;

  public FooTask(final FooResources<$Schema> resources, final FooActivityInstance<$Schema> activity) {
    this.resources = resources;
    this.activity = activity;
    this.state = 0;
  }

  public @Override
  ActivityStatus
  step(final Scheduler<$Timeline, FooEvent, FooActivityInstance<$Schema>> scheduler)
  {
    switch (this.state) {
      case 0:
        scheduler.emit(new FooEvent(1.0));
        this.state = 1;
        return ActivityStatus.delayed(1, Duration.SECOND);

      case 1:
        scheduler.emit(new FooEvent(2.0));

        final var delimitedDynamics = this.resources.dataRate.getDynamics(scheduler.now());
        final var rate = new RealSolver().valueAt(delimitedDynamics.getDynamics(), Duration.ZERO);
        scheduler.emit(new FooEvent(rate));

        this.state = 2;
        return ActivityStatus.delayed(200, Duration.MILLISECONDS);

      case 2:
      default:
        return ActivityStatus.completed();
    }
  }
}
