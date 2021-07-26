package gov.nasa.jpl.aerie.constraints.json;

import gov.nasa.jpl.aerie.constraints.tree.And;
import gov.nasa.jpl.aerie.constraints.tree.InstanceCardinality;
import gov.nasa.jpl.aerie.constraints.tree.RealParameter;
import gov.nasa.jpl.aerie.constraints.tree.Changed;
import gov.nasa.jpl.aerie.constraints.tree.DiscreteResource;
import gov.nasa.jpl.aerie.constraints.tree.DiscreteValue;
import gov.nasa.jpl.aerie.constraints.tree.During;
import gov.nasa.jpl.aerie.constraints.tree.EndOf;
import gov.nasa.jpl.aerie.constraints.tree.Equal;
import gov.nasa.jpl.aerie.constraints.tree.ForEachActivity;
import gov.nasa.jpl.aerie.constraints.tree.GreaterThan;
import gov.nasa.jpl.aerie.constraints.tree.GreaterThanOrEqual;
import gov.nasa.jpl.aerie.constraints.tree.LessThan;
import gov.nasa.jpl.aerie.constraints.tree.LessThanOrEqual;
import gov.nasa.jpl.aerie.constraints.tree.Not;
import gov.nasa.jpl.aerie.constraints.tree.NotEqual;
import gov.nasa.jpl.aerie.constraints.tree.Or;
import gov.nasa.jpl.aerie.constraints.tree.DiscreteParameter;
import gov.nasa.jpl.aerie.constraints.tree.Plus;
import gov.nasa.jpl.aerie.constraints.tree.ProfileExpression;
import gov.nasa.jpl.aerie.constraints.tree.Rate;
import gov.nasa.jpl.aerie.constraints.tree.RealResource;
import gov.nasa.jpl.aerie.constraints.tree.RealValue;
import gov.nasa.jpl.aerie.constraints.tree.StartOf;
import gov.nasa.jpl.aerie.constraints.tree.Times;
import gov.nasa.jpl.aerie.constraints.tree.Transition;
import gov.nasa.jpl.aerie.constraints.tree.ViolationsOf;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import org.junit.Test;

import javax.json.Json;

import static gov.nasa.jpl.aerie.constraints.Assertions.assertEquivalent;
import static gov.nasa.jpl.aerie.constraints.json.ConstraintParsers.discreteProfileExpressionP;
import static gov.nasa.jpl.aerie.constraints.json.ConstraintParsers.linearProfileExpressionP;
import static gov.nasa.jpl.aerie.constraints.json.ConstraintParsers.violationListExpressionP;
import static gov.nasa.jpl.aerie.constraints.json.ConstraintParsers.windowsExpressionP;

public final class ConstraintParsersTest {
  @Test
  public void testParseDiscreteValue() {
    final var json = Json
        .createObjectBuilder()
        .add("type", "DiscreteValue")
        .add("value", false)
        .build();
    final var result = discreteProfileExpressionP.parse(json).getSuccessOrThrow();

    final var expected =
        new DiscreteValue(SerializedValue.of(false));

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseChanged() {
    final var json = Json
        .createObjectBuilder()
        .add("type", "Changed")
        .add("expression", Json
            .createObjectBuilder()
            .add("type", "DiscreteValue")
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
        .add("type", "DiscreteResource")
        .add("name", "ResA")
        .build();
    final var result = discreteProfileExpressionP.parse(json).getSuccessOrThrow();

    final var expected =
        new DiscreteResource("ResA");

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseTransition() {
    final var json = Json
        .createObjectBuilder()
        .add("type", "Transition")
        .add("profile", Json
            .createObjectBuilder()
            .add("type", "DiscreteResource")
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
        .add("type", "RealParameter")
        .add("alias", "act")
        .add("name", "pJones")
        .build();
    final var result = linearProfileExpressionP.parse(json).getSuccessOrThrow();

    final var expected = new RealParameter("act", "pJones");

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseRealValue() {
    final var json = Json
        .createObjectBuilder()
        .add("type", "RealValue")
        .add("value", 3.4)
        .build();
    final var result = linearProfileExpressionP.parse(json).getSuccessOrThrow();

    final var expected = new RealValue(3.4);

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseRealResource() {
    final var json = Json
        .createObjectBuilder()
        .add("type", "RealResource")
        .add("name", "ResA")
        .build();
    final var result = linearProfileExpressionP.parse(json).getSuccessOrThrow();

    final var expected = new RealResource("ResA");

    assertEquivalent(expected, result);
  }

  @Test
  public void testParsePlus() {
    final var json = Json
        .createObjectBuilder()
        .add("type", "Plus")
        .add("left", Json
            .createObjectBuilder()
            .add("type", "RealResource")
            .add("name", "ResA"))
        .add("right", Json
            .createObjectBuilder()
            .add("type", "RealResource")
            .add("name", "ResB"))
        .build();
    final var result = linearProfileExpressionP.parse(json).getSuccessOrThrow();

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
        .add("type", "Times")
        .add("profile", Json
            .createObjectBuilder()
            .add("type", "RealResource")
            .add("name", "ResA"))
        .add("multiplier", 2.7)
        .build();
    final var result = linearProfileExpressionP.parse(json).getSuccessOrThrow();

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
        .add("type", "Rate")
        .add("profile", Json
            .createObjectBuilder()
            .add("type", "RealResource")
            .add("name", "ResA"))
        .build();
    final var result = linearProfileExpressionP.parse(json).getSuccessOrThrow();

    final var expected =
        new Rate(
            new RealResource("ResA"));

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseDuring() {
    final var json = Json
        .createObjectBuilder()
        .add("type", "During")
        .add("alias", "TEST")
        .build();
    final var result = windowsExpressionP.parse(json).getSuccessOrThrow();

    final var expected = new During("TEST");

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseStartOf() {
    final var json = Json
        .createObjectBuilder()
        .add("type", "StartOf")
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
        .add("type", "EndOf")
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
        .add("type", "DiscreteParameter")
        .add("alias", "TEST")
        .add("name", "paramesan")
        .build();
    final var result = discreteProfileExpressionP.parse(json).getSuccessOrThrow();

    final var expected = new DiscreteParameter("TEST", "paramesan");

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseEqual() {
    final var json = Json
        .createObjectBuilder()
        .add("type", "Equal")
        .add("left", Json
            .createObjectBuilder()
            .add("type", "RealResource")
            .add("name", "ResA"))
        .add("right", Json
            .createObjectBuilder()
            .add("type", "RealResource")
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
        .add("type", "NotEqual")
        .add("left", Json
            .createObjectBuilder()
            .add("type", "RealResource")
            .add("name", "ResA"))
        .add("right", Json
            .createObjectBuilder()
            .add("type", "RealResource")
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
        .add("type", "LessThan")
        .add("left", Json
            .createObjectBuilder()
            .add("type", "RealResource")
            .add("name", "ResA"))
        .add("right", Json
            .createObjectBuilder()
            .add("type", "RealResource")
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
        .add("type", "LessThanOrEqual")
        .add("left", Json
            .createObjectBuilder()
            .add("type", "RealResource")
            .add("name", "ResA"))
        .add("right", Json
            .createObjectBuilder()
            .add("type", "RealResource")
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
        .add("type", "GreaterThanOrEqual")
        .add("left", Json
            .createObjectBuilder()
            .add("type", "RealResource")
            .add("name", "ResA"))
        .add("right", Json
            .createObjectBuilder()
            .add("type", "RealResource")
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
        .add("type", "GreaterThan")
        .add("left", Json
            .createObjectBuilder()
            .add("type", "RealResource")
            .add("name", "ResA"))
        .add("right", Json
            .createObjectBuilder()
            .add("type", "RealResource")
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
  public void testParseAnd() {
    final var json = Json
        .createObjectBuilder()
        .add("type", "And")
        .add("expressions", Json
            .createArrayBuilder()
            .add(Json
                     .createObjectBuilder()
                     .add("type", "During")
                     .add("alias", "A"))
            .add(Json
                     .createObjectBuilder()
                     .add("type", "During")
                     .add("alias", "B")))
        .build();

    final var result = windowsExpressionP.parse(json).getSuccessOrThrow();

    final var expected =
        new And(
            new During("A"),
            new During("B"));

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseOr() {
    final var json = Json
        .createObjectBuilder()
        .add("type", "Or")
        .add("expressions", Json
            .createArrayBuilder()
            .add(Json
                     .createObjectBuilder()
                     .add("type", "During")
                     .add("alias", "A"))
            .add(Json
                     .createObjectBuilder()
                     .add("type", "During")
                     .add("alias", "B")))
        .build();

    final var result = windowsExpressionP.parse(json).getSuccessOrThrow();

    final var expected =
        new Or(
            new During("A"),
            new During("B"));

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseNot() {
    final var json = Json
        .createObjectBuilder()
        .add("type", "Not")
        .add("expression", Json
            .createObjectBuilder()
            .add("type", "During")
            .add("alias", "A"))
        .build();

    final var result = windowsExpressionP.parse(json).getSuccessOrThrow();

    final var expected =
        new Not(
            new During("A"));

    assertEquivalent(expected, result);
  }

  @Test
  public void testForEachActivity() {
    final var json = Json
        .createObjectBuilder()
        .add("type", "ForEachActivity")
        .add("activityType", "TypeA")
        .add("alias", "A")
        .add("expression", Json
            .createObjectBuilder()
            .add("type", "During")
            .add("alias", "A"))
        .build();
    final var result = violationListExpressionP.parse(json).getSuccessOrThrow();

    final var expected =
        new ForEachActivity(
            "TypeA",
            "A",
            new ViolationsOf(
                new During("A")));

    assertEquivalent(expected, result);
  }

  @Test
  public void testInstanceCardinality() {
    final var json = Json
        .createObjectBuilder()
        .add("type", "InstanceCardinality")
        .add("activityType", "TypeA")
        .add("minimum", 0)
        .add("maximum", 1)
        .build();

    final var result = violationListExpressionP.parse(json).getSuccessOrThrow();

    final var expected = new InstanceCardinality("TypeA", 0, 1);

    assertEquivalent(expected, result);

  }

  @Test
  public void testParseMassiveExpression() {
    var json = Json
        .createObjectBuilder()
        .add("type", "ForEachActivity")
        .add("activityType", "TypeA")
        .add("alias", "A")
        .add("expression", Json
            .createObjectBuilder()
            .add("type", "ForEachActivity")
            .add("activityType", "TypeB")
            .add("alias", "B")
            .add("expression", Json
                .createObjectBuilder()
                .add("type", "Or")
                .add("expressions", Json
                    .createArrayBuilder()
                    .add(Json
                             .createObjectBuilder()
                             .add("type", "Or")
                             .add("expressions", Json
                                 .createArrayBuilder()
                                 .add(Json
                                          .createObjectBuilder()
                                          .add("type", "Not")
                                          .add("expression", Json
                                              .createObjectBuilder()
                                              .add("type", "LessThan")
                                              .add("left", Json
                                                  .createObjectBuilder()
                                                  .add("type", "Times")
                                                  .add("profile", Json
                                                      .createObjectBuilder()
                                                      .add("type", "RealResource")
                                                      .add("name", "ResC"))
                                                  .add("multiplier", 2.0))
                                              .add("right", Json
                                                  .createObjectBuilder()
                                                  .add("type", "RealParameter")
                                                  .add("alias", "A")
                                                  .add("name", "a"))))
                                 .add(Json
                                          .createObjectBuilder()
                                          .add("type", "Equal")
                                          .add("left", Json
                                              .createObjectBuilder()
                                              .add("type", "DiscreteValue")
                                              .add("value", false))
                                          .add("right", Json
                                              .createObjectBuilder()
                                              .add("type", "DiscreteParameter")
                                              .add("alias", "B")
                                              .add("name", "b")))))
                    .add(Json
                             .createObjectBuilder()
                             .add("type", "Not")
                             .add("expression", Json
                                 .createObjectBuilder()
                                 .add("type", "During")
                                 .add("alias", "A")))
                    .add(Json
                             .createObjectBuilder()
                             .add("type", "Not")
                             .add("expression", Json
                                 .createObjectBuilder()
                                 .add("type", "During")
                                 .add("alias", "B"))))))
        .build();
    final var result = violationListExpressionP.parse(json).getSuccessOrThrow();

    final var expected = new ForEachActivity(
        "TypeA",
        "A",
        new ForEachActivity(
            "TypeB",
            "B",
            new ViolationsOf(
                new Or(
                    new Or(
                        new Not(
                            new LessThan(
                                new Times(
                                    new RealResource("ResC"),
                                    2),
                                new RealParameter("A", "a"))),
                        new Equal<>(
                            new DiscreteValue(SerializedValue.of(false)),
                            new DiscreteParameter("B", "b"))),
                    new Not(new During("A")),
                    new Not(new During("B"))))));

    assertEquivalent(expected, result);
  }

  @Test
  public void testForbiddenActivityOverlap() {
    final var json = Json
        .createObjectBuilder()
        .add("type", "ForbiddenActivityOverlap")
        .add("activityType1", "A")
        .add("activityType2", "B")
        .build();
    final var result = violationListExpressionP.parse(json).getSuccessOrThrow();

    final var expected = new ForEachActivity(
        "A",
        "act1",
        new ForEachActivity(
            "B",
            "act2",
            new ViolationsOf(
                new Not(
                    new And(
                        new During("act1"),
                        new During("act2")
                    )
                )
            )
        )
    );

    assertEquivalent(expected, result);
  }
}
