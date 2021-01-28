package gov.nasa.jpl.aerie.fooadaptation;

import gov.nasa.jpl.aerie.contrib.models.Clock;
import gov.nasa.jpl.aerie.contrib.models.SampledResource;
import gov.nasa.jpl.aerie.contrib.models.Accumulator;
import gov.nasa.jpl.aerie.contrib.models.Register;
import gov.nasa.jpl.aerie.contrib.models.counters.Counter;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.DoubleValueMapper;
import gov.nasa.jpl.aerie.fooadaptation.generated.Model;
import gov.nasa.jpl.aerie.fooadaptation.models.ComplexData;
import gov.nasa.jpl.aerie.fooadaptation.models.SimpleData;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.resources.real.RealResource;

import java.time.Instant;

public final class Mission extends Model {
  // Need a way to pose constraints against activities, and generally modeling activity behavior with resources.
  // Need a clear story for external models.
  // Need to collect profiles from published resources as simulation proceeds.
  // Need to generalize RealDynamics to nonlinear polynomials.

  public final Register<Double> foo;
  public final Accumulator data;
  public final Accumulator source;
  public final Accumulator sink;
  public final SampledResource<Double> batterySoC;
  public final SimpleData simpleData;
  public final Counter<Integer> activitiesExecuted;
  public final ComplexData complexData;

  public final RealResource combo;

  public final Clock utcClock;

  public Mission(final Registrar registrar) {
    super(registrar);

    this.foo = Register.create(registrar.descend("foo"), 0.0);
    this.data = new Accumulator(registrar.descend("data"));
    this.combo = this.data.volume.resource.plus(this.data.rate.resource);

    this.source = new Accumulator(registrar.descend("source"), 100.0, 1.0);
    this.sink = new Accumulator(registrar.descend("sink"), 0.0, 0.5);

    this.activitiesExecuted = Counter.ofInteger(registrar.descend("activities"), 0);

    this.batterySoC = new SampledResource<>(
        registrar.descend("batterySoC"),
        ()->this.source.volume.get() - this.sink.volume.get(),
        0.0,
        new DoubleValueMapper());

    this.simpleData = new SimpleData(registrar.descend("simple_data"));
    this.complexData = new ComplexData(registrar.descend("complex_data"), 5, ComplexData.ImagerResMode.LOW_RES, 30);

    this.utcClock = new Clock(registrar.descend("utcClock"), Instant.parse("2023-08-18T00:00:00.00Z"));
    // TODO: automatically perform this for each @Daemon annotation
    registrar.daemon("test", this::test);

    registrar.constraint("data_volume_constraint", this.data.volume.isBetween(42.0, 101.0));
  }

  public void test() {
    this.foo.set(21.0);
    this.data.rate.add(42.0);
    this.simpleData.a.activate();
    this.simpleData.b.activate();
  }
}
