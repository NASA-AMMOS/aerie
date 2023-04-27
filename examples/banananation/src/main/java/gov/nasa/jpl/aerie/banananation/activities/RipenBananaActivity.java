package gov.nasa.jpl.aerie.banananation.activities;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

import gov.nasa.jpl.aerie.banananation.Mission;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

/**
 * Monke is patient.
 *
 * Waits two days for bananas to ripen. Ripeness is not modelled.
 *
 * @subsystem fruit
 * @contact Jane Doe
 */
@ActivityType("RipenBanana")
public final class RipenBananaActivity {

  @ActivityType.FixedDuration
  public static Duration duration() {
    return Duration.of(48, Duration.HOUR);
  }

  @EffectModel
  public void run(final Mission mission) {
    delay(duration());
  }
}
