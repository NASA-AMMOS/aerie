package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.banananation.Mission;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Validation;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.WithDefaults;

@ActivityType("BakeBananaBread")
public record BakeBananaBreadActivity(double temperature, int tbSugar, boolean glutenFree) {

  @Validation("Temperature must be positive")
  @Validation.Subject({"temperature"})
  public boolean validateTemperature() {
    return this.temperature() > 0;
  }

  @Validation("Gluten-free bread must be baked at a temperature >= 100")
  @Validation.Subject({"glutenFree", "temperature"})
  public boolean validateGlutenFreeTemperature() {
    return !glutenFree || temperature >= 100;
  }

  @EffectModel
  public int run(final Mission mission) {
    mission.plant.add(-2);
    return mission.plant.get();
  }

  public static @WithDefaults final class Defaults {
    public static double temperature = 350.0;
  }
}
