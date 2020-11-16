package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Resources;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ResourcesBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.models.DataModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.models.RegisterModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.states.CumulableState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.generated.activities.TaskSpec;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.discrete.DiscreteResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.BooleanValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.DoubleValueMapper;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;
import java.util.Set;

public final class FooResources<$Schema> extends Resources<$Schema, FooEvent, TaskSpec> {
  // Need a clear story for how to logically group resource questions and event emissions together.
  // Need a way to produce a condition for a resource.
  // Need a way to assemble conditions into an overall constraint.
  // Need a way to extract constraints from an adaptation.
  // Need a way to pose constraints against activities, and generally modeling activity behavior with resources.
  // Need to support waitUntil(condition) (which ought to subsume waitFor(activity)).
  // Need to port the breadcrumb-based replaying tasks to this framework, so that activities can be written
  //   in direct style (rather than continuation-passing or state-machine style).
  // Need a clear story for external models.
  // Need to collect profiles from published resources as simulation proceeds.
  // Need to generalize RealDynamics to nonlinear polynomials.
  // Need to use a more representative event type for the sample.
  // Need to implement compile-time code generation for various aspects of the Framework.

  public FooResources(final ResourcesBuilder<$Schema, FooEvent> builder) {
    super(builder);
  }

  private final Query<$Schema, DataModel>
      dataModel = model(new DataModel(0.0, 0.0), ev -> ev.d);
  public final RealResource<History<? extends $Schema, ?>>
      dataVolume = resource("volume", dataModel, DataModel.volume);
  public final RealResource<History<? extends $Schema, ?>>
      dataRate = resource("rate", dataModel, DataModel.rate);

  public final RealResource<History<? extends $Schema, ?>>
      combo = resource("combo", dataVolume.plus(dataRate));

  private final Query<$Schema, RegisterModel<Double>>
      fooModel = model(new RegisterModel<>(0.0), ev -> Pair.of(Optional.of(ev.d), Set.of(ev.d)));
  public final DiscreteResource<History<? extends $Schema, ?>, Double>
      foo = resource("foo", fooModel, RegisterModel.value(), new DoubleValueMapper());
  public final DiscreteResource<History<? extends $Schema, ?>, Boolean>
      bar = resource("bar", fooModel, RegisterModel.conflicted, new BooleanValueMapper());

  public final CumulableState<$Schema, ?, ?> rate = submodule("data", new CumulableState<>(dataRate, FooEvent::new));

  // TODO: automatically perform this for each @Daemon annotation
  { daemon("test", this::test); }
  public void test() {
    rate.add(42.0);
  }
}
