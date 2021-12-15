package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.MissionModelFacade;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public record SynchronousSimulationAgent (
    PlanService planService,
    MissionModelService missionModelService
) implements SimulationAgent {
  public /*sealed*/ interface Response {
    record Failed(String reason) implements Response {}
    record Success(SimulationResults results) implements Response {}
  }

  @Override
  public void simulate(final String planId, final long planRevision, final ResultsProtocol.WriterRole writer) {
    final Plan plan;
    try {
      plan = this.planService.getPlan(planId);

      if (this.planService.getPlanRevision(planId) != planRevision) {
        writer.failWith("plan with id %s is no longer at revision %s".formatted(
            planId,
            planRevision));
        return;
      }
    } catch (final NoSuchPlanException ex) {
      writer.failWith("no plan with id %s".formatted(planId));
      return;
    }

    final var planDuration = Duration.of(
        plan.startTimestamp.toInstant().until(plan.endTimestamp.toInstant(), ChronoUnit.MICROS),
        Duration.MICROSECONDS);

    final SimulationResults results;
    try {
      results = this.missionModelService.runSimulation(new CreateSimulationMessage(
          plan.missionModelId,
          plan.startTimestamp.toInstant(),
          planDuration,
          serializeScheduledActivities(plan.startTimestamp.toInstant(), plan.activityInstances),
          plan.configuration));
    } catch (final MissionModelService.NoSuchMissionModelException ex) {
      writer.failWith("mission model for existing plan does not exist");
      return;
    } catch (final MissionModelFacade.NoSuchActivityTypeException ex) {
      writer.failWith("activity could not be instantiated");
      return;
    }

    writer.succeedWith(results);
  }

  private static Map<String, Pair<Duration, SerializedActivity>>
  serializeScheduledActivities(
      final Instant startTime,
      final Map<String, gov.nasa.jpl.aerie.merlin.server.models.ActivityInstance> activityInstances)
  {
    final var scheduledActivities = new HashMap<String, Pair<Duration, SerializedActivity>>();

    for (final var entry : activityInstances.entrySet()) {
      final var id = entry.getKey();
      final var activity = entry.getValue();

      scheduledActivities.put(id, Pair.of(
          Duration.of(startTime.until(activity.startTimestamp.toInstant(), ChronoUnit.MICROS), Duration.MICROSECONDS),
          new SerializedActivity(activity.type, activity.parameters)));
    }

    return scheduledActivities;
  }
}
