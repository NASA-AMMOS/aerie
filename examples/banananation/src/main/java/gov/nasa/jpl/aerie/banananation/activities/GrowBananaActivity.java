package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.banananation.Mission;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.ControllableDuration;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Template;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Validation;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.*;

/**
 * Monke has evolve. Monke now make banana. Monke is farmer.
 *
 * This activity causes a monkey to create new bananas in the banana plant.
 *
 * @subsystem fruit
 * @contact John Doe
 */
@ActivityType("GrowBanana")
public record GrowBananaActivity(int quantity, Duration growingDuration) {

  public static @Template GrowBananaActivity defaults() {
    return new GrowBananaActivity(0, Duration.ZERO);
  }

  @Validation("Quantity must be positive")
  public boolean validateBiteSize() {
    return this.quantity() > 0;
  }

  @EffectModel
  @ControllableDuration(parameterName = "growingDuration")
  public void run(final Mission mission) {
    delay(this.growingDuration());
    mission.plant.add(this.quantity());
  }
}
