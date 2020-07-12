package gov.nasa.jpl.ammos.mpsa.aerie.banananation.state;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.events.BananaEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph.ActivityTypeStateFactory;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ViolableConstraint;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.DoubleState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.IndependentStateFactory;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.SettableState;

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
      new ViolableConstraint(fruit.when(x -> x < 2).and(activities.ofType("BiteBanana").whenActive()))
          .withId("consumingLowFruit")
          .withName("Consuming Low Fruit")
          .withMessage("Fruit rationing must be in effect when fruit is low")
          .withCategory("severe"),
      new ViolableConstraint(fruit.when(x -> x < 2))
          .withId("minFruit")
          .withName("Minimum Fruit")
          .withMessage("Running dangerously low on fruit")
          .withCategory("warning"),
      new ViolableConstraint(fruit.when(x -> x > 10))
          .withId("maxFruit")
          .withName("Maximum Fruit")
          .withMessage("Cannot hold more than 10 fruit")
          .withCategory("severe"),
      new ViolableConstraint(fruit.when(x -> x > 5).and(peel.when(y -> y > 5)))
          .withId("fruitsAndPeels")
          .withName("Fruit Peels")
          .withMessage("Should throw away peels before getting more fruit")
          .withCategory("warning"),
      new ViolableConstraint(peel.when(y -> y > 10))
          .withId("mexPeels")
          .withName("Maximum Peels")
          .withMessage("Too many peels is gross. Clean some up")
          .withCategory("severe")
  );
}
