package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.banananation.Flag;
import gov.nasa.jpl.aerie.banananation.Mission;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Parameter;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Validation;

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
  public ComputedAttributes run(final Mission mission) {
    final var biteSizeWasBig = biteSize > 1.0;
    final var newFlag = biteSizeWasBig ? Flag.B : Flag.A;
    mission.flag.set(newFlag);
    mission.fruit.subtract(biteSize);
    return new ComputedAttributes(biteSizeWasBig, newFlag);
  }

  public record ComputedAttributes(boolean biteSizeWasBig, Flag newFlag) {}
}
