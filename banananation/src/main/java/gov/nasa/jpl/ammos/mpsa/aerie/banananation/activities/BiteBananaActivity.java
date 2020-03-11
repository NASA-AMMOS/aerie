package gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.state.BananaStates;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.BasicState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.SettableState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Bite a banana.
 *
 * This activity causes a piece of banana to be bitten off and consumed.
 *
 * @subsystem fruit
 * @contact John Doe
 */
@ActivityType(name="BiteBanana", states=BananaStates.class)
public final class BiteBananaActivity implements Activity<BananaStates> {
  @Parameter
  public double biteSize = 1.0;

  @Parameter
  public List<Integer> intList = null;

  @Parameter
  public List<List<String>> stringList = null;

  @Parameter
  public Map<Integer, List<String>> mappyBoi = null;

  // an obnoxious ideal
  //@Parameter
  //public List<Map<String[][], Map<Integer, List<Float>[]>>> obnoxious;

  @Override
  public List<String> validateParameters() {
    final List<String> failures = new ArrayList<>();

    if (this.biteSize <= 0) {
      failures.add("bite size must be positive");
    }

    return failures;
  }

  @Override
  public void modelEffects(BananaStates states) {
    SettableState<Double> fruitState = states.fruitState;
    fruitState.set(fruitState.get() - biteSize);
  }
}
