package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.banananation.Mission;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

/**
 * This activity type intentionally takes a duration as a parameter, but is not a ControllableDuration activity
 */
@ActivityType("ControllableDurationActivity")
public record ControllableDurationActivity(Duration duration) {

  @EffectModel
  @ActivityType.ControllableDuration(parameterName = "duration")
  public void run(Mission mission) {
    // Creates a profile segment of at most the given duration
    mission.plant.add(1);
    delay(duration);
    mission.plant.add(-1);
  }
}
