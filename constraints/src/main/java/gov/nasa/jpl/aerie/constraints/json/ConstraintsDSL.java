package gov.nasa.jpl.aerie.constraints.json;

import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.And;
import gov.nasa.jpl.aerie.constraints.tree.Or;
import gov.nasa.jpl.aerie.constraints.tree.Expression;
import gov.nasa.jpl.aerie.constraints.tree.True;
import gov.nasa.jpl.aerie.constraints.tree.ViolationsOf;
import gov.nasa.jpl.aerie.json.Iso;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.json.Unit;

import java.util.List;

import static gov.nasa.jpl.aerie.json.BasicParsers.chooseP;
import static gov.nasa.jpl.aerie.json.BasicParsers.listP;
import static gov.nasa.jpl.aerie.json.BasicParsers.literalP;
import static gov.nasa.jpl.aerie.json.BasicParsers.productP;
import static gov.nasa.jpl.aerie.json.BasicParsers.recursiveP;
import static gov.nasa.jpl.aerie.json.Uncurry.tuple;
import static gov.nasa.jpl.aerie.json.Uncurry.untuple;

public class ConstraintsDSL {

  private static JsonParser<And> andF(final JsonParser<Expression<Windows>> windowsExpressionP) {
    return productP
        .field("kind", literalP("WindowsExpressionAnd"))
        .field("expressions", listP(windowsExpressionP))
        .map(Iso.of(untuple(($, expressions) -> new And(expressions)),
                    // This parser is only used for deserialization, so we don't care about the reverse operation.
                    $ -> tuple(Unit.UNIT, $.expressions)));
  }

  private static JsonParser<Or> orF(final JsonParser<Expression<Windows>> windowsExpressionP) {
    return productP
        .field("kind", literalP("WindowsExpressionOr"))
        .field("expressions", listP(windowsExpressionP))
        .map(Iso.of(untuple(($, expressions) -> new Or(expressions)),
                    // This parser is only used for deserialization, so we don't care about the reverse operation.
                    $ -> tuple(Unit.UNIT, $.expressions)));
  }

  private static final JsonParser<True> trueP =
      productP
          .field("kind", literalP("WindowsExpressionTrue"))
          .map(
              Iso.of(
                  untuple(($) -> new True()),
                  $ -> tuple(Unit.UNIT)
              )
          );

  static final JsonParser<Expression<Windows>> windowsExpressionP =
      recursiveP(self -> chooseP(andF(self), orF(self), trueP));

  private static final JsonParser<ViolationsOf> violationsOfP =
      productP
          .field("kind", literalP("ViolationsOf"))
          .field("expression", windowsExpressionP)
          .map(Iso.of(
              untuple(($, constraint) -> new ViolationsOf(constraint)),
              $ -> tuple(Unit.UNIT, $.expression)));

  public static final JsonParser<Expression<List<Violation>>> constraintP =
      recursiveP(self -> chooseP(violationsOfP));

}
