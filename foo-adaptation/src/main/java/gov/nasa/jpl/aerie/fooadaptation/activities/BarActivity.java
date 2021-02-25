package gov.nasa.jpl.aerie.fooadaptation.activities;

import gov.nasa.jpl.aerie.fooadaptation.Mission;
import gov.nasa.jpl.aerie.fooadaptation.generated.Task;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.time.Duration;

@ActivityType("bar")
public final class BarActivity {
  public final class EffectModel extends Task {
    public void run(final Mission mission) {
      System.out.println("1-start");
      call(() -> {
        System.out.println("2-start");
        spawn(() -> {
          System.out.println("3-start");
          delay(1, Duration.SECOND);
          System.out.println("3-end");
        });
        System.out.println("2-end");
      });
      System.out.println("1-end");
    }
  }
}
