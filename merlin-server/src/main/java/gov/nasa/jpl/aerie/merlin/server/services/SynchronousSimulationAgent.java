package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.driver.StartOffsetReducer;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.http.ResponseSerializers;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import org.apache.commons.lang3.tuple.Pair;

import javax.naming.spi.Resolver;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
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
    final PlanService.SimulationArguments configuration;
    try {
      plan = this.planService.getPlan(planId);
      configuration = this.planService.getSimulationArguments(planId, plan.startTimestamp, plan.duration());

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

    final SimulationResults results;
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
          plan.startTimestamp.plusMicros(configuration.offsetFromPlanStart().in(Duration.MICROSECONDS)).toInstant(),
          planDuration,
          configuration.duration(),
          filterByStartTime(plan.activityDirectives, configuration.offsetFromPlanStart(), plan.duration()),
          configuration.modelArguments()));
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

  private Map<ActivityDirectiveId, ActivityDirective> filterByStartTime(final Map<ActivityDirectiveId, ActivityDirective> activityDirectives, final Duration offsetFromPlanStart, final Duration planDuration) {
    final var reduced = new StartOffsetReducer(planDuration, activityDirectives).compute();
    final var relativeToPlan = reduced.get(null);
    final var schedule = new HashMap<ActivityDirectiveId, ActivityDirective>();
    for (final var entry : relativeToPlan) {
      final var directiveId = entry.getKey();
      final var directive = activityDirectives.get(directiveId);
      final var newOffset = entry.getValue().minus(offsetFromPlanStart);
      if (newOffset.isNegative()) continue;
      schedule.put(directiveId, new ActivityDirective(
          newOffset,
          directive.serializedActivity(),
          null,
          true
      ));
    }
    for (final var entry : reduced.entrySet()) {
      if (entry.getKey() == null) continue;
      final var anchorId = entry.getKey();
      final var anchoredDirectives = entry.getValue();
      if (schedule.containsKey(anchorId)) {
        for (final var directive : anchoredDirectives) {
          schedule.put(directive.getKey(), new ActivityDirective(
              directive.getRight(),
              activityDirectives.get(directive.getKey()).serializedActivity(),
              anchorId,
              false
          ));
        }
      }
    }
    return schedule;
  }
}
