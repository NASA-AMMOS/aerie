package gov.nasa.jpl.aerie.scheduler.server.models;

import gov.nasa.jpl.aerie.json.Iso;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.json.ProductParsers;
import gov.nasa.jpl.aerie.json.SumParsers;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static gov.nasa.jpl.aerie.json.BasicParsers.intP;
import static gov.nasa.jpl.aerie.json.BasicParsers.doubleP;
import static gov.nasa.jpl.aerie.json.BasicParsers.listP;
import static gov.nasa.jpl.aerie.json.BasicParsers.longP;
import static gov.nasa.jpl.aerie.json.BasicParsers.mapP;
import static gov.nasa.jpl.aerie.json.BasicParsers.productP;
import static gov.nasa.jpl.aerie.json.BasicParsers.recursiveP;
import static gov.nasa.jpl.aerie.json.BasicParsers.stringP;
import static gov.nasa.jpl.aerie.json.Uncurry.tuple;
import static gov.nasa.jpl.aerie.json.Uncurry.untuple;
import static gov.nasa.jpl.aerie.scheduler.server.http.SerializedValueJsonParser.serializedValueP;

public class SchedulingDSL {

  private static final JsonParser<ActivityTemplate> activityTemplateP =
      productP
          .field("activityType", stringP)
          .field("args", mapP(serializedValueP))
          .map(Iso.of(
              untuple(ActivityTemplate::new),
              $ -> tuple($.activityType(), $.arguments())));


  private static final JsonParser<Duration> durationP =
      longP
      . map(Iso.of(
          microseconds -> Duration.of(microseconds, Duration.MICROSECONDS),
          duration -> duration.in(Duration.MICROSECONDS)));

  private static final JsonParser<ClosedOpenInterval> intervalP =
      productP
          .field("start", durationP)
          .field("end", durationP)
          .map(Iso.of(
              untuple(ClosedOpenInterval::new),
              $ -> tuple($.start(), $.end())));

  private static final JsonParser<CardinalitySpecification> cardinalitySpecificationJsonParser =
      productP
          .optionalField("duration", durationP)
          .optionalField("occurrence", intP)
          .map(Iso.of(
              untuple(CardinalitySpecification::new),
              $ -> tuple($.duration(), $.occurrence())));
  private static final ProductParsers.JsonObjectParser<GoalSpecifier.RecurrenceGoalDefinition> recurrenceGoalDefinitionP =
      productP
          .field("activityTemplate", activityTemplateP)
          .field("interval", durationP)
          .map(Iso.of(
              untuple(GoalSpecifier.RecurrenceGoalDefinition::new),
              goalDefinition -> tuple(
                  goalDefinition.activityTemplate(),
                  goalDefinition.interval())));

  private static final ProductParsers.JsonObjectParser<ActivityExpression> activityExpressionP =
      productP
          .field("type", stringP)
          .map(Iso.of(
              ActivityExpression::new,
              ActivityExpression::type));

  private static final JsonParser<LinearResource> linearResourceP =
      stringP
          .map(Iso.of(LinearResource::new, LinearResource::name));

  private static final ProductParsers.JsonObjectParser<ConstraintExpression.GreaterThan> greaterThanP =
      productP
          .field("left", linearResourceP)
          .field("right", doubleP)
          .map(Iso.of(
              untuple(ConstraintExpression.GreaterThan::new),
              $ -> tuple($.resource(), $.value())));

  private static final ProductParsers.JsonObjectParser<ConstraintExpression.LessThan> lessThanP =
      productP
          .field("left", linearResourceP)
          .field("right", doubleP)
          .map(Iso.of(
              untuple(ConstraintExpression.LessThan::new),
              $ -> tuple($.resource(), $.value())));

  private static final ProductParsers.JsonObjectParser<ConstraintExpression.EqualLinear> equalLinearP =
      productP
          .field("left", linearResourceP)
          .field("right", doubleP)
          .map(Iso.of(
              untuple(ConstraintExpression.EqualLinear::new),
              $ -> tuple($.resource(), $.value())));

  private static final ProductParsers.JsonObjectParser<ConstraintExpression.NotEqualLinear> notEqualLinearP =
      productP
          .field("left", linearResourceP)
          .field("right", doubleP)
          .map(Iso.of(
              untuple(ConstraintExpression.NotEqualLinear::new),
              $ -> tuple($.resource(), $.value())));

  private static final ProductParsers.JsonObjectParser<ConstraintExpression.Between> betweenP =
      productP
          .field("resource", linearResourceP)
          .field("lowerBound", doubleP)
          .field("upperBound", doubleP)
          .map(Iso.of(
              untuple(ConstraintExpression.Between::new),
              $ -> tuple($.resource(), $.lowerBound(), $.upperBound())));

  private static final ProductParsers.JsonObjectParser<ConstraintExpression.Transition> transitionP =
      productP
          .field("resource", linearResourceP)
          .field("from", serializedValueP)
          .field("to", serializedValueP)
          .map(Iso.of(
              untuple(ConstraintExpression.Transition::new),
              $ -> tuple($.resource(), $.from(), $.to())));

  private static ProductParsers.JsonObjectParser<ConstraintExpression.And> windowsAndF(final JsonParser<ConstraintExpression> constraintExpressionP) {
    return productP
        .field("windowsExpressions", listP(constraintExpressionP))
        .map(Iso.of(untuple(ConstraintExpression.And::new),
                    ConstraintExpression.And::expressions));
  }

  private static ProductParsers.JsonObjectParser<ConstraintExpression.Or> windowsOrF(final JsonParser<ConstraintExpression> constraintExpressionP) {
    return productP
        .field("windowsExpressions", listP(constraintExpressionP))
        .map(Iso.of(untuple(ConstraintExpression.Or::new),
                    ConstraintExpression.Or::expressions));
  }

  private static final JsonParser<ConstraintExpression> constraintExpressionP =
      recursiveP(self -> SumParsers.sumP("kind", ConstraintExpression.class, List.of(
          SumParsers.variant("ActivityExpression", ConstraintExpression.ActivityExpression.class, activityExpressionP.map(Iso.of(
              ConstraintExpression.ActivityExpression::new,
              ConstraintExpression.ActivityExpression::expression))),
          SumParsers.variant("WindowsExpressionGreaterThan", ConstraintExpression.GreaterThan.class, greaterThanP),
          SumParsers.variant("WindowsExpressionLessThan", ConstraintExpression.LessThan.class, lessThanP),
          SumParsers.variant("WindowsExpressionEqualLinear", ConstraintExpression.EqualLinear.class, equalLinearP),
          SumParsers.variant("WindowsExpressionNotEqualLinear", ConstraintExpression.NotEqualLinear.class, notEqualLinearP),
          SumParsers.variant("WindowsExpressionBetween", ConstraintExpression.Between.class, betweenP),
          SumParsers.variant("WindowsExpressionTransition", ConstraintExpression.Transition.class, transitionP),
          SumParsers.variant("WindowsExpressionAnd", ConstraintExpression.And.class, windowsAndF(self)),
          SumParsers.variant("WindowsExpressionOr", ConstraintExpression.Or.class, windowsOrF(self)))));

  private static final ProductParsers.JsonObjectParser<GoalSpecifier.CoexistenceGoalDefinition> coexistenceGoalDefinitionP =
      productP
          .field("activityTemplate", activityTemplateP)
          .field("forEach", constraintExpressionP)
          .map(Iso.of(
              untuple(GoalSpecifier.CoexistenceGoalDefinition::new),
              goalDefinition -> tuple(
                  goalDefinition.activityTemplate(),
                  goalDefinition.forEach())));

  private static final ProductParsers.JsonObjectParser<GoalSpecifier.CardinalityGoalDefinition> cardinalityGoalDefinitionP =
      productP
          .field("activityTemplate", activityTemplateP)
          .field("specification", cardinalitySpecificationJsonParser)
          .field("inPeriod", intervalP)
          .map(Iso.of(
              untuple(GoalSpecifier.CardinalityGoalDefinition::new),
              goalDefinition -> tuple(
                  goalDefinition.activityTemplate(),
                  goalDefinition.specification(),
                  goalDefinition.inPeriod())));

  private static ProductParsers.JsonObjectParser<GoalSpecifier.GoalAnd> goalAndF(final JsonParser<GoalSpecifier> goalSpecifierP) {
    return productP
        .field("goals", listP(goalSpecifierP))
        .map(Iso.of(untuple(GoalSpecifier.GoalAnd::new),
                    GoalSpecifier.GoalAnd::goals));
  }

  private static ProductParsers.JsonObjectParser<GoalSpecifier.GoalOr> goalOrF(final JsonParser<GoalSpecifier> goalSpecifierP) {
    return productP
        .field("goals", listP(goalSpecifierP))
        .map(Iso.of(untuple(GoalSpecifier.GoalOr::new),
                    GoalSpecifier.GoalOr::goals));
  }


  private static final JsonParser<GoalSpecifier> goalSpecifierP =
      recursiveP(self -> SumParsers.sumP("kind", GoalSpecifier.class, List.of(
          SumParsers.variant("ActivityRecurrenceGoal", GoalSpecifier.RecurrenceGoalDefinition.class, recurrenceGoalDefinitionP),
          SumParsers.variant("ActivityCoexistenceGoal", GoalSpecifier.CoexistenceGoalDefinition.class, coexistenceGoalDefinitionP),
          SumParsers.variant("ActivityCardinalityGoal", GoalSpecifier.CardinalityGoalDefinition.class, cardinalityGoalDefinitionP),
          SumParsers.variant("GoalAnd", GoalSpecifier.GoalAnd.class, goalAndF(self)),
          SumParsers.variant("GoalOr", GoalSpecifier.GoalOr.class, goalOrF(self))
      )));


  public static final JsonParser<GoalSpecifier> schedulingJsonP = goalSpecifierP;


  public sealed interface GoalSpecifier {
    record RecurrenceGoalDefinition(
        ActivityTemplate activityTemplate,
        Duration interval
    ) implements GoalSpecifier {}
    record CoexistenceGoalDefinition(
        ActivityTemplate activityTemplate,
        ConstraintExpression forEach
    ) implements GoalSpecifier {}
    record CardinalityGoalDefinition(
        ActivityTemplate activityTemplate,
        CardinalitySpecification specification,
        ClosedOpenInterval inPeriod
    ) implements GoalSpecifier {}
    record GoalAnd(List<GoalSpecifier> goals) implements GoalSpecifier {}
    record GoalOr(List<GoalSpecifier> goals) implements GoalSpecifier {}
  }

  public record LinearResource(String name) {}

  public record CardinalitySpecification(Optional<Duration> duration, Optional<Integer> occurrence){}
  public record ClosedOpenInterval(Duration start, Duration end){}
  public record ActivityTemplate(String activityType, Map<String, SerializedValue> arguments) {}

  public record ActivityExpression(String type) {}

  public sealed interface ConstraintExpression {
    record ActivityExpression(SchedulingDSL.ActivityExpression expression) implements ConstraintExpression {}

    record GreaterThan(LinearResource resource, double value) implements ConstraintExpression {}

    record LessThan(LinearResource resource, double value) implements ConstraintExpression {}

    record EqualLinear(LinearResource resource, double value) implements ConstraintExpression {}

    record NotEqualLinear(LinearResource resource, double value) implements ConstraintExpression {}

    record Between(LinearResource resource, double lowerBound, double upperBound) implements ConstraintExpression {}

    record Transition(LinearResource resource, SerializedValue from, SerializedValue to) implements ConstraintExpression {}

    record And(List<ConstraintExpression> expressions) implements ConstraintExpression {}

    record Or(List<ConstraintExpression> expressions) implements ConstraintExpression {}
  }
}
