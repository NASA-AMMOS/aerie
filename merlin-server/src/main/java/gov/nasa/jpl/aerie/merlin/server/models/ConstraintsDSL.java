package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.json.Iso;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.json.ProductParsers;
import gov.nasa.jpl.aerie.json.Unit;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.List;
import java.util.Map;

import static gov.nasa.jpl.aerie.json.BasicParsers.chooseP;
import static gov.nasa.jpl.aerie.json.BasicParsers.intP;
import static gov.nasa.jpl.aerie.json.BasicParsers.listP;
import static gov.nasa.jpl.aerie.json.BasicParsers.literalP;
import static gov.nasa.jpl.aerie.json.BasicParsers.productP;
import static gov.nasa.jpl.aerie.json.BasicParsers.recursiveP;
import static gov.nasa.jpl.aerie.json.Uncurry.tuple;
import static gov.nasa.jpl.aerie.json.Uncurry.untuple;

public class ConstraintsDSL {

  private static final ProductParsers.JsonObjectParser<ConstraintSpecifier.DummyConstraintDefinition> dummyConstraintDefinitionP =
      productP
          .field("kind", literalP("DummyConstraint"))
          .field("someNumber", intP)
          .map(Iso.of(
              untuple(($, someNumber) -> new ConstraintSpecifier.DummyConstraintDefinition(someNumber)),
              $ -> tuple(Unit.UNIT, $.someNumber())));

  private static JsonParser<ConstraintSpecifier.ConstraintAnd> constraintAndF(final JsonParser<ConstraintSpecifier> constraintSpecifierP) {
    return productP
        .field("kind", literalP("ConstraintAnd"))
        .field("constraints", listP(constraintSpecifierP))
        .map(Iso.of(untuple(($, constraints) -> new ConstraintSpecifier.ConstraintAnd(constraints)),
                    $ -> tuple(Unit.UNIT, $.constraints())));
  }

  private static JsonParser<ConstraintSpecifier.ConstraintOr> constraintOrF(final JsonParser<ConstraintSpecifier> constraintSpecifierP) {
    return productP
        .field("kind", literalP("ConstraintOr"))
        .field("constraints", listP(constraintSpecifierP))
        .map(Iso.of(untuple(($, constraints) -> new ConstraintSpecifier.ConstraintOr(constraints)),
                    $ -> tuple(Unit.UNIT, $.constraints())));
  }


  private static final JsonParser<ConstraintSpecifier> constraintSpecifierP =
      recursiveP(self -> chooseP(constraintAndF(self), constraintOrF(self), dummyConstraintDefinitionP));


  public static final JsonParser<ConstraintSpecifier> constraintsJsonP = constraintSpecifierP;


  public enum ConstraintKinds {
    DummyConstraint
  }

  public sealed interface ConstraintSpecifier {
    record DummyConstraintDefinition(
        int someNumber
    ) implements ConstraintSpecifier {}
    record ConstraintAnd(List<ConstraintSpecifier> constraints) implements ConstraintSpecifier {}
    record ConstraintOr(List<ConstraintSpecifier> constraints) implements ConstraintSpecifier {}
  }


  public record ActivityTemplate(String activityType, Map<String, SerializedValue> arguments) {}
}
