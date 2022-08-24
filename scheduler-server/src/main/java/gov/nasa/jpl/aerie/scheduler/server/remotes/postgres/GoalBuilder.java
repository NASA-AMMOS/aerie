package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.tree.WindowsWrapperExpression;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.DurationValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.DurationType;
import gov.nasa.jpl.aerie.scheduler.Range;
import gov.nasa.jpl.aerie.scheduler.constraints.TimeRangeExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityCreationTemplate;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions.TimeExpressionRelativeFixed;
import gov.nasa.jpl.aerie.scheduler.goals.CardinalityGoal;
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
import java.util.function.Function;
public class GoalBuilder {
  private GoalBuilder() {}
  public static Goal goalOfGoalSpecifier(
      final SchedulingDSL.GoalSpecifier goalSpecifier,
      final Timestamp horizonStartTimestamp,
      final Timestamp horizonEndTimestamp,
      final Function<String, ActivityType> lookupActivityType) {
    final var hor = new PlanningHorizon(
        horizonStartTimestamp.toInstant(),
        horizonEndTimestamp.toInstant()).getHor();
    if (goalSpecifier instanceof SchedulingDSL.GoalSpecifier.RecurrenceGoalDefinition g) {
      return new RecurrenceGoal.Builder()
          .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(hor, true)))
          .repeatingEvery(g.interval())
          .thereExistsOne(makeActivityTemplate(g.activityTemplate(), lookupActivityType))
          .build();
    } else if (goalSpecifier instanceof SchedulingDSL.GoalSpecifier.CoexistenceGoalDefinition g) {
      var builder = new CoexistenceGoal.Builder()
          .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(hor, true)))
          .forEach(timeRangeExpressionOfConstraintExpression(
              g.forEach(),
              lookupActivityType))
          .thereExistsOne(makeActivityTemplate(g.activityTemplate(), lookupActivityType));
      if (g.startConstraint().isPresent()) {
        final var startConstraint = g.startConstraint().get();
        final var timeExpression = new TimeExpressionRelativeFixed(
            startConstraint.windowProperty(),
            startConstraint.singleton()
        );
        timeExpression.addOperation(startConstraint.operator(), startConstraint.operand());
        builder.startsAt(timeExpression);
      }
      if (g.endConstraint().isPresent()) {
        final var startConstraint = g.endConstraint().get();
        final var timeExpression = new TimeExpressionRelativeFixed(
            startConstraint.windowProperty(),
            startConstraint.singleton()
        );
        timeExpression.addOperation(startConstraint.operator(), startConstraint.operand());
        builder.endsAt(timeExpression);
      }
      if (g.startConstraint().isEmpty() && g.endConstraint().isEmpty()) {
        throw new Error("Both start and end constraints were empty. This should have been disallowed at the type level.");
      }
      return builder.build();
    } else if (goalSpecifier instanceof SchedulingDSL.GoalSpecifier.GoalAnd g) {
      var builder = new CompositeAndGoal.Builder();
      for (final var subGoalSpecifier : g.goals()) {
        builder = builder.and(goalOfGoalSpecifier(subGoalSpecifier,
                                                  horizonStartTimestamp,
                                                  horizonEndTimestamp,
                                                  lookupActivityType));
      }
      builder.forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(hor, true)));
      return builder.build();
    } else if (goalSpecifier instanceof SchedulingDSL.GoalSpecifier.GoalOr g) {
      var builder = new OptionGoal.Builder();
      for (final var subGoalSpecifier : g.goals()) {
        builder = builder.or(goalOfGoalSpecifier(subGoalSpecifier,
                                                 horizonStartTimestamp,
                                                 horizonEndTimestamp,
                                                 lookupActivityType));
      }
      builder.forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(hor, true)));
      return builder.build();
    }

    else if (goalSpecifier instanceof SchedulingDSL.GoalSpecifier.GoalApplyWhen g) {
      var goal = goalOfGoalSpecifier(g.goal(), horizonStartTimestamp, horizonEndTimestamp, lookupActivityType);
      goal.setTemporalContext(g.windows());
      return goal;
    }

    else if(goalSpecifier instanceof SchedulingDSL.GoalSpecifier.CardinalityGoalDefinition g){
      final var builder = new CardinalityGoal.Builder()
          .thereExistsOne(makeActivityTemplate(g.activityTemplate(), lookupActivityType))
           .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(hor, true)));
      if(g.specification().duration().isPresent()){
        builder.duration(Interval.between(g.specification().duration().get(), Duration.MAX_VALUE));
      }
      if(g.specification().occurrence().isPresent()){
        builder.occurences(new Range<>(g.specification().occurrence().get(), Integer.MAX_VALUE));
      }
      return builder.build();
    } else {
      throw new Error("Unhandled variant of GoalSpecifier:" + goalSpecifier);
    }
  }
  private static TimeRangeExpression timeRangeExpressionOfConstraintExpression(
      final SchedulingDSL.ConstraintExpression constraintExpression,
      final Function<String, ActivityType> lookupActivityType) {
    if (constraintExpression instanceof SchedulingDSL.ConstraintExpression.ActivityExpression c) {
      return new TimeRangeExpression.Builder()
          .from(ActivityExpression.ofType(lookupActivityType.apply(c.type())))
          .build();
    } else if (constraintExpression instanceof SchedulingDSL.ConstraintExpression.WindowsExpression c){
      return new TimeRangeExpression.Builder()
          .from(c.expression())
          .build();
    } else {
      throw new UnexpectedSubtypeError(SchedulingDSL.ConstraintExpression.class, constraintExpression);
    }
  }
  private static ActivityCreationTemplate makeActivityTemplate(
      final SchedulingDSL.ActivityTemplate activityTemplate,
      final Function<String, ActivityType> lookupActivityType) {
    var builder = new ActivityCreationTemplate.Builder();
    final var type = lookupActivityType.apply(activityTemplate.activityType());
    if(type.getDurationType() instanceof DurationType.Controllable durationType){
      //detect duration parameter
      if(activityTemplate.arguments().containsKey(durationType.parameterName())){
        builder.duration(new DurationValueMapper().deserializeValue(activityTemplate.arguments().get(durationType.parameterName())).getSuccessOrThrow());
        activityTemplate.arguments().remove(durationType.parameterName());
      }
    }
    builder = builder.ofType(type);
    for (final var argument : activityTemplate.arguments().entrySet()) {
      builder = builder.withArgument(argument.getKey(), argument.getValue());
    }
    return builder.build();
  }
}
