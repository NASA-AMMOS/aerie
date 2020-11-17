package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.discrete.DiscreteResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealCondition;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.Set;

public final class ProxyContext<$Schema, TaskSpec>
    implements Context<$Schema, TaskSpec>
{
  private Context<$Schema, TaskSpec> context = null;

  public void setTarget(final Context<$Schema, TaskSpec> context) {
    this.context = context;
  }

  public Context<$Schema, TaskSpec> getTarget() {
    return this.context;
  }


  @Override
  public History<? extends $Schema> now() {
    return this.context.now();
  }

  @Override
  public double ask(final RealResource<? super History<? extends $Schema>> resource) {
    return this.context.ask(resource);
  }

  @Override
  public <T> T ask(final DiscreteResource<? super History<? extends $Schema>, T> resource) {
    return this.context.ask(resource);
  }

  @Override
  public final <Event> void emit(final Event event, final Query<? super $Schema, Event, ?> query) {
    this.context.emit(event, query);
  }

  @Override
  public final String spawn(final TaskSpec taskSpec) {
    return this.context.spawn(taskSpec);
  }

  @Override
  public final String defer(final Duration duration, final TaskSpec taskSpec) {
    return this.context.defer(duration, taskSpec);
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
  public void waitFor(final RealResource<? super History<? extends $Schema>> resource, final RealCondition condition) {
    this.context.waitFor(resource, condition);
  }

  @Override
  public <T> void waitFor(final DiscreteResource<? super History<? extends $Schema>, T> resource, final Set<T> condition) {
    this.context.waitFor(resource, condition);
  }
}
