package gov.nasa.jpl.aerie.foomissionmodel.activities;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.*;

import gov.nasa.jpl.aerie.foomissionmodel.Mission;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

@ActivityType("BasicActivity")
public final class BasicActivity {
  @EffectModel
  public void run(final Mission mission) {
    delay(Duration.of(2, Duration.SECONDS));
  }
}
