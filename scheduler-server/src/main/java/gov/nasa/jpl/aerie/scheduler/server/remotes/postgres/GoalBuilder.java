package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.ActivityCreationTemplate;
import gov.nasa.jpl.aerie.scheduler.ActivityType;
import gov.nasa.jpl.aerie.scheduler.CompositeAndGoal;
import gov.nasa.jpl.aerie.scheduler.Goal;
import gov.nasa.jpl.aerie.scheduler.OptionGoal;
import gov.nasa.jpl.aerie.scheduler.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.RecurrenceGoal;
import gov.nasa.jpl.aerie.scheduler.Time;
import gov.nasa.jpl.aerie.scheduler.server.models.GoalId;
import gov.nasa.jpl.aerie.scheduler.server.models.GoalRecord;
import gov.nasa.jpl.aerie.scheduler.server.models.SchedulingDSL;
import gov.nasa.jpl.aerie.scheduler.server.models.Timestamp;
import gov.nasa.jpl.aerie.scheduler.server.services.SchedulingDSLCompilationService;

import java.io.IOException;

public class GoalBuilder {
  sealed interface GoalBuildResult {
    record Success(GoalRecord goalRecord) implements GoalBuildResult {}
    record Failure(String reason) implements GoalBuildResult {}
  }
  static GoalBuildResult buildGoalRecord(
      final PostgresGoalRecord pgGoal,
      final Timestamp horizonStartTimestamp,
      final Timestamp horizonEndTimestamp,
      final SchedulingDSLCompilationService schedulingDSLCompilationService)
  {
    final SchedulingDSL.GoalSpecifier goalSpecifier;
    try {
      goalSpecifier = schedulingDSLCompilationService.compileSchedulingGoalDSL(
          pgGoal.definition(),
          pgGoal.name());
    } catch (SchedulingDSLCompilationService.SchedulingDSLCompilationException e) {
      return new GoalBuildResult.Failure(e.getMessage());
    }
    final var goalId = new GoalId(pgGoal.id());

    final var goal = goalOfGoalSpecifier(
        goalSpecifier,
        horizonStartTimestamp,
        horizonEndTimestamp);

    return new GoalBuildResult.Success(new GoalRecord(goalId, goal));
  }

  private static Goal goalOfGoalSpecifier(
      final SchedulingDSL.GoalSpecifier goalSpecifier,
      final Timestamp horizonStartTimestamp,
      final Timestamp horizonEndTimestamp) {
    if (goalSpecifier instanceof SchedulingDSL.GoalSpecifier.GoalDefinition g) {
      return goalOfGoalDefinition(g, horizonStartTimestamp, horizonEndTimestamp);
    } else if (goalSpecifier instanceof SchedulingDSL.GoalSpecifier.GoalAnd g) {
      var builder = new CompositeAndGoal.Builder();
      for (final var subGoalSpecifier : g.goals()) {
        builder = builder.and(goalOfGoalSpecifier(subGoalSpecifier,
                                                  horizonStartTimestamp,
                                                  horizonEndTimestamp));
      }
      return builder.build();
    } else if (goalSpecifier instanceof SchedulingDSL.GoalSpecifier.GoalOr g) {
      var builder = new OptionGoal.Builder();
      for (final var subGoalSpecifier : g.goals()) {
        builder = builder.or(goalOfGoalSpecifier(subGoalSpecifier,
                                                 horizonStartTimestamp,
                                                 horizonEndTimestamp));
      }
      return builder.build();
    } else {
      throw new Error("Unhandled variant of GoalSpecifier:" + goalSpecifier);
    }
  }

  private static Goal goalOfGoalDefinition(
      final SchedulingDSL.GoalSpecifier.GoalDefinition goalDefinition,
      final Timestamp horizonStartTimestamp,
      final Timestamp horizonEndTimestamp) {
    final var hor = new PlanningHorizon(
        Time.fromString(horizonStartTimestamp.toString()),
        Time.fromString(horizonEndTimestamp.toString())).getHor();
    return switch(goalDefinition.kind()) {
      case ActivityRecurrenceGoal -> new RecurrenceGoal.Builder()
          .forAllTimeIn(hor)
          .repeatingEvery(goalDefinition.interval())
          .thereExistsOne(makeActivityTemplate(goalDefinition))
          .build();
    };
  }

  private static ActivityCreationTemplate makeActivityTemplate(final SchedulingDSL.GoalSpecifier.GoalDefinition goalDefinition) {
    final var activityTemplate = goalDefinition.activityTemplate();
    var builder = new ActivityCreationTemplate.Builder()
        .ofType(new ActivityType(activityTemplate.activityType()));
    for (final var argument : activityTemplate.arguments().entrySet()) {
      builder = builder.withArgument(argument.getKey(), argument.getValue());
    }
    builder = builder.duration(Duration.ZERO);
    return builder.build();
  }
}
