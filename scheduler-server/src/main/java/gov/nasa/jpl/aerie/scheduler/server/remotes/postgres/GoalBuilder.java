package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

import gov.nasa.jpl.aerie.scheduler.ActivityCreationTemplate;
import gov.nasa.jpl.aerie.scheduler.ActivityType;
import gov.nasa.jpl.aerie.scheduler.CompositeAndGoal;
import gov.nasa.jpl.aerie.scheduler.Goal;
import gov.nasa.jpl.aerie.scheduler.OptionGoal;
import gov.nasa.jpl.aerie.scheduler.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.RecurrenceGoal;
import gov.nasa.jpl.aerie.scheduler.Time;
import gov.nasa.jpl.aerie.scheduler.server.models.SchedulingDSL;
import gov.nasa.jpl.aerie.scheduler.server.models.Timestamp;

import java.util.function.Function;

public class GoalBuilder {
  private GoalBuilder() {}

  public static Goal goalOfGoalSpecifier(
      final SchedulingDSL.GoalSpecifier goalSpecifier,
      final Timestamp horizonStartTimestamp,
      final Timestamp horizonEndTimestamp,
      final Function<String, ActivityType> lookupActivityType) {
    if (goalSpecifier instanceof SchedulingDSL.GoalSpecifier.GoalDefinition g) {
      return goalOfGoalDefinition(g, horizonStartTimestamp, horizonEndTimestamp, lookupActivityType);
    } else if (goalSpecifier instanceof SchedulingDSL.GoalSpecifier.GoalAnd g) {
      var builder = new CompositeAndGoal.Builder();
      for (final var subGoalSpecifier : g.goals()) {
        builder = builder.and(goalOfGoalSpecifier(subGoalSpecifier,
                                                  horizonStartTimestamp,
                                                  horizonEndTimestamp,
                                                  lookupActivityType));
      }
      return builder.build();
    } else if (goalSpecifier instanceof SchedulingDSL.GoalSpecifier.GoalOr g) {
      var builder = new OptionGoal.Builder();
      for (final var subGoalSpecifier : g.goals()) {
        builder = builder.or(goalOfGoalSpecifier(subGoalSpecifier,
                                                 horizonStartTimestamp,
                                                 horizonEndTimestamp,
                                                 lookupActivityType));
      }
      return builder.build();
    } else {
      throw new Error("Unhandled variant of GoalSpecifier:" + goalSpecifier);
    }
  }

  private static Goal goalOfGoalDefinition(
      final SchedulingDSL.GoalSpecifier.GoalDefinition goalDefinition,
      final Timestamp horizonStartTimestamp,
      final Timestamp horizonEndTimestamp,
      final Function<String, ActivityType> lookupActivityType) {
    final var hor = new PlanningHorizon(
        Time.fromString(horizonStartTimestamp.toString()),
        Time.fromString(horizonEndTimestamp.toString())).getHor();
    return switch(goalDefinition.kind()) {
      case ActivityRecurrenceGoal -> new RecurrenceGoal.Builder()
          .forAllTimeIn(hor)
          .repeatingEvery(goalDefinition.interval())
          .thereExistsOne(makeActivityTemplate(goalDefinition, lookupActivityType))
          .build();
    };
  }

  private static ActivityCreationTemplate makeActivityTemplate(
      final SchedulingDSL.GoalSpecifier.GoalDefinition goalDefinition,
      final Function<String, ActivityType> lookupActivityType) {
    final var activityTemplate = goalDefinition.activityTemplate();
    var builder = new ActivityCreationTemplate.Builder()
        .ofType(lookupActivityType.apply(activityTemplate.activityType()));
    for (final var argument : activityTemplate.arguments().entrySet()) {
      builder = builder.withArgument(argument.getKey(), argument.getValue());
    }
    return builder.build();
  }
}
