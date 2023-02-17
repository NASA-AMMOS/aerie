package gov.nasa.jpl.aerie.foomissionmodel.models;

import gov.nasa.jpl.aerie.merlin.framework.ModelActions;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

/**
 * A daemon task that tracks the number of minutes since plan start
 */
public class TimeTrackerDaemon {
  private int minutesElapsed;

  public int getMinutesElapsed() {
    return minutesElapsed;
  }

  public TimeTrackerDaemon(){
    minutesElapsed = 0;
  }

  public void run(){
    minutesElapsed = 0;
    while(true) {
      ModelActions.delay(Duration.MINUTE);
      minutesElapsed++;
    }
  }

}
