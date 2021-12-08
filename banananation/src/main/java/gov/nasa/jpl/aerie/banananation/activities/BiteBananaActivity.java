package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.banananation.Flag;
import gov.nasa.jpl.aerie.banananation.Mission;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.Parameter;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.Validation;

/**
 * Bite a banana.
 *
 * This activity causes a piece of banana to be bitten off and consumed.
 *
 * @subsystem fruit
 * @contact John Doe
 */
@ActivityType("BiteBanana")
public final class BiteBananaActivity {
  @Parameter
  public double biteSize = 1.0;

  @Validation("bite size must be positive")
  public boolean validateBiteSize() {
    return this.biteSize > 0;
  }

  @EffectModel
  public void run(final Mission mission) {
    mission.flag.set((biteSize > 1.0) ? Flag.B : Flag.A);
    try {
      Thread.sleep((int)(1000*biteSize));
    } catch (InterruptedException ex) {
      System.err.println("Hold on just a few seconds...");
    }
    mission.fruit.subtract(biteSize);
  }
}
