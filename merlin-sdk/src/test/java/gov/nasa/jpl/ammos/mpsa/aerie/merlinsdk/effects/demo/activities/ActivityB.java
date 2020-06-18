package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.states.States.call;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.states.States.log;

public final class ActivityB implements Activity {
  @Override
  public void modelEffects() {
    log.add("Before B");
    call(new ActivityA());
    log.add("After B");
  }
}
