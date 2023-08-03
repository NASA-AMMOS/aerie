package gov.nasa.jpl.aerie.foomissionmodel.models;

import gov.nasa.jpl.aerie.contrib.models.counters.Counter;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.*;
import static gov.nasa.jpl.aerie.merlin.framework.TrampoliningTask.RepeatingTaskStatus.delayed;

/**
 * A daemon task that tracks the number of minutes since plan start
 */
public class TimeTrackerDaemon {
  private Counter<Integer> minutesElapsed;

  public int getMinutesElapsed() {
    return minutesElapsed.get();
  }

  public TimeTrackerDaemon(){ minutesElapsed = Counter.ofInteger(0);}

  public void run() {
    defer(Duration.MINUTE, () ->
        spawn(repeating(() -> {
          minutesElapsed.add(1);
          return delayed(Duration.MINUTE);
        })));
  }
}
