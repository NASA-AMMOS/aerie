package gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.state.IncrementState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.OutputState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;

import java.util.ArrayList;
import java.util.List;

@ActivityType("PeelBanana")
public final class PeelBananaActivity implements Activity {
  private static final double MASHED_BANANA_AMOUNT = 1.0;

  @Parameter
  public String peelDirection = "fromStem";

  @OutputState("fruit")
  public IncrementState<Double> fruitState = null;

  @OutputState("peel")
  public IncrementState<Double> peelState = null;

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
    if (peelDirection.equals("fromStem")) {
      fruitState.decrementBy(MASHED_BANANA_AMOUNT);
    }

    peelState.decrementBy(1.0);
  }
}
