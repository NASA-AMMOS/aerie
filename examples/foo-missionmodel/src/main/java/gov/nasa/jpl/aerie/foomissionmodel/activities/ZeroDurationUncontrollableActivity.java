package gov.nasa.jpl.aerie.foomissionmodel.activities;

import gov.nasa.jpl.aerie.foomissionmodel.Mission;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;

@ActivityType("ZeroDurationUncontrollableActivity")
public final class ZeroDurationUncontrollableActivity {
  @EffectModel
  public void run(final Mission mission) {}
}
