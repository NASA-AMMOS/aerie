package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph.ActivityTypeState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph.ActivityTypeStateFactory;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ViolableConstraint;

import java.util.List;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.Querier.ctx;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.Querier.dataQuerier;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.Querier.activityQuerier;

public final class States {
  public static final CumulableResource<String> log = new LogResource(ctx::emit);

  private static final DataBinsResource bins = new DataBinsResource(dataQuerier, ctx::emit);
  public static final DataBinResource binA = bins.bin("bin A");

  private static final ActivityTypeStateFactory activities = new ActivityTypeStateFactory(activityQuerier);
  public static final ActivityTypeState activityA = activities.ofType("ActivityA");
  public static final ActivityTypeState activityB = activities.ofType("ActivityB");

  public static final List<ViolableConstraint> violableConstraints = List.of(
      new ViolableConstraint(activityB.whenActive().minus(activityA.whenActive()))
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
