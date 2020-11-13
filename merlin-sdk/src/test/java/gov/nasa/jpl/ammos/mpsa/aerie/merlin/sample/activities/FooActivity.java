package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.FooResources;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.ClosedInterval;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealCondition;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration.SECOND;

public final class FooActivity {
  // These aren't activity parameters, since they aren't annotated with @Parameter.
  // TODO: Make these parameters, and update FooActivityInstance to reflect that.
  public int x = 0;
  public String y = "test";

  public final class EffectModel<$Schema> extends Task<$Schema> {
    @Override
    protected void run(final FooResources<$Schema> resources) {
      if (y.equals("test")) {
        resources.rate.add(x);
      }

      resources.rate.add(1.0);
      delay(1, SECOND);
      waitFor(resources.dataVolume, new RealCondition(ClosedInterval.between(5.0, 10.0)));
      resources.rate.add(2.0);
      resources.rate.add(resources.rate.get());
    }
  }
}
