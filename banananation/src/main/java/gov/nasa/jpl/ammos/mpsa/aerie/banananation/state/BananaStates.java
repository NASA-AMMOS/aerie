package gov.nasa.jpl.ammos.mpsa.aerie.banananation.state;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.events.BananaEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.activities.ActivityTypeStateFactory;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ViolableConstraint;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.independent.DoubleState;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.independent.IndependentStateFactory;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.independent.SettableState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ConditionTypes.StateComparator;

import java.util.List;

import static gov.nasa.jpl.ammos.mpsa.aerie.banananation.state.BananaQuerier.activityQuery;
import static gov.nasa.jpl.ammos.mpsa.aerie.banananation.state.BananaQuerier.ctx;
import static gov.nasa.jpl.ammos.mpsa.aerie.banananation.state.BananaQuerier.query;

public final class BananaStates {
  private static final ActivityTypeStateFactory activities = new ActivityTypeStateFactory(activityQuery);

  public static final IndependentStateFactory factory = new IndependentStateFactory(query, (ev) -> ctx.emit(BananaEvent.independent(ev)));

  public static final DoubleState fruit = factory.cumulative("fruit", 4.0);
  public static final DoubleState peel = factory.cumulative("peel", 4.0);

  public enum Flag { A, B }
  public static final SettableState<Flag> flag = factory.enumerated("flag", Flag.A);

  public static final List<ViolableConstraint> violableConstraints = List.of(
      new ViolableConstraint(fruit.whenLessThan(2.0).and(activities.ofType("BiteBanana").whenActive()))
          .withId("consumingLowFruit")
          .withName("Consuming Low Fruit")
          .withMessage("Fruit rationing must be in effect when fruit is low")
          .withCategory("severe"),
      new ViolableConstraint(fruit.whenLessThan(2.0))
          .withId("minFruit")
          .withName("Minimum Fruit")
          .withMessage("Running dangerously low on fruit")
          .withCategory("warning"),
      new ViolableConstraint(fruit.whenGreaterThan(10.0))
          .withId("maxFruit")
          .withName("Maximum Fruit")
          .withMessage("Cannot hold more than 10 fruit")
          .withCategory("severe"),
      new ViolableConstraint(fruit.whenGreaterThan(5.0).and(peel.whenGreaterThan(5.0)))
          .withId("fruitsAndPeels")
          .withName("Fruit Peels")
          .withMessage("Should throw away peels before getting more fruit")
          .withCategory("warning"),
      new ViolableConstraint(peel.whenGreaterThan(10.0))
          .withId("mexPeels")
          .withName("Maximum Peels")
          .withMessage("Too many peels is gross. Clean some up")
          .withCategory("severe")
  );
  static {
    final var constraintNames = new java.util.HashSet<String>();
    for (final var violableConstraint : violableConstraints) {
      if (!constraintNames.add(violableConstraint.name)) {
        throw new Error("More than one violable constraint with name \"" + violableConstraint.name + "\". Each name must be unique.");
      }
    }
  }
}
