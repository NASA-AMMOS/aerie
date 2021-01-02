package gov.nasa.jpl.ammos.mpsa.aerie.fooadaptation;

import gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.Clock;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.SampledResource;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.Accumulator;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.Register;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.serialization.mappers.DoubleValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.fooadaptation.generated.Model;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.resources.real.RealResource;

import java.time.Instant;

public final class Mission<$Schema> extends Model<$Schema> {
  // Need a way to pose constraints against activities, and generally modeling activity behavior with resources.
  // Need a clear story for external models.
  // Need to collect profiles from published resources as simulation proceeds.
  // Need to generalize RealDynamics to nonlinear polynomials.

  public final Register<$Schema, Double> foo;
  public final Accumulator<$Schema> data;
  public final Accumulator<$Schema> source;
  public final Accumulator<$Schema> sink;
  public final SampledResource<$Schema, Double> batterySoC;

  public final RealResource<$Schema> combo;

  public final Clock<$Schema> utcClock;

  public Mission(final Registrar<$Schema> registrar) {
    super(registrar);

    this.foo = Register.create(registrar.descend("foo"), 0.0);
    this.data = new Accumulator<>(registrar.descend("data"));
    this.combo = this.data.volume.resource.plus(this.data.rate.resource);

    this.source = new Accumulator<>(registrar.descend("source"), 100.0, 1.0);
    this.sink = new Accumulator<>(registrar.descend("sink"), 0.0, 0.5);
    this.batterySoC = new SampledResource<>(
        registrar.descend("batterySoC"),
        ()->this.source.volume.get() - this.sink.volume.get(),
        0.0,
        new DoubleValueMapper());

    Instant instant = Instant.parse("2023-08-18T00:00:00.00Z");
    this.utcClock = new Clock<>(registrar.descend("utcClock"), instant);
    // TODO: automatically perform this for each @Daemon annotation
    registrar.daemon("test", this::test);
  }

  public void test() {
    foo.set(21.0);
    data.rate.add(42.0);
  }
}
