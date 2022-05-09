package gov.nasa.jpl.aerie.scheduler.server.models;

import gov.nasa.jpl.aerie.json.Iso;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.json.ProductParsers;
import gov.nasa.jpl.aerie.json.SumParsers;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.List;
import java.util.Map;

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

  private static final JsonParser<ConstraintExpression> constraintExpressionP =
      SumParsers.sumP("kind", ConstraintExpression.class, List.of(
          SumParsers.variant("ActivityExpression", ConstraintExpression.ActivityExpression.class, activityExpressionP.map(Iso.of(
              ConstraintExpression.ActivityExpression::new,
              ConstraintExpression.ActivityExpression::expression))),
          SumParsers.variant("WindowsExpressionGreaterThan", ConstraintExpression.GreaterThan.class, greaterThanP)));

  private static final ProductParsers.JsonObjectParser<GoalSpecifier.CoexistenceGoalDefinition> coexistenceGoalDefinitionP =
      productP
          .field("activityTemplate", activityTemplateP)
          .field("forEach", constraintExpressionP)
          .map(Iso.of(
              untuple(GoalSpecifier.CoexistenceGoalDefinition::new),
              goalDefinition -> tuple(
                  goalDefinition.activityTemplate(),
                  goalDefinition.forEach())));

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
    record GoalAnd(List<GoalSpecifier> goals) implements GoalSpecifier {}
    record GoalOr(List<GoalSpecifier> goals) implements GoalSpecifier {}
  }

  public record LinearResource(String name) {}

  public record ActivityTemplate(String activityType, Map<String, SerializedValue> arguments) {}

  public record ActivityExpression(String type) {}

  public sealed interface ConstraintExpression {
    record ActivityExpression(SchedulingDSL.ActivityExpression expression) implements ConstraintExpression {}

    record GreaterThan(LinearResource resource, double value) implements ConstraintExpression {}
  }
}
