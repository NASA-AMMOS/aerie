package gov.nasa.jpl.aerie.procedural.examples.fooprocedures.constraints;

import gov.nasa.jpl.aerie.constraints.Constraint;
import gov.nasa.jpl.aerie.constraints.Violations;
import gov.nasa.jpl.aerie.timeline.collections.profiles.Real;
import gov.nasa.jpl.aerie.timeline.plan.Plan;
import gov.nasa.jpl.aerie.timeline.CollectOptions;
import org.jetbrains.annotations.NotNull;

class ConstFruit implements Constraint {
  @NotNull
  @Override
  public Violations run(Plan plan, @NotNull CollectOptions options) {
    final var fruit = plan.resource("/fruit", Real::deserialize);

    return Violations.violateOn(
        fruit.equalTo(4),
        false
    );
  }
}
