package gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.state.BananaStates;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.OutputState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.SettableState;

import java.util.ArrayList;
import java.util.List;

/**
 * Bite a banana.
 *
 * This activity causes a piece of banana to be bitten off and consumed.
 *
 * @subsystem fruit
 * @contact John Doe
 */
@ActivityType("BiteBanana")
public final class BiteBananaActivity implements Activity<BananaStates> {
  @Parameter
  public double biteSize = 1.0;

  @Override
  public List<String> validateParameters() {
    final List<String> failures = new ArrayList<>();

    if (this.biteSize <= 0) {
      failures.add("bite size must be positive");
    }

    return failures;
  }

  @Override
  public void modelEffects(SimulationContext<BananaStates> ctx, BananaStates states) {
    SettableState<Double> fruitState = states.fruitState;
    fruitState.setValue(fruitState.getValue() - biteSize);
  }
}
