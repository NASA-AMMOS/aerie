package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.driver.SimulationException;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResultsInterface;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.http.ResponseSerializers;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;

import javax.json.Json;
import java.time.temporal.ChronoUnit;
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
      plan = this.planService.getPlanForSimulation(planId);

      // Validate plan revision
      final var currentRevisionData = this.planService.getPlanRevisionData(planId);
      final var validationResult = currentRevisionData.matches(revisionData);
      if (validationResult instanceof RevisionData.MatchResult.Failure failure) {
        writer.failWith(b -> b
            .type("SIMULATION_REQUEST_NOT_RELEVANT")
            .message("Simulation request no longer relevant: %s".formatted(failure.reason())));
        return;
      }
    } catch (final NoSuchPlanException ex) {
      writer.failWith(b -> b
          .type("NO_SUCH_PLAN")
          .message(ex.toString())
          .data(ResponseSerializers.serializeNoSuchPlanException(ex))
          .trace(ex));
      return;
    }

    final var planDuration = Duration.of(
        plan.startTimestamp.toInstant().until(plan.endTimestamp.toInstant(), ChronoUnit.MICROS),
        Duration.MICROSECONDS);
    final var simDuration = Duration.of(
        plan.simulationStartTimestamp.toInstant().until(plan.simulationEndTimestamp.toInstant(), ChronoUnit.MICROS),
        Duration.MICROSECONDS);

    final SimulationResultsInterface results;
    try {
      // Validate plan activity construction
      {
        final var failures = this.missionModelService.validateActivityInstantiations(
            plan.missionModelId,
            plan.activityDirectives.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().serializedActivity())));

        if (!failures.isEmpty()) {
          writer.failWith(b -> b
              .type("PLAN_CONTAINS_UNCONSTRUCTABLE_ACTIVITIES")
              .message("Plan contains unconstructable activities")
              .data(ResponseSerializers.serializeUnconstructableActivityFailures(failures)));
          return;
        }
      }

      results = this.missionModelService.runSimulation(new CreateSimulationMessage(
          plan.missionModelId,
          plan.simulationStartTimestamp.toInstant(),
          simDuration,
          plan.startTimestamp.toInstant(),
          planDuration,
          plan.activityDirectives,
          plan.configuration));
    } catch (SimulationException ex) {
      writer.failWith(b -> b
          .type("SIMULATION_EXCEPTION")
          .data(Json.createObjectBuilder()
                    .add("elapsedTime", SimulationException.formatDuration(ex.elapsedTime))
                    .add("utcTimeDoy", SimulationException.formatInstant(ex.instant))
                    .build())
          .trace(ex.cause));
      return;
    } catch (final MissionModelService.NoSuchMissionModelException ex) {
      writer.failWith(b -> b
          .type("NO_SUCH_MISSION_MODEL")
          .message(ex.toString())
          .data(ResponseSerializers.serializeNoSuchMissionModelException(ex))
          .trace(ex));
      return;
    } catch (final MissionModelService.NoSuchActivityTypeException ex) {
      writer.failWith(b -> b
          .type("NO_SUCH_ACTIVITY_TYPE")
          .message("Activity of type `%s` could not be instantiated".formatted(ex.activityTypeId))
          .data(ResponseSerializers.serializeNoSuchActivityTypeException(ex))
          .trace(ex));
      return;
    }

    writer.succeedWith(results);
  }
}
