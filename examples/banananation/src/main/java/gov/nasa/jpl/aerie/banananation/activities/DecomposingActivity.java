package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.banananation.Mission;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;
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
      return new DurationSpecification() {
        @Override
        public DurationType getDurationType() {
          return ;
        }

        @Override
        public DurationBounds getDurationBounds() {
          return null;
        }
      };
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
      return new DurationSpecification() {
        @Override
        public DurationType getDurationType() {
          final var grandChildDurationSpecification = GrandchildActivity.getDurationSpecification();
          return DurationType.combine(DurationType.Constant, grandChildDurationSpecification.getDurationType());
        }

        @Override
        public DurationBounds getDurationBounds() {
          return new DurationBounds(constantDuration, constantDuration);
        }
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
      call(new GrandchildActivity(1));
      delay(constantDuration);
      call(new GrandchildActivity(2));
    }
  }

  @ActivityType("grandchild")
  public static final class GrandchildActivity {

    private static final Duration constantDuration = Duration.of(6 * 24, HOURS);

    static DurationSpecification getDurationSpecification() {
      return new DurationSpecification() {
        @Override
        public DurationType getDurationType() {
          return DurationType.Constant;
        }

        @Override
        public DurationBounds getDurationBounds() {
          return new DurationBounds(constantDuration, constantDuration);
        }
      };
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
