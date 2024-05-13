package gov.nasa.jpl.aerie.scheduler.goals;

import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.procedural.scheduling.ProcedureMapper;
import gov.nasa.jpl.aerie.procedural.scheduling.plan.Edit;
import gov.nasa.jpl.aerie.scheduler.ProcedureLoader;
import gov.nasa.jpl.aerie.scheduler.model.ActivityType;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirectiveId;
import gov.nasa.jpl.aerie.scheduler.plan.InMemoryEditablePlan;
import gov.nasa.jpl.aerie.scheduler.plan.SchedulerToProcedurePlanAdapter;
import gov.nasa.jpl.aerie.scheduler.simulation.SimulationFacade;
import gov.nasa.jpl.aerie.scheduler.solver.Evaluation;
import gov.nasa.jpl.aerie.timeline.CollectOptions;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.timeline.payloads.activities.DirectiveStart;
import org.apache.commons.lang3.NotImplementedException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static gov.nasa.jpl.aerie.scheduler.plan.InMemoryEditablePlan.toSchedulingActivityDirective;

public class Procedure extends Goal {
  //  private final gov.nasa.jpl.aerie.scheduling.Procedure procedure;
  private final String jarPath;
  private final Map<String, SerializedValue> args;

  public Procedure(final PlanningHorizon planningHorizon, String jarPath, Map<String, SerializedValue> args) {
    this.simulateAfter = true;
    this.planHorizon = planningHorizon;
    this.jarPath = jarPath;
    this.args = args;
  }

  public void run(Evaluation eval, Plan plan, MissionModel<?> missionModel, Function<String, ActivityType> lookupActivityType, SimulationFacade simulationFacade) {
    final ProcedureMapper<?> procedureMapper;
    try {
      procedureMapper = ProcedureLoader.loadProcedure(Path.of(jarPath));
    } catch (ProcedureLoader.ProcedureLoadException e) {
      throw new RuntimeException(e);
    }

    List<SchedulingActivityDirective> newActivities = new ArrayList<>();

    final var inMemoryPlan = new SchedulerToProcedurePlanAdapter(
        plan,
        planHorizon
    );

    final var nextUniqueDirectiveId = plan.getActivities().stream().map($ -> Math.abs($.id().id())).max(Long::compare).orElse(0L) + 1;

    final var editablePlan = new InMemoryEditablePlan(
        missionModel,
        nextUniqueDirectiveId,
        inMemoryPlan,
        simulationFacade,
        lookupActivityType::apply
    );

    /*
     TODO

     Comments from Joel:
     - Part of the intent of editablePlan was to be able to re-use it across procedures.
       - Could be done by initializing EditablePlanImpl with simulation results

     Duration construction and arithmetic can be less awkward
     */

    final var options = new CollectOptions(inMemoryPlan.totalBounds());

    procedureMapper.deserialize(SerializedValue.of(this.args)).run(editablePlan, options);

    if (!editablePlan.getUncommittedChanges().isEmpty()) {
      throw new NotImplementedException("emit warning");
    }
    for (final var edit : editablePlan.getTotalDiff()) {
      if (edit instanceof Edit.Create c) {
        newActivities.add(toSchedulingActivityDirective(c.getDirective(), lookupActivityType::apply));
      } else {
        throw new IllegalStateException("Unexpected value: " + edit);
      }
    }

    final var evaluation = eval.forGoal(this);
    for (final var activity : newActivities) {
      plan.add(activity);
      evaluation.associate(activity, true);
    }
    evaluation.setScore(0.0);
  }
}