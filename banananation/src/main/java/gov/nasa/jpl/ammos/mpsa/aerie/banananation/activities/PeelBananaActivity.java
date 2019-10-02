package gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.state.BananaStates;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.OutputState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationContext;

import java.util.ArrayList;
import java.util.List;

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
@ActivityType("PeelBanana")
public final class PeelBananaActivity implements Activity<BananaStates> {
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
  public void modelEffects(SimulationContext<BananaStates> ctx, BananaStates states) {
    if (peelDirection.equals("fromStem")) {
      states.fruitState.setValue(states.fruitState.getValue() - MASHED_BANANA_AMOUNT);
    }

    states.peelState.setValue(states.peelState.getValue() - 1.0);
  }
}
