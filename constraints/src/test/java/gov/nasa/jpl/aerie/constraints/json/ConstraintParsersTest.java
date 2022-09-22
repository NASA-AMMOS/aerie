package gov.nasa.jpl.aerie.constraints.json;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.tree.All;
import gov.nasa.jpl.aerie.constraints.tree.Changes;
import gov.nasa.jpl.aerie.constraints.tree.DiscreteParameter;
import gov.nasa.jpl.aerie.constraints.tree.DiscreteResource;
import gov.nasa.jpl.aerie.constraints.tree.DiscreteValue;
import gov.nasa.jpl.aerie.constraints.tree.ActivityWindow;
import gov.nasa.jpl.aerie.constraints.tree.EndOf;
import gov.nasa.jpl.aerie.constraints.tree.Ends;
import gov.nasa.jpl.aerie.constraints.tree.Equal;
import gov.nasa.jpl.aerie.constraints.tree.ForEachActivity;
import gov.nasa.jpl.aerie.constraints.tree.GreaterThan;
import gov.nasa.jpl.aerie.constraints.tree.GreaterThanOrEqual;
import gov.nasa.jpl.aerie.constraints.tree.LessThan;
import gov.nasa.jpl.aerie.constraints.tree.LessThanOrEqual;
import gov.nasa.jpl.aerie.constraints.tree.Invert;
import gov.nasa.jpl.aerie.constraints.tree.NotEqual;
import gov.nasa.jpl.aerie.constraints.tree.Any;
import gov.nasa.jpl.aerie.constraints.tree.Plus;
import gov.nasa.jpl.aerie.constraints.tree.ProfileExpression;
import gov.nasa.jpl.aerie.constraints.tree.Rate;
import gov.nasa.jpl.aerie.constraints.tree.RealParameter;
import gov.nasa.jpl.aerie.constraints.tree.RealResource;
import gov.nasa.jpl.aerie.constraints.tree.RealValue;
import gov.nasa.jpl.aerie.constraints.tree.Split;
import gov.nasa.jpl.aerie.constraints.tree.SpansFromWindows;
import gov.nasa.jpl.aerie.constraints.tree.StartOf;
import gov.nasa.jpl.aerie.constraints.tree.Starts;
import gov.nasa.jpl.aerie.constraints.tree.Times;
import gov.nasa.jpl.aerie.constraints.tree.Transition;
import gov.nasa.jpl.aerie.constraints.tree.ViolationsOf;
import gov.nasa.jpl.aerie.constraints.tree.WindowsFromSpans;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.junit.jupiter.api.Test;

import javax.json.Json;

import static gov.nasa.jpl.aerie.constraints.Assertions.assertEquivalent;
import static gov.nasa.jpl.aerie.constraints.json.ConstraintParsers.*;

public final class ConstraintParsersTest {
  @Test
  public void testParseDiscreteValue() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "DiscreteProfileValue")
        .add("value", false)
        .build();
    final var result = discreteProfileExprP.parse(json).getSuccessOrThrow();

    final var expected =
        new DiscreteValue(SerializedValue.of(false));

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseChanges() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "ProfileChanges")
        .add("expression", Json
            .createObjectBuilder()
            .add("kind", "DiscreteProfileValue")
            .add("value", false))
        .build();
    final var result = windowsExpressionP.parse(json).getSuccessOrThrow();

    final var expected =
        new Changes<>(
            new ProfileExpression<>(
                new DiscreteValue(SerializedValue.of(false))));

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseDiscreteResource() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "DiscreteProfileResource")
        .add("name", "ResA")
        .build();
    final var result = discreteResourceP.parse(json).getSuccessOrThrow();

    final var expected =
        new DiscreteResource("ResA");

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseTransition() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "DiscreteProfileTransition")
        .add("profile", Json
            .createObjectBuilder()
            .add("kind", "DiscreteProfileResource")
            .add("name", "ResA"))
        .add("from", "old")
        .add("to", "new")
        .build();
    final var result = windowsExpressionP.parse(json).getSuccessOrThrow();

    final var expected =
        new Transition(
            new DiscreteResource("ResA"),
            SerializedValue.of("old"),
            SerializedValue.of("new"));

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseRealParameter() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "RealProfileParameter")
        .add("alias", "act")
        .add("name", "pJones")
        .build();
    final var result = linearProfileExprP.parse(json).getSuccessOrThrow();

    final var expected = new RealParameter("act", "pJones");

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseRealValue() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "RealProfileValue")
        .add("value", 3.4)
        .build();
    final var result = linearProfileExprP.parse(json).getSuccessOrThrow();

    final var expected = new RealValue(3.4);

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseRealResource() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "RealProfileResource")
        .add("name", "ResA")
        .build();
    final var result = linearProfileExprP.parse(json).getSuccessOrThrow();

    final var expected = new RealResource("ResA");

    assertEquivalent(expected, result);
  }

  @Test
  public void testParsePlus() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "RealProfilePlus")
        .add("left", Json
            .createObjectBuilder()
            .add("kind", "RealProfileResource")
            .add("name", "ResA"))
        .add("right", Json
            .createObjectBuilder()
            .add("kind", "RealProfileResource")
            .add("name", "ResB"))
        .build();
    final var result = linearProfileExprP.parse(json).getSuccessOrThrow();

    final var expected =
        new Plus(
            new RealResource("ResA"),
            new RealResource("ResB"));

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseTimes() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "RealProfileTimes")
        .add("profile", Json
            .createObjectBuilder()
            .add("kind", "RealProfileResource")
            .add("name", "ResA"))
        .add("multiplier", 2.7)
        .build();
    final var result = linearProfileExprP.parse(json).getSuccessOrThrow();

    final var expected =
        new Times(
            new RealResource("ResA"),
            2.7);

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseRate() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "RealProfileRate")
        .add("profile", Json
            .createObjectBuilder()
            .add("kind", "RealProfileResource")
            .add("name", "ResA"))
        .build();
    final var result = linearProfileExprP.parse(json).getSuccessOrThrow();

    final var expected =
        new Rate(
            new RealResource("ResA"));

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseDuring() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "WindowsExpressionActivityWindow")
        .add("alias", "TEST")
        .build();
    final var result = windowsExpressionP.parse(json).getSuccessOrThrow();

    final var expected = new ActivityWindow("TEST");

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseStartOf() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "WindowsExpressionStartOf")
        .add("alias", "TEST")
        .build();
    final var result = windowsExpressionP.parse(json).getSuccessOrThrow();

    final var expected = new StartOf("TEST");

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseEndOf() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "WindowsExpressionEndOf")
        .add("alias", "TEST")
        .build();
    final var result = windowsExpressionP.parse(json).getSuccessOrThrow();

    final var expected = new EndOf("TEST");

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseParameter() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "DiscreteProfileParameter")
        .add("alias", "TEST")
        .add("name", "paramesan")
        .build();
    final var result = discreteProfileExprP.parse(json).getSuccessOrThrow();

    final var expected = new DiscreteParameter("TEST", "paramesan");

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseEqual() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "ExpressionEqual")
        .add("left", Json
            .createObjectBuilder()
            .add("kind", "RealProfileResource")
            .add("name", "ResA"))
        .add("right", Json
            .createObjectBuilder()
            .add("kind", "RealProfileResource")
            .add("name", "ResB"))
        .build();
    final var result = windowsExpressionP.parse(json).getSuccessOrThrow();

    final var expected =
        new Equal<>(
            new RealResource("ResA"),
            new RealResource("ResB"));

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseNotEqual() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "ExpressionNotEqual")
        .add("left", Json
            .createObjectBuilder()
            .add("kind", "RealProfileResource")
            .add("name", "ResA"))
        .add("right", Json
            .createObjectBuilder()
            .add("kind", "RealProfileResource")
            .add("name", "ResB"))
        .build();
    final var result = windowsExpressionP.parse(json).getSuccessOrThrow();

    final var expected =
        new NotEqual<>(
            new RealResource("ResA"),
            new RealResource("ResB"));

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseLessThan() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "RealProfileLessThan")
        .add("left", Json
            .createObjectBuilder()
            .add("kind", "RealProfileResource")
            .add("name", "ResA"))
        .add("right", Json
            .createObjectBuilder()
            .add("kind", "RealProfileResource")
            .add("name", "ResB"))
        .build();
    final var result = windowsExpressionP.parse(json).getSuccessOrThrow();

    final var expected =
        new LessThan(
            new RealResource("ResA"),
            new RealResource("ResB"));

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseLessThanOrEqual() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "RealProfileLessThanOrEqual")
        .add("left", Json
            .createObjectBuilder()
            .add("kind", "RealProfileResource")
            .add("name", "ResA"))
        .add("right", Json
            .createObjectBuilder()
            .add("kind", "RealProfileResource")
            .add("name", "ResB"))
        .build();
    final var result = windowsExpressionP.parse(json).getSuccessOrThrow();

    final var expected =
        new LessThanOrEqual(
            new RealResource("ResA"),
            new RealResource("ResB"));

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseGreaterThanOrEqual() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "RealProfileGreaterThanOrEqual")
        .add("left", Json
            .createObjectBuilder()
            .add("kind", "RealProfileResource")
            .add("name", "ResA"))
        .add("right", Json
            .createObjectBuilder()
            .add("kind", "RealProfileResource")
            .add("name", "ResB"))
        .build();
    final var result = windowsExpressionP.parse(json).getSuccessOrThrow();

    final var expected =
        new GreaterThanOrEqual(
            new RealResource("ResA"),
            new RealResource("ResB"));

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseGreaterThan() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "RealProfileGreaterThan")
        .add("left", Json
            .createObjectBuilder()
            .add("kind", "RealProfileResource")
            .add("name", "ResA"))
        .add("right", Json
            .createObjectBuilder()
            .add("kind", "RealProfileResource")
            .add("name", "ResB"))
        .build();
    final var result = windowsExpressionP.parse(json).getSuccessOrThrow();

    final var expected =
        new GreaterThan(
            new RealResource("ResA"),
            new RealResource("ResB"));

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseAll() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "WindowsExpressionAll")
        .add("expressions", Json
            .createArrayBuilder()
            .add(Json
                     .createObjectBuilder()
                     .add("kind", "WindowsExpressionActivityWindow")
                     .add("alias", "A"))
            .add(Json
                     .createObjectBuilder()
                     .add("kind", "WindowsExpressionActivityWindow")
                     .add("alias", "B")))
        .build();

    final var result = windowsExpressionP.parse(json).getSuccessOrThrow();

    final var expected =
        new All(
            new ActivityWindow("A"),
            new ActivityWindow("B"));

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseAny() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "WindowsExpressionAny")
        .add("expressions", Json
            .createArrayBuilder()
            .add(Json
                     .createObjectBuilder()
                     .add("kind", "WindowsExpressionActivityWindow")
                     .add("alias", "A"))
            .add(Json
                     .createObjectBuilder()
                     .add("kind", "WindowsExpressionActivityWindow")
                     .add("alias", "B")))
        .build();

    final var result = windowsExpressionP.parse(json).getSuccessOrThrow();

    final var expected =
        new Any(
            new ActivityWindow("A"),
            new ActivityWindow("B"));

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseInvert() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "WindowsExpressionInvert")
        .add("expression", Json
            .createObjectBuilder()
            .add("kind", "WindowsExpressionActivityWindow")
            .add("alias", "A"))
        .build();

    final var result = windowsExpressionP.parse(json).getSuccessOrThrow();

    final var expected =
        new Invert(
            new ActivityWindow("A"));

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseStarts() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "IntervalsExpressionStarts")
        .add("expression", Json
            .createObjectBuilder()
            .add("kind", "WindowsExpressionActivityWindow")
            .add("alias", "A"))
        .build();

    final var result = windowsExpressionP.parse(json).getSuccessOrThrow();

    final var expected =
        new Starts<>(
            new ActivityWindow("A"));

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseEnds() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "IntervalsExpressionEnds")
        .add("expression", Json
            .createObjectBuilder()
            .add("kind", "WindowsExpressionActivityWindow")
            .add("alias", "A"))
        .build();

    final var result = windowsExpressionP.parse(json).getSuccessOrThrow();

    final var expected =
        new Ends<>(
            new ActivityWindow("A"));

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseSplitWindows() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "SpansExpressionSplit")
        .add("intervals", Json
            .createObjectBuilder()
            .add("kind", "WindowsExpressionActivityWindow")
            .add("alias", "A"))
        .add("numberOfSubIntervals", 3)
        .add("internalStartInclusivity", "Exclusive")
        .add("internalEndInclusivity", "Exclusive")
        .build();

    final var result = spansExpressionP.parse(json).getSuccessOrThrow();

    final var expected =
        new Split<>(
            new ActivityWindow("A"),
            3,
            Interval.Inclusivity.Exclusive,
            Interval.Inclusivity.Exclusive
        );

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseSplitSpans() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "SpansExpressionSplit")
        .add("intervals", Json.createObjectBuilder()
            .add("kind", "SpansExpressionFromWindows")
            .add("windowsExpression", Json
              .createObjectBuilder()
              .add("kind", "WindowsExpressionActivityWindow")
              .add("alias", "A"))
        )
        .add("numberOfSubIntervals", 3)
        .add("internalStartInclusivity", "Inclusive")
        .add("internalEndInclusivity", "Exclusive")
        .build();

    final var result = spansExpressionP.parse(json).getSuccessOrThrow();

    final var expected =
        new Split<>(
            new SpansFromWindows(
                new ActivityWindow("A")
            ),
            3,
            Interval.Inclusivity.Inclusive,
            Interval.Inclusivity.Exclusive
        );

    assertEquivalent(expected, result);
  }

  @Test
  public void testForEachActivity() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "ForEachActivity")
        .add("activityType", "TypeA")
        .add("alias", "A")
        .add("expression", Json
            .createObjectBuilder()
            .add("kind", "WindowsExpressionActivityWindow")
            .add("alias", "A"))
        .build();
    final var result = constraintP.parse(json).getSuccessOrThrow();

    final var expected =
        new ForEachActivity(
            "TypeA",
            "A",
            new ViolationsOf(
                new ActivityWindow("A")));

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseMassiveExpression() {
    var json = Json
        .createObjectBuilder()
        .add("kind", "ForEachActivity")
        .add("activityType", "TypeA")
        .add("alias", "A")
        .add("expression", Json
            .createObjectBuilder()
            .add("kind", "ForEachActivity")
            .add("activityType", "TypeB")
            .add("alias", "B")
            .add("expression", Json
                .createObjectBuilder()
                .add("kind", "WindowsExpressionAny")
                .add("expressions", Json
                    .createArrayBuilder()
                    .add(Json
                             .createObjectBuilder()
                             .add("kind", "WindowsExpressionAny")
                             .add("expressions", Json
                                 .createArrayBuilder()
                                 .add(Json
                                          .createObjectBuilder()
                                          .add("kind", "WindowsExpressionInvert")
                                          .add("expression", Json
                                              .createObjectBuilder()
                                              .add("kind", "RealProfileLessThan")
                                              .add("left", Json
                                                  .createObjectBuilder()
                                                  .add("kind", "RealProfileTimes")
                                                  .add("profile", Json
                                                      .createObjectBuilder()
                                                      .add("kind", "RealProfileResource")
                                                      .add("name", "ResC"))
                                                  .add("multiplier", 2.0))
                                              .add("right", Json
                                                  .createObjectBuilder()
                                                  .add("kind", "RealProfileParameter")
                                                  .add("alias", "A")
                                                  .add("name", "a"))))
                                 .add(Json
                                          .createObjectBuilder()
                                          .add("kind", "ExpressionEqual")
                                          .add("left", Json
                                              .createObjectBuilder()
                                              .add("kind", "DiscreteProfileValue")
                                              .add("value", false))
                                          .add("right", Json
                                              .createObjectBuilder()
                                              .add("kind", "DiscreteProfileParameter")
                                              .add("alias", "B")
                                              .add("name", "b")))))
                    .add(Json
                             .createObjectBuilder()
                             .add("kind", "WindowsExpressionInvert")
                             .add("expression", Json
                                 .createObjectBuilder()
                                 .add("kind", "WindowsExpressionActivityWindow")
                                 .add("alias", "A")))
                    .add(Json
                             .createObjectBuilder()
                             .add("kind", "WindowsExpressionInvert")
                             .add("expression", Json
                                 .createObjectBuilder()
                                 .add("kind", "WindowsExpressionActivityWindow")
                                 .add("alias", "B"))))))
        .build();
    final var result = constraintP.parse(json).getSuccessOrThrow();

    final var expected = new ForEachActivity(
        "TypeA",
        "A",
        new ForEachActivity(
            "TypeB",
            "B",
            new ViolationsOf(
                new Any(
                    new Any(
                        new Invert(
                            new LessThan(
                                new Times(
                                    new RealResource("ResC"),
                                    2),
                                new RealParameter("A", "a"))),
                        new Equal<>(
                            new DiscreteValue(SerializedValue.of(false)),
                            new DiscreteParameter("B", "b"))),
                    new Invert(new ActivityWindow("A")),
                    new Invert(new ActivityWindow("B"))))));

    assertEquivalent(expected, result);
  }

  // I know its excessive, I just wanted to be sure :)
  @Test
  public void testDeepMutualRecursion() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "WindowsExpressionFromSpans")
        .add("spansExpression", Json
            .createObjectBuilder()
            .add("kind", "SpansExpressionFromWindows")
            .add("windowsExpression", Json
                .createObjectBuilder()
                .add("kind", "WindowsExpressionFromSpans")
                .add("spansExpression", Json
                    .createObjectBuilder()
                    .add("kind", "SpansExpressionFromWindows")
                    .add("windowsExpression", Json
                        .createObjectBuilder()
                        .add("kind", "WindowsExpressionFromSpans")
                        .add("spansExpression", Json
                            .createObjectBuilder()
                            .add("kind", "SpansExpressionFromWindows")
                            .add("windowsExpression", Json
                                .createObjectBuilder()
                                .add("kind", "WindowsExpressionFromSpans")
                                .add("spansExpression", Json
                                    .createObjectBuilder()
                                    .add("kind", "SpansExpressionFromWindows")
                                    .add("windowsExpression", Json
                                        .createObjectBuilder()
                                        .add("kind", "WindowsExpressionFromSpans")
                                        .add("spansExpression", Json
                                            .createObjectBuilder()
                                            .add("kind", "SpansExpressionFromWindows")
                                            .add("windowsExpression", Json
                                                .createObjectBuilder()
                                                .add("kind", "WindowsExpressionFromSpans")
                                                .add("spansExpression", Json
                                                    .createObjectBuilder()
                                                    .add("kind", "SpansExpressionFromWindows")
                                                    .add("windowsExpression", Json
                                                        .createObjectBuilder()
                                                        .add("kind", "WindowsExpressionActivityWindow")
                                                        .add("alias", "TEST")))))))))))))
        .build();
    final var result = windowsExpressionP.parse(json).getSuccessOrThrow();

    final var expected = new WindowsFromSpans(
        new SpansFromWindows(
            new WindowsFromSpans(
                new SpansFromWindows(
                    new WindowsFromSpans(
                        new SpansFromWindows(
                            new WindowsFromSpans(
                                new SpansFromWindows(
                                    new WindowsFromSpans(
                                        new SpansFromWindows(
                                            new WindowsFromSpans(
                                                new SpansFromWindows(
                                                    new ActivityWindow("TEST")
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    );

    assertEquivalent(expected, result);
  }
}
