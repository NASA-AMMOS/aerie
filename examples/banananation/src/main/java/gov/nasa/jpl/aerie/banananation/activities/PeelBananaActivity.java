package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.banananation.Mission;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Parameter;

/**
 * Peel a banana, in preparation for consumption.
 *
 * This activity causes a banana to enter the peeled state, allowing
 * it to be bitten later. Peeling from the wrong end will cause some
 * amount of banana to become <b>unbiteable</b>.
 *
 * @subsystem fruit
 * @contact Jane Doe
 */
@ActivityType("PeelBanana")
public final class PeelBananaActivity {
  private static final double MASHED_BANANA_AMOUNT = 1.0;

  public enum PeelDirectionEnum {
    fromStem,
    fromTip,
  }

  /**
   * The direction from which to peel the banana
   *
   * <p>
   *     For example, if <code>peelDirection</code> is <code>fromStem</code>, some fruit will be subtracted due to
   *     banana being mashed. If <code>peelDirection</code> is <code>fromTip</code>, no banana is mashed. In both
   *     cases, the /peel resource is reduced by 1.
   * </p>
   */
  @Parameter
  public PeelDirectionEnum peelDirection = PeelDirectionEnum.fromStem;

  @EffectModel
  public void run(final Mission mission) {
    if (peelDirection.equals(PeelDirectionEnum.fromStem)) {
      mission.fruit.subtract(MASHED_BANANA_AMOUNT);
    }
    mission.peel.subtract(1.0);
  }
}
