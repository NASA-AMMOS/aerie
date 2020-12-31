package gov.nasa.jpl.ammos.mpsa.aerie.fooadaptation;

import gov.nasa.jpl.ammos.mpsa.aerie.fooadaptation.generated.ModuleX;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.resources.real.RealResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ResourcesBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.ClockModule;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.ComputedModule;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.LinearIntegrationModule;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.RegisterModule;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.serialization.mappers.DoubleValueMapper;

import java.time.Instant;

public final class FooResources<$Schema> extends ModuleX<$Schema> {
  // Need a way to pose constraints against activities, and generally modeling activity behavior with resources.
  // Need a clear story for external models.
  // Need to collect profiles from published resources as simulation proceeds.
  // Need to generalize RealDynamics to nonlinear polynomials.
  // Need to implement compile-time code generation for various aspects of the Framework.

  public final RegisterModule<$Schema, Double> foo;
  public final LinearIntegrationModule<$Schema> data;
  public final LinearIntegrationModule<$Schema> source;
  public final LinearIntegrationModule<$Schema> sink;
  public final ComputedModule<$Schema, Double> batterySoC;

  public final RealResource<$Schema> combo;

  public final ClockModule<$Schema> utcClock;

  public FooResources(final ResourcesBuilder.Cursor<$Schema> builder) {
    super(builder);

    this.foo = RegisterModule.create(builder.descend("foo"), 0.0);
    this.data = new LinearIntegrationModule<>(builder.descend("data"));
    this.combo = this.data.volume.resource.plus(this.data.rate.resource);

    this.source = new LinearIntegrationModule<>(builder.descend("source"), 100.0, 1.0);
    this.sink = new LinearIntegrationModule<>(builder.descend("sink"), 0.0, 0.5);
    this.batterySoC = new ComputedModule<>(
        builder.descend("batterySoC"),
        ()->this.source.volume.get() - this.sink.volume.get(),
        0.0,
        new DoubleValueMapper());

    Instant instant = Instant.parse("2023-08-18T00:00:00.00Z");
    this.utcClock = new ClockModule<>(builder.descend("utcClock"), instant);
    // TODO: automatically perform this for each @Daemon annotation
    builder.daemon("test", this::test);
  }

  public void test() {
    foo.set(21.0);
    data.rate.add(42.0);
  }
}
