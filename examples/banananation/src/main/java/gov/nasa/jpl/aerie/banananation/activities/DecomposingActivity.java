package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.banananation.Mission;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.model.DurationSpecification;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import static gov.nasa.jpl.aerie.banananation.generated.ActivityActions.call;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.*;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.HOURS;

public final class DecomposingActivity {
  @ActivityType("parent")
  public static final class ParentActivity {

    static DurationSpecification getDurationSpecification() {
      return () -> DurationSpecification.DurationType.combineMultiple(
        ChildActivity.getDurationSpecification().getDurationType(),
        new DurationSpecification.DurationType.Constant(Duration.of(30 * 24, HOURS)),
        ChildActivity.getDurationSpecification().getDurationType()
      );
    }

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

    private static Duration constantDuration = Duration.of(15 * 24, HOURS);

    static DurationSpecification getDurationSpecification() {
      return () -> {
        final var grandChildDurationSpecification = GrandchildActivity.getDurationSpecification();
        return DurationSpecification.DurationType.combine(new DurationSpecification.DurationType.Constant(constantDuration), grandChildDurationSpecification.getDurationType());
      };
    }

    @Parameter
    public int counter = 0;

    public ChildActivity() {}

    public ChildActivity(final int counter) {
      this.counter = counter;
    }

    @EffectModel
    public void run(final Mission mission) {
      delay(constantDuration);
      call(new GrandchildActivity(2));
    }
  }

  @ActivityType("grandchild")
  public static final class GrandchildActivity {

    private static final Duration constantDuration = Duration.of(6 * 24, HOURS);

    @ActivityType.DurationSpecification
    public static DurationSpecification getDurationSpecification() {
      return () -> new DurationSpecification.DurationType.Constant(constantDuration);
    }

    @Parameter
    public int counter = 0;

    public GrandchildActivity() {}

    public GrandchildActivity(final int counter) {
      this.counter = counter;
    }

    @EffectModel
    public void run(final Mission mission) {
      delay(constantDuration);
    }
  }
}
