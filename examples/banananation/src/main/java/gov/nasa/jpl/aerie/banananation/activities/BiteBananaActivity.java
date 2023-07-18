package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.banananation.Flag;
import gov.nasa.jpl.aerie.banananation.Mission;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.AutoValueMapper;
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

  /**
   * The amount by which to reduce /fruit
   */
  @Parameter
  public double biteSize = 1.0;

  @Validation("bite size must be positive")
  @Validation.Subject("biteSize")
  public boolean validateBiteSize() {
    return this.biteSize > 0;
  }

  @EffectModel
  public ComputedAttributes run(final Mission mission) {
    final var bigBiteSize = biteSize > 1.0;
    final var newFlag = bigBiteSize ? Flag.B : Flag.A;
    mission.flag.set(newFlag);
    mission.fruit.subtract(biteSize);
    return new ComputedAttributes(bigBiteSize, newFlag);
  }

  @AutoValueMapper.Record
  public record ComputedAttributes(boolean biteSizeWasBig, Flag newFlag) {}
}
