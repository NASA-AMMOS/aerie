package gov.nasa.jpl.aerie.fooadaptation;

import gov.nasa.jpl.aerie.contrib.models.Accumulator;
import gov.nasa.jpl.aerie.contrib.models.Clock;
import gov.nasa.jpl.aerie.contrib.models.Register;
import gov.nasa.jpl.aerie.contrib.models.counters.Counter;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.BooleanValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.DoubleValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.IntegerValueMapper;
import gov.nasa.jpl.aerie.fooadaptation.models.Imager;
import gov.nasa.jpl.aerie.fooadaptation.models.ImagerMode;
import gov.nasa.jpl.aerie.fooadaptation.models.SimpleData;
import gov.nasa.jpl.aerie.merlin.framework.ModelActions;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.resources.real.RealResource;
import gov.nasa.jpl.aerie.time.Duration;

import java.time.Instant;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.*;

public final class Mission {
  // Need a way to pose constraints against activities, and generally modeling activity behavior with resources.
  // Need a clear story for external models.
  // Need to generalize RealDynamics to nonlinear polynomials.

  public final Register<Double> foo = Register.create(0.0);
  public final Accumulator data = new Accumulator();
  public final Accumulator source = new Accumulator(100.0, 1.0);
  public final Accumulator sink;
  public final SimpleData simpleData = new SimpleData();
  public final Counter<Integer> activitiesExecuted = Counter.ofInteger(0);
  public final Imager complexData = new Imager(5, ImagerMode.LOW_RES, 30);

  public final RealResource combo = this.data.plus(this.data.rate);

  public final Clock utcClock = new Clock(Instant.parse("2023-08-18T00:00:00.00Z"));
  private final Registrar cachedRegistrar; // Normally bad practice, only stored to demonstrate built/unbuilt check

  public Mission(final Registrar registrar, final Configuration config) {
    this.cachedRegistrar = registrar;

    this.sink = new Accumulator(0.0, config.sinkRate);

    spawn(this::test);

    // Assert adaptation is unbuilt
    if (registrar.isInitializationComplete())
      throw new AssertionError("Registrar should not report initialization as complete");

    // TODO: Move resource registration out into an Aerie-specific binding layer.
    //   (This binding layer should also be the one responsible for feeding in constructor parameters.)
    registrar.resource("/foo", this.foo, new DoubleValueMapper());
    registrar.resource("/foo/conflicted", this.foo::isConflicted, new BooleanValueMapper());
    registrar.resource("/batterySoC", this.source.minus(this.sink));
    registrar.resource("/data", this.data);
    registrar.resource("/data/rate", this.data.rate);
    registrar.resource("/source", this.source);
    registrar.resource("/source/rate", this.source.rate);
    registrar.resource("/sink", this.sink);
    registrar.resource("/sink/rate", this.sink.rate);
    registrar.resource("/activitiesExecuted", this.activitiesExecuted, new IntegerValueMapper());
    registrar.resource("/utcClock", this.utcClock.ticks);

    registrar.resource("/simple_data/a/volume", this.simpleData.a.volume);
    registrar.resource("/simple_data/a/rate", this.simpleData.a.rate);
    registrar.resource("/simple_data/b/volume", this.simpleData.b.volume);
    registrar.resource("/simple_data/b/rate", this.simpleData.b.rate);
    registrar.resource("/simple_data/total_volume", this.simpleData.totalVolume);

    spawn(() -> { // Register a never-ending daemon task
      while (true) {
        ModelActions.delay(Duration.SECOND);
      }
    });
  }

  public void test() {
    this.foo.set(21.0);
    this.data.rate.add(42.0);
    this.simpleData.a.activate();
    this.simpleData.b.activate();

    // Assert adaptation is built
    if (!cachedRegistrar.isInitializationComplete())
      throw new AssertionError("Registrar should report initialization as complete");
  }
}
