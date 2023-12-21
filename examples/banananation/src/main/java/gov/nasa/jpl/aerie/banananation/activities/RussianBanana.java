package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.banananation.Mission;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.List;

import static gov.nasa.jpl.aerie.banananation.generated.ActivityActions.call;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

/**
 * Russian Banana Encloses Banana
 *
 * This activity causes a piece of banana to be bitten off and consumed.
 *
 * @subsystem fruit
 * @contact John Doe
 */
@ActivityType("RussianBanana")
public final class RussianBanana {

  @Export.Parameter
  public List<Integer> testints;

  @Export.Parameter
  public List<BiteBananaActivity> biteBananaActivity;

  @Export.Parameter
  public PeelBananaActivity peelBananaActivity;


  @ActivityType.EffectModel
  public void run(final Mission mission) {
    for (final var bite : biteBananaActivity) {
      call(mission, bite);
      delay(Duration.of(30, Duration.MINUTE));
    }
    call(mission, peelBananaActivity);
  }

}
