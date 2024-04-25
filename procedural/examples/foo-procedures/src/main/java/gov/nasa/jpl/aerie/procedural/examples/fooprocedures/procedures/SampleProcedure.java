package gov.nasa.jpl.aerie.procedural.examples.fooprocedures.procedures;

import gov.nasa.jpl.aerie.procedural.scheduling.plan.EditablePlan;
import gov.nasa.jpl.aerie.procedural.scheduling.Procedure;
import gov.nasa.jpl.aerie.procedural.scheduling.plan.NewDirective;
import gov.nasa.jpl.aerie.procedural.scheduling.simulation.SimulateOptions;
import gov.nasa.jpl.aerie.scheduling.annotations.SchedulingProcedure;
import gov.nasa.jpl.aerie.timeline.CollectOptions;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.timeline.payloads.activities.AnyDirective;
import gov.nasa.jpl.aerie.timeline.payloads.activities.DirectiveStart;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@SchedulingProcedure
public record SampleProcedure() implements Procedure {
  @Override
  public void run(EditablePlan plan, @NotNull CollectOptions options) {
    final var firstTime = Duration.hours(24);
    final var step = Duration.hours(6);

    var currentTime = firstTime;
    for (var i = 0; i < quantity; i++) {
      plan.create(new NewDirective(
          new AnyDirective(Map.of()),
          "name",
          "BiteBanana",
          new DirectiveStart.Absolute(currentTime)
          ));
      currentTime = currentTime.plus(step);
    }
    plan.commit();
    var results = plan.simulate(new SimulateOptions());
    var size = results.instances().collect().size();
  }
}
