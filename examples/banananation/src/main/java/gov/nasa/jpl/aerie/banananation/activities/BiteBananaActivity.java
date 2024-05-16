package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.banananation.Flag;
import gov.nasa.jpl.aerie.banananation.Mission;
import gov.nasa.jpl.aerie.contrib.metadata.Unit;
import gov.nasa.jpl.aerie.contrib.models.ValidationResult;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.AutoValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Parameter;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Validation;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.spawn;

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

  @Banannotation("Specifies the size of bite to take")
  @Unit("m")
  public double biteSize = 1.0;

  @Validation
  public ValidationResult validateBiteSize() {
    return new ValidationResult(this.biteSize > 0, "biteSize", "bite size must be positive");
  }

  @EffectModel
  public ComputedAttributes run(final Mission mission) {
    final var bigBiteSize = biteSize > 1.0;
    final var newFlag = bigBiteSize ? Flag.B : Flag.A;
    mission.flag.set(newFlag);
    mission.fruit.subtract(biteSize);

    mission.warnings.log("BiteBanana issued a warning!");

    spawn(() -> {
      mission.warnings.log("BiteBanana sub-task issued another warning! ");
    });

    return new ComputedAttributes(bigBiteSize, newFlag);
  }

  @AutoValueMapper.Record
  public record ComputedAttributes(boolean biteSizeWasBig, Flag newFlag) {}
}
