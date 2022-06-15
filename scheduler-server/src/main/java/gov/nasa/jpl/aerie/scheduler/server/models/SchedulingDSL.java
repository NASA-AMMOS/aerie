package gov.nasa.jpl.aerie.scheduler.server.models;

import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.Expression;
import gov.nasa.jpl.aerie.json.Iso;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.json.ProductParsers;
import gov.nasa.jpl.aerie.json.SumParsers;
import gov.nasa.jpl.aerie.json.Unit;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.TimeUtility;
import gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions.TimeAnchor;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static gov.nasa.jpl.aerie.json.BasicParsers.boolP;
import static gov.nasa.jpl.aerie.json.BasicParsers.enumP;
import static gov.nasa.jpl.aerie.json.BasicParsers.intP;
import static gov.nasa.jpl.aerie.constraints.json.ConstraintParsers.windowsExpressionP;
import static gov.nasa.jpl.aerie.json.BasicParsers.chooseP;
import static gov.nasa.jpl.aerie.json.BasicParsers.listP;
import static gov.nasa.jpl.aerie.json.BasicParsers.literalP;
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

  private static final ProductParsers.JsonObjectParser<ConstraintExpression.ActivityExpression> activityExpressionP =
      productP
          .field("kind", literalP("ActivityExpression"))
          .field("type", stringP)
          .map(Iso.of(
              $ -> new ConstraintExpression.ActivityExpression($.getRight()),
              t -> Pair.of(Unit.UNIT, t.type())));

  private static final JsonParser<LinearResource> linearResourceP =
      stringP
          .map(Iso.of(LinearResource::new, LinearResource::name));

  private static final JsonParser<ConstraintExpression> constraintExpressionP =
      chooseP(
          activityExpressionP,
          windowsExpressionP.map(Iso.of(
              ConstraintExpression.WindowsExpression::new,
              ConstraintExpression.WindowsExpression::expression)));

  private static final JsonParser<ActivityTimingConstraint> activityTimingConstraintP =
      productP
          .field("windowProperty", enumP(TimeAnchor.class, Enum::name))
          .field("operator", enumP(TimeUtility.Operator.class, Enum::name))
          .field("operand", durationP)
          .field("singleton", boolP)
          .map(Iso.of(
              untuple(ActivityTimingConstraint::new),
              $ -> tuple($.windowProperty(), $.operator(), $.operand(), $.singleton())));

  private static final ProductParsers.JsonObjectParser<GoalSpecifier.CoexistenceGoalDefinition> coexistenceGoalDefinitionP =
      productP
          .field("activityTemplate", activityTemplateP)
          .field("forEach", constraintExpressionP)
          .optionalField("startConstraint", activityTimingConstraintP)
          .optionalField("endConstraint", activityTimingConstraintP)
          .map(Iso.of(
              untuple(GoalSpecifier.CoexistenceGoalDefinition::new),
              goalDefinition -> tuple(
                  goalDefinition.activityTemplate(),
                  goalDefinition.forEach(),
                  goalDefinition.startConstraint(),
                  goalDefinition.endConstraint())));

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
        ConstraintExpression forEach,
        Optional<ActivityTimingConstraint> startConstraint,
        Optional<ActivityTimingConstraint> endConstraint
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

  public sealed interface ConstraintExpression {
    record ActivityExpression(String type) implements ConstraintExpression {}

    record WindowsExpression(Expression<Windows> expression) implements ConstraintExpression {}
  }

  public record ActivityTimingConstraint(TimeAnchor windowProperty, TimeUtility.Operator operator, Duration operand, boolean singleton) {}
}
