package gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.state.BananaStates;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;

import java.util.ArrayList;
import java.util.List;

import static gov.nasa.jpl.ammos.mpsa.aerie.banananation.state.BananaStates.fruitState;
import static gov.nasa.jpl.ammos.mpsa.aerie.banananation.state.BananaStates.peelState;

/**
 * Peel a banana, in preparation for consumption.
 *
 * This activity causes a banana to enter the peeled state, allowing
 * it to be bitten later. Peeling from the wrong end will cause some
 * amount of banana to become unbiteable.
 *
 * @subsystem fruit
 * @contact Jane Doe
 */
@ActivityType(name="PeelBanana", states=BananaStates.class)
public final class PeelBananaActivity implements Activity {
  private static final double MASHED_BANANA_AMOUNT = 1.0;

  @Parameter
  public String peelDirection = "fromStem";

  @Override
  public List<String> validateParameters() {
    final List<String> failures = new ArrayList<>();

    if (!List.of("fromStem", "fromTip").contains(this.peelDirection)) {
      failures.add("peel direction must be fromStem or fromTip");
    }

    return failures;
  }

  @Override
  public void modelEffects() {
    if (this.peelDirection.equals("fromStem")) {
      fruitState.set(fruitState.get() - MASHED_BANANA_AMOUNT);
    }

    peelState.set(peelState.get() - 1.0);
  }
}
