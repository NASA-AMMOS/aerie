package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.banananation.Mission;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Validation;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.WithDefaults;

@ActivityType("BakeBananaBread")
public record BakeBananaBreadActivity(double temperature, int tbSugar, boolean glutenFree) {

  @Validation("Temperature must be positive")
  public boolean validateTemperature() {
    return this.temperature() > 0;
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
