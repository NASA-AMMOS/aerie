package gov.nasa.jpl.aerie.foomissionmodel.activities;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

import gov.nasa.jpl.aerie.foomissionmodel.Mission;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

@ActivityType("OtherControllableDurationActivity")
public record OtherControllableDurationActivity(Duration duration) {

  @ActivityType.EffectModel
  @ActivityType.ControllableDuration(parameterName = "duration")
  public void run(final Mission mission) {
    delay(duration);
  }
}
