package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.Duration;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.AdaptationFacade;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class RunSimulationAction {
  public /*sealed*/ interface Response {
    record Failed(String reason) implements Response {}
    record Success(SimulationResults results) implements Response {}
  }

  private final PlanService planService;
  private final AdaptationService adaptationService;

  public RunSimulationAction(
      final PlanService planService,
      final AdaptationService adaptationService)
  {
    this.planService = Objects.requireNonNull(planService);
    this.adaptationService = Objects.requireNonNull(adaptationService);
  }

  public Response run(final String planId, final long planRevision) {
    final Plan plan;
    try {
      plan = this.planService.getPlanById(planId);

      if (this.planService.getPlanRevisionById(planId) != planRevision) {
        return new Response.Failed("plan with id %s is no longer at revision %s".formatted(
            planId,
            planRevision));
      }
    } catch (final NoSuchPlanException ex) {
      return new Response.Failed("no plan with id %s".formatted(planId));
    }

    final var planDuration = Duration.of(
        plan.startTimestamp.toInstant().until(plan.endTimestamp.toInstant(), ChronoUnit.MICROS),
        Duration.MICROSECONDS);

    final SimulationResults results;
    try {
      results = this.adaptationService.runSimulation(new CreateSimulationMessage(
          plan.adaptationId,
          plan.startTimestamp.toInstant(),
          planDuration,
          serializeScheduledActivities(plan.startTimestamp.toInstant(), plan.activityInstances)));
    } catch (final AdaptationService.NoSuchAdaptationException ex) {
      return new Response.Failed("adaptation for existing plan does not exist");
    } catch (final SimulationDriver.TaskSpecInstantiationException | AdaptationFacade.NoSuchActivityTypeException ex) {
      return new Response.Failed("activity could not be instantiated");
    }

    return new Response.Success(results);
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
