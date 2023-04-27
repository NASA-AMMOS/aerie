package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Spans;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.ActivitySpan;
import gov.nasa.jpl.aerie.constraints.tree.Expression;
import gov.nasa.jpl.aerie.constraints.tree.ForEachActivitySpans;
import gov.nasa.jpl.aerie.constraints.tree.SpansFromWindows;
import gov.nasa.jpl.aerie.constraints.tree.WindowsWrapperExpression;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.DurationType;
import gov.nasa.jpl.aerie.scheduler.Range;
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
      final Function<String, ActivityType> lookupActivityType,
      final boolean simulateAfter) {
    final var hor =
        new PlanningHorizon(horizonStartTimestamp.toInstant(), horizonEndTimestamp.toInstant())
            .getHor();
    if (goalSpecifier instanceof SchedulingDSL.GoalSpecifier.RecurrenceGoalDefinition g) {
      final var builder =
          new RecurrenceGoal.Builder()
              .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(hor, true)))
              .repeatingEvery(g.interval())
              .shouldRollbackIfUnsatisfied(g.shouldRollbackIfUnsatisfied())
              .thereExistsOne(makeActivityTemplate(g.activityTemplate(), lookupActivityType))
              .simulateAfter(simulateAfter);
      if (g.activityFinder().isPresent()) {
        builder.match(buildActivityExpression(g.activityFinder().get(), lookupActivityType));
      }
      return builder.build();
    } else if (goalSpecifier instanceof SchedulingDSL.GoalSpecifier.CoexistenceGoalDefinition g) {
      var builder =
          new CoexistenceGoal.Builder()
              .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(hor, true)))
              .forEach(spansOfConstraintExpression(g.forEach()))
              .thereExistsOne(makeActivityTemplate(g.activityTemplate(), lookupActivityType))
              .simulateAfter(simulateAfter)
              .shouldRollbackIfUnsatisfied(g.shouldRollbackIfUnsatisfied())
              .aliasForAnchors(g.alias());
      if (g.startConstraint().isPresent()) {
        final var startConstraint = g.startConstraint().get();
        final var timeExpression =
            new TimeExpressionRelativeFixed(
                startConstraint.windowProperty(), startConstraint.singleton());
        timeExpression.addOperation(startConstraint.operator(), startConstraint.operand());
        builder.startsAt(timeExpression);
      }
      if (g.endConstraint().isPresent()) {
        final var startConstraint = g.endConstraint().get();
        final var timeExpression =
            new TimeExpressionRelativeFixed(
                startConstraint.windowProperty(), startConstraint.singleton());
        timeExpression.addOperation(startConstraint.operator(), startConstraint.operand());
        builder.endsAt(timeExpression);
      }
      if (g.startConstraint().isEmpty() && g.endConstraint().isEmpty()) {
        throw new Error(
            "Both start and end constraints were empty. This should have been disallowed at the"
                + " type level.");
      }
      if (g.activityFinder().isPresent()) {
        builder.match(buildActivityExpression(g.activityFinder().get(), lookupActivityType));
      }
      return builder.build();
    } else if (goalSpecifier instanceof SchedulingDSL.GoalSpecifier.GoalAnd g) {
      var builder = new CompositeAndGoal.Builder();
      for (final var subGoalSpecifier : g.goals()) {
        builder =
            builder.and(
                goalOfGoalSpecifier(
                    subGoalSpecifier,
                    horizonStartTimestamp,
                    horizonEndTimestamp,
                    lookupActivityType,
                    simulateAfter));
      }
      builder.simulateAfter(simulateAfter);
      builder.forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(hor, true)));
      builder.shouldRollbackIfUnsatisfied(g.shouldRollbackIfUnsatisfied());
      return builder.build();
    } else if (goalSpecifier instanceof SchedulingDSL.GoalSpecifier.GoalOr g) {
      var builder = new OptionGoal.Builder();
      for (final var subGoalSpecifier : g.goals()) {
        builder =
            builder.or(
                goalOfGoalSpecifier(
                    subGoalSpecifier,
                    horizonStartTimestamp,
                    horizonEndTimestamp,
                    lookupActivityType,
                    simulateAfter));
      }
      builder.simulateAfter(simulateAfter);
      builder.forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(hor, true)));
      builder.shouldRollbackIfUnsatisfied(g.shouldRollbackIfUnsatisfied());
      return builder.build();
    } else if (goalSpecifier instanceof SchedulingDSL.GoalSpecifier.GoalApplyWhen g) {
      var goal =
          goalOfGoalSpecifier(
              g.goal(),
              horizonStartTimestamp,
              horizonEndTimestamp,
              lookupActivityType,
              simulateAfter);
      goal.setTemporalContext(g.windows());
      return goal;
    } else if (goalSpecifier instanceof SchedulingDSL.GoalSpecifier.CardinalityGoalDefinition g) {
      final var builder =
          new CardinalityGoal.Builder()
              .thereExistsOne(makeActivityTemplate(g.activityTemplate(), lookupActivityType))
              .simulateAfter(simulateAfter)
              .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(hor, true)))
              .shouldRollbackIfUnsatisfied(g.shouldRollbackIfUnsatisfied());
      if (g.specification().duration().isPresent()) {
        builder.duration(Interval.between(g.specification().duration().get(), Duration.MAX_VALUE));
      }
      if (g.specification().occurrence().isPresent()) {
        builder.occurences(new Range<>(g.specification().occurrence().get(), Integer.MAX_VALUE));
      }
      if (g.activityFinder().isPresent()) {
        builder.match(buildActivityExpression(g.activityFinder().get(), lookupActivityType));
      }
      return builder.build();
    } else {
      throw new Error("Unhandled variant of GoalSpecifier:" + goalSpecifier);
    }
  }

  private static ActivityExpression buildActivityExpression(
      SchedulingDSL.ConstraintExpression.ActivityExpression activityExpr,
      final Function<String, ActivityType> lookupActivityType) {
    final var builder =
        new ActivityExpression.Builder().ofType(lookupActivityType.apply(activityExpr.type()));
    if (activityExpr.arguments().isPresent()) {
      activityExpr.arguments().get().fields().forEach(builder::withArgument);
    }
    return builder.build();
  }

  private static Expression<Spans> spansOfConstraintExpression(
      final SchedulingDSL.ConstraintExpression constraintExpression) {
    if (constraintExpression instanceof SchedulingDSL.ConstraintExpression.ActivityExpression c) {
      return new ForEachActivitySpans(
          c.type(), "alias" + c.type(), new ActivitySpan("alias" + c.type()));
    } else if (constraintExpression
        instanceof SchedulingDSL.ConstraintExpression.WindowsExpression c) {
      return new SpansFromWindows(c.expression());
    } else {
      throw new UnexpectedSubtypeError(
          SchedulingDSL.ConstraintExpression.class, constraintExpression);
    }
  }

  private static ActivityCreationTemplate makeActivityTemplate(
      final SchedulingDSL.ActivityTemplate activityTemplate,
      final Function<String, ActivityType> lookupActivityType) {
    var builder = new ActivityCreationTemplate.Builder();
    final var type = lookupActivityType.apply(activityTemplate.activityType());
    if (type.getDurationType() instanceof DurationType.Controllable durationType) {
      // detect duration parameter
      if (activityTemplate.arguments().fields().containsKey(durationType.parameterName())) {
        final var argument =
            activityTemplate.arguments().fields().get(durationType.parameterName());
        if (argument != null) {
          builder.duration(argument.expression);
          activityTemplate.arguments().fields().remove(durationType.parameterName());
        } else {
          // nothing, other cases will be handled by below section
        }
      }
    }
    builder = builder.ofType(type);
    for (final var argument : activityTemplate.arguments().fields().entrySet()) {
      builder.withArgument(argument.getKey(), argument.getValue());
    }
    return builder.build();
  }
}
