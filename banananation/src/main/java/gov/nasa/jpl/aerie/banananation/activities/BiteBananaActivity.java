package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.banananation.Mission;
import gov.nasa.jpl.aerie.banananation.Flag;
import gov.nasa.jpl.aerie.banananation.generated.Task;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
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

  public final class EffectModel<$Schema> extends Task<$Schema> {
    public void run(final Mission<$Schema> resources) {
      resources.flag.set((biteSize > 1.0) ? Flag.B : Flag.A);
      resources.fruit.subtract(biteSize);
    }
  }
}
