package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.discrete.DiscreteResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealCondition;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.Set;

public final class ProxyContext<$Schema, Event, Activity>
    implements Context<$Schema, Event, Activity>
{
  private Context<$Schema, Event, Activity> context = null;

  public void setTarget(final Context<$Schema, Event, Activity> context) {
    this.context = context;
  }

  public Context<$Schema, Event, Activity> getTarget() {
    return this.context;
  }


  @Override
  public History<? extends $Schema, Event> now() {
    return this.context.now();
  }

  @Override
  public double ask(final RealResource<? super History<? extends $Schema, ?>> resource) {
    return this.context.ask(resource);
  }

  @Override
  public <T> T ask(final DiscreteResource<? super History<? extends $Schema, ?>, T> resource) {
    return this.context.ask(resource);
  }

  @Override
  public final void emit(final Event event) {
    this.context.emit(event);
  }

  @Override
  public final String spawn(final Activity activity) {
    return this.context.spawn(activity);
  }

  @Override
  public final String defer(final Duration duration, final Activity activity) {
    return this.context.defer(duration, activity);
  }

  @Override
  public final void delay(final Duration duration) {
    this.context.delay(duration);
  }

  @Override
  public final void waitFor(final String id) {
    this.context.waitFor(id);
  }

  @Override
  public void waitFor(final RealResource<? super History<? extends $Schema, ?>> resource, final RealCondition condition) {
    this.context.waitFor(resource, condition);
  }

  @Override
  public <T> void waitFor(final DiscreteResource<? super History<? extends $Schema, ?>, T> resource, final Set<T> condition) {
    this.context.waitFor(resource, condition);
  }
}
