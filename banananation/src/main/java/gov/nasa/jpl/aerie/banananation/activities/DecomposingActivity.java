package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.banananation.Mission;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.Parameter;

import static gov.nasa.jpl.aerie.banananation.generated.ActivityActions.call;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.*;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.HOURS;

public final class DecomposingActivity {
  @ActivityType("parent")
  public static final class ParentActivity {
    @Parameter
    public String label = "unlabeled";

    @EffectModel
    public void run(final Mission mission) {
      call(new ChildActivity(1));
      delay(30*24, HOURS);
      call(new ChildActivity(2));
    }
  }

  @ActivityType("child")
  public static final class ChildActivity {
    @Parameter
    public int counter = 0;

    public ChildActivity() {}

    public ChildActivity(final int counter) {
      this.counter = counter;
    }

    @EffectModel
    public void run(final Mission mission) {
      delay(15*24, HOURS);
    }
  }
}
