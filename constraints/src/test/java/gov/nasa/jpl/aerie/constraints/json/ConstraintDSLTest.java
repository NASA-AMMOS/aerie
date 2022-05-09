package gov.nasa.jpl.aerie.constraints.json;

import gov.nasa.jpl.aerie.constraints.tree.*;
import org.junit.jupiter.api.Test;

import javax.json.Json;

import static gov.nasa.jpl.aerie.constraints.Assertions.assertEquivalent;
import static gov.nasa.jpl.aerie.constraints.json.ConstraintsDSL.*;

public final class ConstraintDSLTest {

  @Test
  public void testParseTrue() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "WindowsExpressionTrue")
        .build();

    final var result = windowsExpressionP.parse(json).getSuccessOrThrow();

    final var expected = new True();

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseAnd() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "WindowsExpressionAnd")
        .add("expressions", Json
            .createArrayBuilder()
            .add(Json
                     .createObjectBuilder()
                     .add("kind", "WindowsExpressionTrue"))
            .add(Json
                     .createObjectBuilder()
                     .add("kind", "WindowsExpressionTrue")))
        .build();

    final var result = windowsExpressionP.parse(json).getSuccessOrThrow();

    final var expected =
        new And(
            new True(),
            new True());

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseOr() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "WindowsExpressionOr")
        .add("expressions", Json
            .createArrayBuilder()
            .add(Json
                     .createObjectBuilder()
                     .add("kind", "WindowsExpressionTrue"))
            .add(Json
                     .createObjectBuilder()
                     .add("kind", "WindowsExpressionTrue")))
        .build();

    final var result = windowsExpressionP.parse(json).getSuccessOrThrow();

    final var expected =
        new Or(
            new True(),
            new True());

    assertEquivalent(expected, result);
  }

  @Test
  public void testParseViolationsOf() {
    final var json = Json
        .createObjectBuilder()
        .add("kind", "ViolationsOf")
        .add("expression", Json
            .createObjectBuilder()
                .add("kind", "WindowsExpressionTrue"))
        .build();

    final var result = constraintP.parse(json).getSuccessOrThrow();

    final var expected = new ViolationsOf(new True());

    assertEquivalent(expected, result);
  }

}
