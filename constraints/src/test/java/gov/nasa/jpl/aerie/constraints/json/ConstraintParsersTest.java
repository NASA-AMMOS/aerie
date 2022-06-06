package gov.nasa.jpl.aerie.constraints.json;

import gov.nasa.jpl.aerie.constraints.tree.All;
import gov.nasa.jpl.aerie.constraints.tree.Changed;
import gov.nasa.jpl.aerie.constraints.tree.DiscreteParameter;
import gov.nasa.jpl.aerie.constraints.tree.DiscreteResource;
import gov.nasa.jpl.aerie.constraints.tree.DiscreteValue;
import gov.nasa.jpl.aerie.constraints.tree.ActivityWindow;
import gov.nasa.jpl.aerie.constraints.tree.EndOf;
import gov.nasa.jpl.aerie.constraints.tree.Equal;
import gov.nasa.jpl.aerie.constraints.tree.ForEachActivity;
import gov.nasa.jpl.aerie.constraints.tree.GreaterThan;
import gov.nasa.jpl.aerie.constraints.tree.GreaterThanOrEqual;
import gov.nasa.jpl.aerie.constraints.tree.LessThan;
import gov.nasa.jpl.aerie.constraints.tree.LessThanOrEqual;
import gov.nasa.jpl.aerie.constraints.tree.Not;
import gov.nasa.jpl.aerie.constraints.tree.NotEqual;
import gov.nasa.jpl.aerie.constraints.tree.Any;
import gov.nasa.jpl.aerie.constraints.tree.Plus;
import gov.nasa.jpl.aerie.constraints.tree.ProfileExpression;
import gov.nasa.jpl.aerie.constraints.tree.Rate;
import gov.nasa.jpl.aerie.constraints.tree.RealParameter;
import gov.nasa.jpl.aerie.constraints.tree.RealResource;
import gov.nasa.jpl.aerie.constraints.tree.RealValue;
import gov.nasa.jpl.aerie.constraints.tree.StartOf;
import gov.nasa.jpl.aerie.constraints.tree.Times;
import gov.nasa.jpl.aerie.constraints.tree.Transition;
import gov.nasa.jpl.aerie.constraints.tree.ViolationsOf;
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
  public void testParseChanged() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "ProfileChanged")
        .add("expression", Json
            .createObjectBuilder()
            .add("kind", "DiscreteProfileValue")
            .add("value", false))
        .build();
    final var result = windowsExpressionP.parse(json).getSuccessOrThrow();

    final var expected =
        new Changed<>(
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
  public void testParseNot() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "WindowsExpressionNot")
        .add("expression", Json
            .createObjectBuilder()
            .add("kind", "WindowsExpressionActivityWindow")
            .add("alias", "A"))
        .build();

    final var result = windowsExpressionP.parse(json).getSuccessOrThrow();

    final var expected =
        new Not(
            new ActivityWindow("A"));

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
                                          .add("kind", "WindowsExpressionNot")
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
                             .add("kind", "WindowsExpressionNot")
                             .add("expression", Json
                                 .createObjectBuilder()
                                 .add("kind", "WindowsExpressionActivityWindow")
                                 .add("alias", "A")))
                    .add(Json
                             .createObjectBuilder()
                             .add("kind", "WindowsExpressionNot")
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
                        new Not(
                            new LessThan(
                                new Times(
                                    new RealResource("ResC"),
                                    2),
                                new RealParameter("A", "a"))),
                        new Equal<>(
                            new DiscreteValue(SerializedValue.of(false)),
                            new DiscreteParameter("B", "b"))),
                    new Not(new ActivityWindow("A")),
                    new Not(new ActivityWindow("B"))))));

    assertEquivalent(expected, result);
  }
}
