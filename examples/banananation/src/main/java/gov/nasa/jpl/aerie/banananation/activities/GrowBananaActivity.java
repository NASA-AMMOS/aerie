package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.banananation.Flag;
import gov.nasa.jpl.aerie.banananation.Mission;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.Validation;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.Template;

/**
 * Monke has evolve. Monke now make banana. Monke is farmer.
 *
 * This activity causes a monkey to create new bananas in the banana plant.
 *
 * @subsystem fruit
 * @contact John Doe
 */
@ActivityType("GrowBanana")
public record GrowBananaActivity(int quantity) {

  public static @Template GrowBananaActivity defaults() {
    return new GrowBananaActivity(0);
  }

  @Validation("Quantity must be positive")
  public boolean validateBiteSize() {
    return this.quantity() > 0;
  }

  @EffectModel
  public void run(final Mission mission) {
    mission.plant.add(this.quantity());
  }
}

