package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.And;
import gov.nasa.jpl.aerie.constraints.tree.DiscreteResource;
import gov.nasa.jpl.aerie.constraints.tree.Equal;
import gov.nasa.jpl.aerie.constraints.tree.Expression;
import gov.nasa.jpl.aerie.constraints.tree.GreaterThan;
import gov.nasa.jpl.aerie.constraints.tree.LessThan;
import gov.nasa.jpl.aerie.constraints.tree.NotEqual;
import gov.nasa.jpl.aerie.constraints.tree.Or;
import gov.nasa.jpl.aerie.constraints.tree.RealResource;
import gov.nasa.jpl.aerie.constraints.tree.RealValue;
import gov.nasa.jpl.aerie.constraints.tree.Transition;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.During;
import gov.nasa.jpl.aerie.constraints.tree.ForEachActivity;
import gov.nasa.jpl.aerie.constraints.tree.ViolationsOf;
import gov.nasa.jpl.aerie.constraints.tree.WindowsOf;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.DurationValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.DurationType;
import gov.nasa.jpl.aerie.scheduler.Range;
import gov.nasa.jpl.aerie.scheduler.constraints.TimeRangeExpression;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.constraints.TimeRangeExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityCreationTemplate;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions.TimeAnchor;
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
      final Function<String, ActivityType> lookupActivityType) {
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
              lookupActivityType))
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
    } else if(goalSpecifier instanceof SchedulingDSL.GoalSpecifier.CardinalityGoalDefinition g){
      final var builder = new CardinalityGoal.Builder()
          .thereExistsOne(makeActivityTemplate(g.activityTemplate(), lookupActivityType))
          .forAllTimeIn(hor)
          .inPeriod(new TimeRangeExpression.Builder()
                           .from(new Windows(Window.betweenClosedOpen(g.inPeriod().start(), g.inPeriod().end())))
                           .build());
      if(g.specification().duration().isPresent()){
        builder.duration(Window.between(g.specification().duration().get(), Duration.MAX_VALUE));
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
          .from(ActivityExpression.ofType(lookupActivityType.apply(c.expression().type())))
          .build();
    } else {
      return new TimeRangeExpression.Builder()
          .from(expressionOfConstraintExpression(constraintExpression))
          .build();
    }
  }

  private static Expression<Windows> expressionOfConstraintExpression(
      final SchedulingDSL.ConstraintExpression constraintExpression) {
    if (constraintExpression instanceof SchedulingDSL.ConstraintExpression.ActivityExpression c) {
      throw new NotImplementedException("Nested ActivityExpressions are not yet supported");
    } else if (constraintExpression instanceof SchedulingDSL.ConstraintExpression.GreaterThan c) {
      return new GreaterThan(new RealResource(c.resource().name()), new RealValue(c.value()));
    } else if (constraintExpression instanceof SchedulingDSL.ConstraintExpression.LessThan c) {
      return new LessThan(new RealResource(c.resource().name()), new RealValue(c.value()));
    } else if (constraintExpression instanceof SchedulingDSL.ConstraintExpression.EqualLinear c) {
      return new Equal<>(new RealResource(c.resource().name()), new RealValue(c.value()));
    } else if (constraintExpression instanceof SchedulingDSL.ConstraintExpression.NotEqualLinear c) {
      return new NotEqual<>(new RealResource(c.resource().name()), new RealValue(c.value()));
    } else if (constraintExpression instanceof SchedulingDSL.ConstraintExpression.Between c) {
      return new And(new GreaterThan(new RealResource(c.resource().name()), new RealValue(c.lowerBound())),
                     new LessThan(new RealResource(c.resource().name()), new RealValue(c.upperBound())));
    } else if (constraintExpression instanceof SchedulingDSL.ConstraintExpression.Transition c) {
      return new Transition(new DiscreteResource(c.resource().name()), c.from(), c.to());
    } else if (constraintExpression instanceof SchedulingDSL.ConstraintExpression.And c) {
      return new And(c.expressions()
                      .stream()
                      .map(GoalBuilder::expressionOfConstraintExpression)
                      .toList());
    } else if (constraintExpression instanceof SchedulingDSL.ConstraintExpression.Or c) {
      return new Or(c.expressions()
                     .stream()
                     .map(GoalBuilder::expressionOfConstraintExpression)
                     .toList());
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
