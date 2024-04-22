package gov.nasa.jpl.aerie.procedural.examples.fooprocedures.procedures;

import gov.nasa.jpl.aerie.constraints.Violations;
import gov.nasa.jpl.aerie.procedural.constraints.Constraint;
import gov.nasa.jpl.aerie.timeline.CollectOptions;
import gov.nasa.jpl.aerie.timeline.collections.profiles.Real;
import gov.nasa.jpl.aerie.timeline.plan.SimulatedPlan;
import org.jetbrains.annotations.NotNull;

public class ConstFruit implements Constraint {
  @NotNull
  @Override
  public Violations run(SimulatedPlan plan, @NotNull CollectOptions options) {
    final var fruit = plan.resource("/fruit", Real::deserialize);

    return Violations.violateOn(
        fruit.equalTo(4),
        false
    );
  }
}
