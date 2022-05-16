package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.constraints.TimeRangeExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityCreationTemplate;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.resources.ExternalState;
import gov.nasa.jpl.aerie.scheduler.constraints.resources.StateConstraintExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.resources.StateConstraintExpressionDisjunction;
import gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions.TimeAnchor;
import gov.nasa.jpl.aerie.scheduler.goals.CoexistenceGoal;
import gov.nasa.jpl.aerie.scheduler.goals.CompositeAndGoal;
import gov.nasa.jpl.aerie.scheduler.goals.Goal;
import gov.nasa.jpl.aerie.scheduler.goals.OptionGoal;
import gov.nasa.jpl.aerie.scheduler.goals.RecurrenceGoal;
import gov.nasa.jpl.aerie.scheduler.model.ActivityType;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.server.models.SchedulingDSL;
import gov.nasa.jpl.aerie.scheduler.server.models.Timestamp;
import gov.nasa.jpl.aerie.scheduler.server.services.UnexpectedSubtypeError;
import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class GoalBuilder {
  private GoalBuilder() {}

  public static Goal goalOfGoalSpecifier(
      final SchedulingDSL.GoalSpecifier goalSpecifier,
      final Timestamp horizonStartTimestamp,
      final Timestamp horizonEndTimestamp,
      final Function<String, ActivityType> lookupActivityType,
      final Function<String, ExternalState> lookupResource) {
    final var hor = new PlanningHorizon(
        horizonStartTimestamp.toInstant(),
        horizonEndTimestamp.toInstant()).getHor();
    if (goalSpecifier instanceof SchedulingDSL.GoalSpecifier.RecurrenceGoalDefinition g) {
      return new RecurrenceGoal.Builder()
          .forAllTimeIn(hor)
          .repeatingEvery(g.interval())
          .thereExistsOne(makeActivityTemplate(g.activityTemplate(), lookupActivityType))
          .build();
    } else if (goalSpecifier instanceof SchedulingDSL.GoalSpecifier.CoexistenceGoalDefinition g) {
      var builder = new CoexistenceGoal.Builder()
          .forAllTimeIn(hor)
          .forEach(timeRangeExpressionOfConstraintExpression(
              g.forEach(),
              lookupActivityType,
              lookupResource))
          .thereExistsOne(makeActivityTemplate(g.activityTemplate(), lookupActivityType));
      // TODO: This if statement should be removed when the DSL supports time expressions.
      if (g.forEach() instanceof SchedulingDSL.ConstraintExpression.ActivityExpression) {
        builder = builder.startsAt(TimeAnchor.END);
      } else {
        builder = builder.startsAt(TimeAnchor.START);
      }
      return builder.build();
    } else if (goalSpecifier instanceof SchedulingDSL.GoalSpecifier.GoalAnd g) {
      var builder = new CompositeAndGoal.Builder();
      for (final var subGoalSpecifier : g.goals()) {
        builder = builder.and(goalOfGoalSpecifier(subGoalSpecifier,
                                                  horizonStartTimestamp,
                                                  horizonEndTimestamp,
                                                  lookupActivityType,
                                                  lookupResource));
      }
      return builder.build();
    } else if (goalSpecifier instanceof SchedulingDSL.GoalSpecifier.GoalOr g) {
      var builder = new OptionGoal.Builder();
      for (final var subGoalSpecifier : g.goals()) {
        builder = builder.or(goalOfGoalSpecifier(subGoalSpecifier,
                                                 horizonStartTimestamp,
                                                 horizonEndTimestamp,
                                                 lookupActivityType,
                                                 lookupResource));
      }
      return builder.build();
    } else {
      throw new Error("Unhandled variant of GoalSpecifier:" + goalSpecifier);
    }
  }

  private static TimeRangeExpression timeRangeExpressionOfConstraintExpression(
      final SchedulingDSL.ConstraintExpression constraintExpression,
      final Function<String, ActivityType> lookupActivityType,
      final Function<String, ExternalState> lookupResource) {
    if (constraintExpression instanceof SchedulingDSL.ConstraintExpression.ActivityExpression c) {
      return new TimeRangeExpression.Builder()
          .from(ActivityExpression.ofType(lookupActivityType.apply(c.expression().type())))
          .build();
    } else if (constraintExpression instanceof SchedulingDSL.ConstraintExpression.GreaterThan c) {
      return new TimeRangeExpression.Builder()
          .from(new StateConstraintExpression.Builder()
                    .above(lookupResource.apply(c.resource().name()), SerializedValue.of(c.value()))
                    .build())
          .build();
    } else if (constraintExpression instanceof SchedulingDSL.ConstraintExpression.LessThan c) {
      return new TimeRangeExpression.Builder()
          .from(new StateConstraintExpression.Builder()
                    .lessThan(lookupResource.apply(c.resource().name()), SerializedValue.of(c.value()))
                    .build())
          .build();
    } else if (constraintExpression instanceof SchedulingDSL.ConstraintExpression.EqualLinear c) {
      return new TimeRangeExpression.Builder()
          .from(new StateConstraintExpression.Builder()
                    .equal(lookupResource.apply(c.resource().name()), SerializedValue.of(c.value()))
                    .build())
          .build();
    } else if (constraintExpression instanceof SchedulingDSL.ConstraintExpression.NotEqualLinear c) {
      return new TimeRangeExpression.Builder()
          .from(new StateConstraintExpression.Builder()
                    .notEqual(lookupResource.apply(c.resource().name()), SerializedValue.of(c.value()))
                    .build())
          .build();
    } else if (constraintExpression instanceof SchedulingDSL.ConstraintExpression.Between c) {
      return new TimeRangeExpression.Builder()
          .from(new StateConstraintExpression.Builder()
                    .between(
                        lookupResource.apply(c.resource().name()),
                        SerializedValue.of(c.lowerBound()),
                        SerializedValue.of(c.upperBound()))
                    .build())
          .build();
    } else if (constraintExpression instanceof SchedulingDSL.ConstraintExpression.Transition c) {
      return new TimeRangeExpression.Builder()
          .from(new StateConstraintExpression.Builder()
                    .transition(
                        lookupResource.apply(c.resource().name()),
                        List.of(c.from()),
                        List.of(c.to()))
                    .build())
          .build();
    } else if (constraintExpression instanceof SchedulingDSL.ConstraintExpression.And c) {
      final var timeRangeExpressions = new ArrayList<TimeRangeExpression>(c.expressions().size());
      for (final var expression : c.expressions()) {
        timeRangeExpressions.add(timeRangeExpressionOfConstraintExpression(expression, lookupActivityType, lookupResource));
      }
      var builder = new TimeRangeExpression.Builder();
      for (final var timeRangeExpression : timeRangeExpressions) {
        builder = builder.from(timeRangeExpression);
      }
      return builder.build();
    } else if (constraintExpression instanceof SchedulingDSL.ConstraintExpression.Or c) {
      final var stateConstraintExpressions = new ArrayList<StateConstraintExpression>(c.expressions().size());
      for (final var expression : c.expressions()) {
        // TODO: Allow ActivityExpressions in Or expressions
        if (expression instanceof SchedulingDSL.ConstraintExpression.ActivityExpression) throw new NotImplementedException("ActivityExpressions not supported in Or yet");
        stateConstraintExpressions.addAll(timeRangeExpressionOfConstraintExpression(expression, lookupActivityType, lookupResource).getStateConstraints());
      }
      return new TimeRangeExpression.Builder()
          .from(new StateConstraintExpressionDisjunction(stateConstraintExpressions))
          .build();
    } else {
      throw new UnexpectedSubtypeError(SchedulingDSL.ConstraintExpression.class, constraintExpression);
    }
  }

  private static ActivityCreationTemplate makeActivityTemplate(
      final SchedulingDSL.ActivityTemplate activityTemplate,
      final Function<String, ActivityType> lookupActivityType) {
    var builder = new ActivityCreationTemplate.Builder()
        .ofType(lookupActivityType.apply(activityTemplate.activityType()));
    for (final var argument : activityTemplate.arguments().entrySet()) {
      builder = builder.withArgument(argument.getKey(), argument.getValue());
    }
    return builder.build();
  }
}
