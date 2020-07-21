package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.states.States.binA;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.states.States.log;

public final class ActivityA implements Activity {
  @Override
  public void modelEffects() {
    binA.rate.add(binA.rate);
    log.add("" + binA.rate);
    log.add("Hello, from Activity A!");
  }
}
