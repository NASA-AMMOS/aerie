package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Scheduler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Resource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import org.apache.commons.lang3.NotImplementedException;

public abstract class Context<$Timeline, Event, Activity> {
  private final Scheduler<$Timeline, Event, Activity> scheduler;

  public Context(final Scheduler<$Timeline, Event, Activity> scheduler) {
    this.scheduler = scheduler;
  }

  public final History<? extends $Timeline, ?> now() {
    return this.scheduler.now();
  }

  public final void emit(final Event event) {
    this.scheduler.emit(event);
  }

  public final String spawn(final Activity activity) {
    return this.scheduler.spawn(activity);
  }

  public final String defer(final Duration duration, final Activity activity) {
    return this.scheduler.defer(duration, activity);
  }

  public final void delay(final Duration duration) {
    throw new NotImplementedException("");
  }

  public final void waitFor(final String id) {
    throw new NotImplementedException("");
  }


  public final void call(final Activity activity) {
    this.waitFor(this.spawn(activity));
  }

  public final String defer(final long quantity, final Duration unit, final Activity activity) {
    return this.defer(Duration.of(quantity, unit), activity);
  }

  public final void delay(final long quantity, final Duration unit) {
    this.delay(Duration.of(quantity, unit));
  }

  public final <T> T ask(final Resource<? super History<? extends $Timeline, ?>, T> resource) {
    return resource.getDynamics(this.now()).getDynamics();
  }
}
