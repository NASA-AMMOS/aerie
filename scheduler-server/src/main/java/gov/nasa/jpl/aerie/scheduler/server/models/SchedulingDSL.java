package gov.nasa.jpl.aerie.scheduler.server.models;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.Expression;
import gov.nasa.jpl.aerie.json.JsonObjectParser;
import gov.nasa.jpl.aerie.json.JsonParser;
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
          .map(
              untuple(ActivityTemplate::new),
              $ -> tuple($.activityType(), $.arguments()));

  private static final JsonParser<Duration> durationP =
      longP
          . map(
              microseconds -> Duration.of(microseconds, Duration.MICROSECONDS),
              duration -> duration.in(Duration.MICROSECONDS));

  private static final JsonParser<ClosedOpenInterval> intervalP =
      productP
          .field("start", durationP)
          .field("end", durationP)
          .map(
              untuple(ClosedOpenInterval::new),
              $ -> tuple($.start(), $.end()));

  private static final JsonParser<CardinalitySpecification> cardinalitySpecificationJsonParser =
      productP
          .optionalField("duration", durationP)
          .optionalField("occurrence", intP)
          .map(
              untuple(CardinalitySpecification::new),
              $ -> tuple($.duration(), $.occurrence()));

  private static final JsonObjectParser<GoalSpecifier.RecurrenceGoalDefinition> recurrenceGoalDefinitionP =
      productP
          .field("activityTemplate", activityTemplateP)
          .field("interval", durationP)
          .map(
              untuple(GoalSpecifier.RecurrenceGoalDefinition::new),
              goalDefinition -> tuple(
                  goalDefinition.activityTemplate(),
                  goalDefinition.interval()));

  private static final JsonObjectParser<ConstraintExpression.ActivityExpression> activityExpressionP =
      productP
          .field("kind", literalP("ActivityExpression"))
          .field("type", stringP)
          .map(
              $ -> new ConstraintExpression.ActivityExpression($.getRight()),
              t -> Pair.of(Unit.UNIT, t.type()));

  private static final JsonParser<LinearResource> linearResourceP =
      stringP
          .map(LinearResource::new, LinearResource::name);

  private static final JsonParser<ConstraintExpression> constraintExpressionP =
      chooseP(
          activityExpressionP,
          windowsExpressionP.map(
              ConstraintExpression.WindowsExpression::new,
              ConstraintExpression.WindowsExpression::expression));

  private static final JsonParser<ActivityTimingConstraint> activityTimingConstraintP =
      productP
          .field("windowProperty", enumP(TimeAnchor.class, Enum::name))
          .field("operator", enumP(TimeUtility.Operator.class, Enum::name))
          .field("operand", durationP)
          .field("singleton", boolP)
          .map(
              untuple(ActivityTimingConstraint::new),
              $ -> tuple($.windowProperty(), $.operator(), $.operand(), $.singleton()));

  private static final JsonObjectParser<GoalSpecifier.CoexistenceGoalDefinition> coexistenceGoalDefinitionP =
      productP
          .field("activityTemplate", activityTemplateP)
          .field("forEach", constraintExpressionP)
          .optionalField("startConstraint", activityTimingConstraintP)
          .optionalField("endConstraint", activityTimingConstraintP)
          .map(
              untuple(GoalSpecifier.CoexistenceGoalDefinition::new),
              goalDefinition -> tuple(
                  goalDefinition.activityTemplate(),
                  goalDefinition.forEach(),
                  goalDefinition.startConstraint(),
                  goalDefinition.endConstraint()));

  private static final JsonObjectParser<GoalSpecifier.CardinalityGoalDefinition> cardinalityGoalDefinitionP =
      productP
          .field("activityTemplate", activityTemplateP)
          .field("specification", cardinalitySpecificationJsonParser)
          .map(
              untuple(GoalSpecifier.CardinalityGoalDefinition::new),
              goalDefinition -> tuple(
                  goalDefinition.activityTemplate(),
                  goalDefinition.specification()));

  private static JsonObjectParser<GoalSpecifier.GoalAnd> goalAndF(final JsonParser<GoalSpecifier> goalSpecifierP) {
    return productP
        .field("goals", listP(goalSpecifierP))
        .map(untuple(GoalSpecifier.GoalAnd::new),
            GoalSpecifier.GoalAnd::goals);
  }

  private static JsonObjectParser<GoalSpecifier.GoalOr> goalOrF(final JsonParser<GoalSpecifier> goalSpecifierP) {
    return productP
        .field("goals", listP(goalSpecifierP))
        .map(untuple(GoalSpecifier.GoalOr::new),
            GoalSpecifier.GoalOr::goals);
  }

  private static JsonObjectParser<GoalSpecifier.GoalApplyWhen> goalApplyWhenF(final JsonParser<GoalSpecifier> goalSpecifierP) {
    return productP
        .field("goal", goalSpecifierP)
        .field("window", windowsExpressionP)
        .map(untuple(GoalSpecifier.GoalApplyWhen::new),
            goalDefinition -> tuple(
                goalDefinition.goal(),
                goalDefinition.windows()));
  }


  private static final JsonParser<GoalSpecifier> goalSpecifierP =
      recursiveP(self -> SumParsers.sumP("kind", GoalSpecifier.class, List.of(
          SumParsers.variant("ActivityRecurrenceGoal", GoalSpecifier.RecurrenceGoalDefinition.class, recurrenceGoalDefinitionP),
          SumParsers.variant("ActivityCoexistenceGoal", GoalSpecifier.CoexistenceGoalDefinition.class, coexistenceGoalDefinitionP),
          SumParsers.variant("ActivityCardinalityGoal", GoalSpecifier.CardinalityGoalDefinition.class, cardinalityGoalDefinitionP),
          SumParsers.variant("GoalAnd", GoalSpecifier.GoalAnd.class, goalAndF(self)),
          SumParsers.variant("GoalOr", GoalSpecifier.GoalOr.class, goalOrF(self)),
          SumParsers.variant("ApplyWhen", GoalSpecifier.GoalApplyWhen.class, goalApplyWhenF(self))
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
        CardinalitySpecification specification
    ) implements GoalSpecifier {}
    record GoalAnd(List<GoalSpecifier> goals) implements GoalSpecifier {}
    record GoalOr(List<GoalSpecifier> goals) implements GoalSpecifier {}
    record GoalApplyWhen(
        GoalSpecifier goal,
        Expression<Windows> windows
    ) implements GoalSpecifier {}
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
