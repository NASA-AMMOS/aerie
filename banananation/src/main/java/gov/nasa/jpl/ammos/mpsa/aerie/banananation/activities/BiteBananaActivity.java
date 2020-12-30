package gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.BanananationResources;
import gov.nasa.jpl.ammos.mpsa.aerie.banananation.Flag;
import gov.nasa.jpl.ammos.mpsa.aerie.banananation.generated.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.annotations.ActivityType.Parameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.annotations.ActivityType.Validation;

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
    public void run(final BanananationResources<$Schema> resources) {
      resources.flag.set(Flag.B);
      resources.fruit.subtract(biteSize);
    }
  }
}
