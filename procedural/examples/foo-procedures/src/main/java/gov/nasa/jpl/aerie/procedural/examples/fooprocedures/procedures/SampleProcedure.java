package gov.nasa.jpl.aerie.procedural.examples.fooprocedures.procedures;

import gov.nasa.jpl.aerie.scheduling.EditablePlan;
import gov.nasa.jpl.aerie.scheduling.Procedure;
import gov.nasa.jpl.aerie.scheduling.plan.NewDirective;
import gov.nasa.jpl.aerie.timeline.CollectOptions;
import gov.nasa.jpl.aerie.timeline.Duration;
import gov.nasa.jpl.aerie.timeline.payloads.activities.AnyDirective;

import java.util.Map;

public class SampleProcedure implements Procedure {
  @Override
  public void run(EditablePlan plan, CollectOptions options) {
    plan.create(new NewDirective(new AnyDirective(Map.of()), "name", "BiteBanana", Duration.hours(24)));
    plan.commit();
  }
}
