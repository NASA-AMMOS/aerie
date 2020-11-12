package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Context;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.FooActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.FooEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.FooResources;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.ClosedInterval;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealCondition;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

public final class FooActivity<$Schema> extends Activity<$Schema> {
  @Override
  public void modelEffects(
      final Context<? extends $Schema, FooEvent, FooActivityInstance<$Schema>> ctx,
      final FooResources<$Schema> resources)
  {
    resources.rate.add(ctx, 1.0);
    ctx.delay(1, Duration.SECOND);
    ctx.waitFor(resources.dataVolume, new RealCondition(ClosedInterval.between(5.0, 10.0)));
    resources.rate.add(ctx, 2.0);
    resources.rate.add(ctx, resources.rate.get(ctx));
  }
}
