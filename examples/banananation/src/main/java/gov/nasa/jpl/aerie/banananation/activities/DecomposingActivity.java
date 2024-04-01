package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.banananation.Mission;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.AllChildren;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Parameter;

import static gov.nasa.jpl.aerie.banananation.generated.ActivityActions.call;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.*;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.HOURS;

public final class DecomposingActivity {
  @ActivityType("parent")
  public static final class ParentActivity {
    @Parameter
    public String label = "unlabeled";

    @AllChildren(children = {"child"})
    @EffectModel
    public void run(final Mission mission) {
      call(mission, new ChildActivity(1));
      delay(30*24, HOURS);
      call(mission, new ChildActivity(2));
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

    @ActivityType.AllChildren(children = {"grandchild"})
    @EffectModel
    public void run(final Mission mission) {
      call(mission, new GrandchildActivity(1));
      delay(15*24, HOURS);
      call(mission, new GrandchildActivity(2));
    }
  }

  @ActivityType("grandchild")
  public static final class GrandchildActivity {
    @Parameter
    public int counter = 0;

    public GrandchildActivity() {}

    public GrandchildActivity(final int counter) {
      this.counter = counter;
    }

    @EffectModel
    @AllChildren(children = {})
    public void run(final Mission mission) {
      delay(6*24, HOURS);
    }
  }
}
