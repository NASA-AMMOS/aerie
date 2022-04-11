package gov.nasa.jpl.aerie.scheduler.server.models;

import gov.nasa.jpl.aerie.json.Iso;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.json.ProductParsers;
import gov.nasa.jpl.aerie.json.SumParsers;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.List;
import java.util.Map;

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


  private static final JsonParser<Duration> durationP
      = longP
      . map(Iso.of(
          microseconds -> Duration.of(microseconds, Duration.MICROSECONDS),
          duration -> duration.in(Duration.MICROSECONDS)));

  private static final ProductParsers.JsonObjectParser<GoalSpecifier.RecurrenceGoalDefinition>
      recurrenceGoalDefinitionP =
      productP
          .field("activityTemplate", activityTemplateP)
          .field("interval", durationP)
          .map(Iso.of(
              untuple(GoalSpecifier.RecurrenceGoalDefinition::new),
              goalDefinition -> tuple(
                  goalDefinition.activityTemplate(),
                  goalDefinition.interval())));

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
          SumParsers.variant("GoalAnd", GoalSpecifier.GoalAnd.class, goalAndF(self)),
          SumParsers.variant("GoalOr", GoalSpecifier.GoalOr.class, goalOrF(self))
      )));


  public static final JsonParser<GoalSpecifier> schedulingJsonP = goalSpecifierP;


  public sealed interface GoalSpecifier {
    record RecurrenceGoalDefinition(
        ActivityTemplate activityTemplate,
        Duration interval
    ) implements GoalSpecifier {}
    record GoalAnd(List<GoalSpecifier> goals) implements GoalSpecifier {}
    record GoalOr(List<GoalSpecifier> goals) implements GoalSpecifier {}
  }


  public record ActivityTemplate(String activityType, Map<String, SerializedValue> arguments) {}
}
