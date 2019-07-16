package gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.state.IncrementState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.OutputState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;

import java.util.ArrayList;
import java.util.List;

@ActivityType("BiteBanana")
public final class BiteBananaActivity implements Activity {
  @Parameter
  public double biteSize = 1.0;

  @OutputState("fruit")
  public IncrementState<Double> fruitState = null;

  @Override
  public List<String> validateParameters() {
    final List<String> failures = new ArrayList<>();

    if (this.biteSize <= 0) {
      failures.add("bite size must be positive");
    }

    return failures;
  }

  @Override
  public void modelEffects() {
    fruitState.decrementBy(biteSize);
  }
}
