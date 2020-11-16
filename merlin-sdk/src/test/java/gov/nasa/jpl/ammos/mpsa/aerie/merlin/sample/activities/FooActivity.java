package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.annotations.Parameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.annotations.Validation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.generated.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.FooResources;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.ClosedInterval;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealCondition;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration.SECOND;

public final class FooActivity {
  @Parameter
  public int x = 0;

  @Parameter
  public String y = "test";

  @Validation("x cannot be exactly 99")
  public boolean validateX() {
    return (x != 99);
  }

  @Validation("y cannot be 'bad'")
  public boolean validateY() {
    return !y.equals("bad");
  }

  public final class EffectModel<$Schema> extends Task<$Schema> {
    public void run(final FooResources<$Schema> resources) {
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
