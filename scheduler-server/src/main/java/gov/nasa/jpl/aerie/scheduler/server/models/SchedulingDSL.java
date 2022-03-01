package gov.nasa.jpl.aerie.scheduler.server.models;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.json.Iso;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.json.Unit;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.List;
import java.util.Map;

import static gov.nasa.jpl.aerie.json.BasicParsers.chooseP;
import static gov.nasa.jpl.aerie.json.BasicParsers.enumP;
import static gov.nasa.jpl.aerie.json.BasicParsers.intP;
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

  private static final JsonParser<WindowSetSpecifier.ConstraintOperatorEntirePlanWindow>
      constraintOperatorEntirePlanWindowP =
      productP
          .field("kind", literalP("ConstraintOperatorEntirePlanWindow"))
          .map(Iso.of($ -> new WindowSetSpecifier.ConstraintOperatorEntirePlanWindow(), $ -> Unit.UNIT));

  private static JsonParser<WindowSetSpecifier.WindowSetCombineOperatorIntersection>
  windowSetCombineOperatorIntersectionF(final JsonParser<WindowSetSpecifier> windowSetSpecifierP) {
    return productP
        .field("kind", literalP("WindowSetCombineOperatorIntersection"))
        .field("expressions", listP(windowSetSpecifierP))
        .map(Iso.of(
            untuple(($, expressions) -> new WindowSetSpecifier.WindowSetCombineOperatorIntersection(expressions)),
            (WindowSetSpecifier.WindowSetCombineOperatorIntersection $) -> tuple(Unit.UNIT, $.expressions())));
  }

  private static JsonParser<WindowSetSpecifier.WindowSetCombineOperatorUnion>
  windowSetCombineOperatorUnionF(final JsonParser<WindowSetSpecifier> windowSetSpecifierP) {
    return productP
        .field("kind", literalP("WindowSetCombineOperatorUnion"))
        .field("expressions", listP(windowSetSpecifierP))
        .map(Iso.of(
            untuple(($, expressions) -> new WindowSetSpecifier.WindowSetCombineOperatorUnion(expressions)),
            (WindowSetSpecifier.WindowSetCombineOperatorUnion $) -> tuple(Unit.UNIT, $.expressions())));
  }

  private static JsonParser<WindowSetSpecifier.WindowSetCombineOperatorDifference>
  windowSetCombineOperatorDifferenceF(final JsonParser<WindowSetSpecifier> windowSetSpecifierP) {
    return productP
        .field("kind", literalP("WindowSetCombineOperatorUnion"))
        .field("left", windowSetSpecifierP)
        .field("right", windowSetSpecifierP)
        .map(Iso.of(
            untuple(($, left, right) -> new WindowSetSpecifier.WindowSetCombineOperatorDifference(left, right)),
            (WindowSetSpecifier.WindowSetCombineOperatorDifference $) -> tuple(Unit.UNIT, $.left(), $.right())));
  }



  private static final JsonParser<WindowSetSpecifier> windowSetSpecifierP =
      recursiveP(self ->
                     chooseP(
                         constraintOperatorEntirePlanWindowP,
                         windowSetCombineOperatorIntersectionF(self),
                         windowSetCombineOperatorUnionF(self),
                         windowSetCombineOperatorDifferenceF(self)
                     ));

  private static final JsonParser<ActivityTemplate> activityTemplateP =
      productP
          .field("name", stringP)
          .field("activityType", stringP)
          .field("arguments", mapP(serializedValueP))
          .map(Iso.of(
              untuple(ActivityTemplate::new),
              $ -> tuple($.name(), $.activityType(), $.arguments())));


  public static final JsonParser<Duration> durationP
      = longP
      . map(Iso.of(
          microseconds -> Duration.of(microseconds, Duration.MICROSECONDS),
          duration -> duration.in(Duration.MICROSECONDS)));

  private static final JsonParser<GoalSpecifier.GoalDefinition> goalDefinitionP =
      productP
          .field("kind", enumP(GoalKinds.class, Enum::name))
          .field("windows", windowSetSpecifierP)
          .field("activityTemplate", activityTemplateP)
          .field("interval", durationP)
          .map(Iso.of(
              untuple(GoalSpecifier.GoalDefinition::new),
              goalDefinition -> tuple(
                  goalDefinition.kind(),
                  goalDefinition.windowSetSpecifier(),
                  goalDefinition.activityTemplate(),
                  goalDefinition.interval())));

  private static JsonParser<GoalSpecifier.GoalAnd> goalAndF(final JsonParser<GoalSpecifier> goalSpecifierP) {
    return productP
        .field("kind", literalP("GoalAnd"))
        .field("goals", listP(goalSpecifierP))
        .map(Iso.of(untuple(($, goals) -> new GoalSpecifier.GoalAnd(goals)),
                    $ -> tuple(Unit.UNIT, $.goals())));
  }

  private static JsonParser<GoalSpecifier.GoalOr> goalOrF(final JsonParser<GoalSpecifier> goalSpecifierP) {
    return productP
        .field("kind", literalP("GoalOr"))
        .field("goals", listP(goalSpecifierP))
        .map(Iso.of(untuple(($, goals) -> new GoalSpecifier.GoalOr(goals)),
                    $ -> tuple(Unit.UNIT, $.goals())));
  }


  public static final JsonParser<GoalSpecifier> goalSpecifierP =
      recursiveP(self -> chooseP(goalAndF(self), goalOrF(self), goalDefinitionP));


  public static final JsonParser<GoalSpecifier> schedulingJsonP = goalSpecifierP;


  public enum GoalKinds {
    ActivityRecurrenceGoal
  }

  public sealed interface GoalSpecifier {
    record GoalDefinition(
        GoalKinds kind,
        WindowSetSpecifier windowSetSpecifier,
        ActivityTemplate activityTemplate,
        Duration interval
    ) implements GoalSpecifier {
    }
    record GoalAnd(List<GoalSpecifier> goals) implements GoalSpecifier {}
    record GoalOr(List<GoalSpecifier> goals) implements GoalSpecifier {}
  }



  public sealed interface WindowSetSpecifier {
    record ConstraintOperatorEntirePlanWindow() implements WindowSetSpecifier {}
    record WindowSetCombineOperatorIntersection(List<WindowSetSpecifier> expressions) implements WindowSetSpecifier {}
    record WindowSetCombineOperatorUnion(List<WindowSetSpecifier> expressions) implements WindowSetSpecifier {}
    record WindowSetCombineOperatorDifference(WindowSetSpecifier left, WindowSetSpecifier right) implements WindowSetSpecifier {}
  }

  public record ActivityTemplate(String name, String activityType, Map<String, SerializedValue> arguments) {}
}
