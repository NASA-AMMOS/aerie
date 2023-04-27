package gov.nasa.jpl.aerie.banananation.activities;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.*;

import gov.nasa.jpl.aerie.banananation.Mission;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

/**
 * Nap time [banana style]!!!!
 * This activity has no effect :)
 *
 * @subsystem fruit
 * @contact Jane Doe
 */
@ActivityType("BananaNap")
public final class BananaNapActivity {
  @ActivityType.FixedDuration public static final Duration DURATION = Duration.HOUR;

  @EffectModel
  public void run(final Mission mission) {
    delay(DURATION);
  }
}
