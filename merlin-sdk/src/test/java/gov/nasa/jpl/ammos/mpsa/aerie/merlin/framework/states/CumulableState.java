package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Module;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealResource;

import java.util.Objects;
import java.util.function.Function;

public final class CumulableState<$Schema, Event, TaskSpec> extends Module<$Schema, TaskSpec> {
  private final Query<$Schema, Event, ?> query;
  private final RealResource<History<? extends $Schema>> resource;
  private final Function<Double, Event> emitter;

  public CumulableState(
      final Query<$Schema, Event, ?> query,
      final RealResource<History<? extends $Schema>> resource,
      final Function<Double, Event> emitter)
  {
    this.query = Objects.requireNonNull(query);
    this.resource = Objects.requireNonNull(resource);
    this.emitter = Objects.requireNonNull(emitter);
  }

  public void add(final double delta) {
    emit(this.emitter.apply(delta), this.query);
  }

  public double get() {
    return ask(this.resource);
  }
}
