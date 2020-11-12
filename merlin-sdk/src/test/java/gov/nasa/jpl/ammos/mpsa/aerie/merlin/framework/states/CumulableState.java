package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealResource;

import java.util.Objects;
import java.util.function.Function;

public final class CumulableState<$Schema, Event> {
  private final RealResource<History<? extends $Schema, ?>> resource;
  private final Function<Double, Event> emitter;

  public CumulableState(
      final RealResource<History<? extends $Schema, ?>> resource,
      final Function<Double, Event> emitter)
  {
    this.resource = Objects.requireNonNull(resource);
    this.emitter = Objects.requireNonNull(emitter);
  }

  public void add(
      final gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Context<? extends $Schema, Event, ?> ctx,
      final double delta)
  {
    ctx.emit(this.emitter.apply(delta));
  }

  public double get(final gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Context<? extends $Schema, Event, ?> ctx) {
    return ctx.ask(this.resource);
  }
}
