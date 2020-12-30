package gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.BanananationResources;
import gov.nasa.jpl.ammos.mpsa.aerie.banananation.generated.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.annotations.ActivityType.Parameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.annotations.ActivityType.Validation;

import java.util.List;

/**
 * Peel a banana, in preparation for consumption.
 *
 * This activity causes a banana to enter the peeled state, allowing
 * it to be bitten later. Peeling from the wrong end will cause some
 * amount of banana to become unbiteable.
 *
 * @subsystem fruit
 * @contact Jane Doe
 */
@ActivityType("PeelBanana")
public final class PeelBananaActivity {
  private static final double MASHED_BANANA_AMOUNT = 1.0;

  @Parameter
  public String peelDirection = "fromStem";

  @Validation("peel direction must be fromStem or fromTip")
  public boolean validatePeelDirection() {
    return List.of("fromStem", "fromTip").contains(this.peelDirection);
  }

  public final class EffectModel<$Schema> extends Task<$Schema> {
    public void run(final BanananationResources<$Schema> resources) {
      if (peelDirection.equals("fromStem")) {
        resources.fruit.subtract(MASHED_BANANA_AMOUNT);
      }
      resources.peel.subtract(1.0);
    }
  }
}
