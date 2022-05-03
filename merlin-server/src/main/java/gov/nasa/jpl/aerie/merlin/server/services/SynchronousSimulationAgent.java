package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.MissionModelFacade;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public record SynchronousSimulationAgent (
    PlanService planService,
    MissionModelService missionModelService
) implements SimulationAgent {
  public sealed interface Response {
    record Failed(String reason) implements Response {}
    record Success(SimulationResults results) implements Response {}
  }

  @Override
  public void simulate(final PlanId planId, final RevisionData revisionData, final ResultsProtocol.WriterRole writer) {
    final Plan plan;
    try {
      plan = this.planService.getPlan(planId);

      // Validate plan revision
      final var currentRevisionData = this.planService.getPlanRevisionData(planId);
      final var validationResult = currentRevisionData.matches(revisionData);
      if (validationResult instanceof RevisionData.MatchResult.Failure failure) {
        writer.failWith(String.format("Simulation request no longer relevant: %s", failure.reason()));
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
      // Validate plan activity construction
      {
        final var failures = this.missionModelService.validateActivityInstantiations(
            plan.missionModelId,
            plan.activityInstances.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> new SerializedActivity(e.getValue().type, e.getValue().arguments))));

        if (!failures.isEmpty()) {
          writer.failWith(failures.entrySet().stream()
              .map(e -> "%s: %s".formatted(e.getKey(), e.getValue()))
              .collect(Collectors.joining(System.lineSeparator())));
          return;
        }
      }

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

  private static Map<ActivityInstanceId, Pair<Duration, SerializedActivity>>
  serializeScheduledActivities(
      final Instant startTime,
      final Map<ActivityInstanceId, gov.nasa.jpl.aerie.merlin.server.models.ActivityInstance> activityInstances)
  {
    final var scheduledActivities = new HashMap<ActivityInstanceId, Pair<Duration, SerializedActivity>>();

    for (final var entry : activityInstances.entrySet()) {
      final var id = entry.getKey();
      final var activity = entry.getValue();

      scheduledActivities.put(id, Pair.of(
          Duration.of(startTime.until(activity.startTimestamp.toInstant(), ChronoUnit.MICROS), Duration.MICROSECONDS),
          new SerializedActivity(activity.type, activity.arguments)));
    }

    return scheduledActivities;
  }
}
