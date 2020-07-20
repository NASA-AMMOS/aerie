package gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.state.BananaStates.Flag;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;

import java.util.ArrayList;
import java.util.List;

import static gov.nasa.jpl.ammos.mpsa.aerie.banananation.state.BananaStates.flag;
import static gov.nasa.jpl.ammos.mpsa.aerie.banananation.state.BananaStates.fruit;

/**
 * Bite a banana.
 *
 * This activity causes a piece of banana to be bitten off and consumed.
 *
 * @subsystem fruit
 * @contact John Doe
 */
@ActivityType(name="BiteBanana", generateMapper=true)
public final class BiteBananaActivity implements Activity {
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
  public void modelEffects() {
    flag.set(Flag.B);
    fruit.add(-biteSize);
  }
}
