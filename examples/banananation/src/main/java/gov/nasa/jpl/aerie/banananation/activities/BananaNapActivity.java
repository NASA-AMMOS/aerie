package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.banananation.Mission;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;

/**
 * Nap time [banana style]!!!!
 * This activity has no effect :)
 *
 * @subsystem fruit
 * @contact Jane Doe
 */
@ActivityType("BananaNap")
public final class BananaNapActivity {
  @EffectModel
  public void run(final Mission mission) {

  }
}
