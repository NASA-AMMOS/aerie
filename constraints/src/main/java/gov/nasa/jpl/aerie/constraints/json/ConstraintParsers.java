package gov.nasa.jpl.aerie.constraints.json;

import gov.nasa.jpl.aerie.constraints.model.DiscreteProfile;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.Profile;
import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.And;
import gov.nasa.jpl.aerie.constraints.tree.RealParameter;
import gov.nasa.jpl.aerie.constraints.tree.Changed;
import gov.nasa.jpl.aerie.constraints.tree.DiscreteResource;
import gov.nasa.jpl.aerie.constraints.tree.DiscreteValue;
import gov.nasa.jpl.aerie.constraints.tree.During;
import gov.nasa.jpl.aerie.constraints.tree.EndOf;
import gov.nasa.jpl.aerie.constraints.tree.Equal;
import gov.nasa.jpl.aerie.constraints.tree.Expression;
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
import gov.nasa.jpl.aerie.json.BasicParsers;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;

import javax.json.JsonValue;
import java.util.List;

import static gov.nasa.jpl.aerie.json.BasicParsers.boolP;
import static gov.nasa.jpl.aerie.json.BasicParsers.chooseP;
import static gov.nasa.jpl.aerie.json.BasicParsers.doubleP;
import static gov.nasa.jpl.aerie.json.BasicParsers.listP;
import static gov.nasa.jpl.aerie.json.BasicParsers.literalP;
import static gov.nasa.jpl.aerie.json.BasicParsers.longP;
import static gov.nasa.jpl.aerie.json.BasicParsers.mapP;
import static gov.nasa.jpl.aerie.json.BasicParsers.nullP;
import static gov.nasa.jpl.aerie.json.BasicParsers.productP;
import static gov.nasa.jpl.aerie.json.BasicParsers.recursiveP;
import static gov.nasa.jpl.aerie.json.BasicParsers.stringP;
import static gov.nasa.jpl.aerie.json.Uncurry.uncurry2;
import static gov.nasa.jpl.aerie.json.Uncurry.uncurry3;
import static gov.nasa.jpl.aerie.json.Uncurry.uncurry4;

public final class ConstraintParsers {
  private ConstraintParsers() {}

  private static final JsonParser<SerializedValue> serializedValueP =
      recursiveP(selfP -> BasicParsers
          . <SerializedValue>sumP()
          . when(
              JsonValue.ValueType.NULL,
              nullP.map(SerializedValue::of))
          . when(
              JsonValue.ValueType.TRUE,
              boolP.map(SerializedValue::of))
          . when(
              JsonValue.ValueType.FALSE,
              boolP.map(SerializedValue::of))
          . when(
              JsonValue.ValueType.STRING,
              stringP.map(SerializedValue::of))
          . when(JsonValue.ValueType.NUMBER, chooseP(
              longP.map(SerializedValue::of),
              doubleP.map(SerializedValue::of)))
          . when(
              JsonValue.ValueType.ARRAY,
              listP(selfP).map(SerializedValue::of))
          . when(
              JsonValue.ValueType.OBJECT,
              mapP(selfP).map(SerializedValue::of)));

  private static final JsonParser<Expression<DiscreteProfile>> discreteResourceP =
      productP
          .field("type", literalP("DiscreteResource"))
          .field("name", stringP)
          .map(uncurry2(type -> name -> new DiscreteResource(name)));

  private static final JsonParser<Expression<LinearProfile>> realResourceP =
      productP
          .field("type", literalP("RealResource"))
          .field("name", stringP)
          .map(uncurry2(type -> name -> new RealResource(name)));

  private static final JsonParser<Expression<DiscreteProfile>> discreteValueP =
      productP
          .field("type", literalP("DiscreteValue"))
          .field("value", serializedValueP)
          .map(uncurry2(type -> value -> new DiscreteValue(value)));

  private static final JsonParser<Expression<DiscreteProfile>> discreteParameterP =
      productP
          .field("type", literalP("DiscreteParameter"))
          .field("alias", stringP)
          .field("name", stringP)
          .map(uncurry3(type -> alias -> name -> new DiscreteParameter(alias, name)));

  private static final JsonParser<Expression<DiscreteProfile>> discreteProfileExprP =
      recursiveP(selfP -> chooseP(
          discreteResourceP,
          discreteValueP,
          discreteParameterP));

  private static final JsonParser<Expression<LinearProfile>> realValueP =
      productP
          .field("type", literalP("RealValue"))
          .field("value", doubleP)
          .map(uncurry2(type -> value -> new RealValue(value)));

  private static JsonParser<Expression<LinearProfile>> plusF(final JsonParser<Expression<LinearProfile>> linearProfileExpressionP) {
      return productP
          .field("type", literalP("Plus"))
          .field("left", linearProfileExpressionP)
          .field("right", linearProfileExpressionP)
          .map(uncurry3(type -> left -> right -> new Plus(left, right)));
  }

  private static JsonParser<Expression<LinearProfile>> timesF(final JsonParser<Expression<LinearProfile>> linearProfileExpressionP) {
    return productP
        .field("type", literalP("Times"))
        .field("profile", linearProfileExpressionP)
        .field("multiplier", doubleP)
        .map(uncurry3(type -> profile -> multiplier -> new Times(profile, multiplier)));
  }

  private static JsonParser<Expression<LinearProfile>> rateF(final JsonParser<Expression<LinearProfile>> linearProfileExpressionP) {
    return productP
        .field("type", literalP("Rate"))
        .field("profile", linearProfileExpressionP)
        .map(uncurry2(type -> profile -> new Rate(profile)));
  }

  private final static JsonParser<Expression<LinearProfile>> realParameterP =
      productP
          .field("type", literalP("RealParameter"))
          .field("alias", stringP)
          .field("name", stringP)
          .map(uncurry3(type -> alias -> name -> new RealParameter(alias, name)));

  private static final JsonParser<Expression<LinearProfile>> linearProfileExprP =
      recursiveP(selfP -> chooseP(
          realResourceP,
          realValueP,
          realParameterP,
          plusF(selfP),
          timesF(selfP),
          rateF(selfP)));

  private static final JsonParser<ProfileExpression<?>> profileExpressionP =
      chooseP(
          linearProfileExprP.map(ProfileExpression::new),
          discreteProfileExprP.map(ProfileExpression::new));

  private static final JsonParser<Expression<Windows>> transitionP =
      productP
          .field("type", literalP("Transition"))
          .field("profile", discreteProfileExprP)
          .field("from", serializedValueP)
          .field("to", serializedValueP)
          .map(uncurry4(type -> profile -> from -> to -> new Transition(profile, from, to)));

  private static final JsonParser<Expression<Windows>> duringP =
      productP
          .field("type", literalP("During"))
          .field("alias", stringP)
          .map(uncurry2(type -> alias -> new During(alias)));

  private static final JsonParser<Expression<Windows>> startOfP =
      productP
          .field("type", literalP("StartOf"))
          .field("alias", stringP)
          .map(uncurry2(type -> alias -> new StartOf(alias)));

  private static final JsonParser<Expression<Windows>> endOfP =
      productP
          .field("type", literalP("EndOf"))
          .field("alias", stringP)
          .map(uncurry2(type -> alias -> new EndOf(alias)));

  private static <P extends Profile<P>> JsonParser<Expression<Windows>> equalF(final JsonParser<Expression<P>> expressionParser) {
    return productP
        .field("type", literalP("Equal"))
        .field("left", expressionParser)
        .field("right", expressionParser)
        .map(uncurry3(type -> left -> right -> new Equal<>(left, right)));
  }

  private static <P extends Profile<P>> JsonParser<Expression<Windows>> notEqualF(final JsonParser<Expression<P>> expressionParser) {
    return productP
        .field("type", literalP("NotEqual"))
        .field("left", expressionParser)
        .field("right", expressionParser)
        .map(uncurry3(type -> left -> right -> new NotEqual<>(left, right)));
  }

  private static final JsonParser<Expression<Windows>> lessThanP =
    productP
        .field("type", literalP("LessThan"))
        .field("left", linearProfileExprP)
        .field("right", linearProfileExprP)
        .map(uncurry3(type -> left -> right -> new LessThan(left, right)));

  private static final JsonParser<Expression<Windows>> lessThanOrEqualP =
      productP
          .field("type", literalP("LessThanOrEqual"))
          .field("left", linearProfileExprP)
          .field("right", linearProfileExprP)
          .map(uncurry3(type -> left -> right -> new LessThanOrEqual(left, right)));

  private static final JsonParser<Expression<Windows>> greaterThanP =
      productP
          .field("type", literalP("GreaterThan"))
          .field("left", linearProfileExprP)
          .field("right", linearProfileExprP)
          .map(uncurry3(type -> left -> right -> new GreaterThan(left, right)));

  private static final JsonParser<Expression<Windows>> greaterThanOrEqualP =
      productP
          .field("type", literalP("GreaterThanOrEqual"))
          .field("left", linearProfileExprP)
          .field("right", linearProfileExprP)
          .map(uncurry3(type -> left -> right -> new GreaterThanOrEqual(left, right)));

  private static JsonParser<Expression<Windows>> andF(final JsonParser<Expression<Windows>> windowsExpressionP) {
    return productP
        .field("type", literalP("And"))
        .field("expressions", listP(windowsExpressionP))
        .map(uncurry2(type -> expressions -> new And(expressions)));
  }

  private static JsonParser<Expression<Windows>> orF(final JsonParser<Expression<Windows>> windowsExpressionP) {
    return productP
        .field("type", literalP("Or"))
        .field("expressions", listP(windowsExpressionP))
        .map(uncurry2(type -> expressions -> new Or(expressions)));
  }

  private static JsonParser<Expression<Windows>> notF(final JsonParser<? extends Expression<Windows>> windowsExpressionP) {
    return productP
        .field("type", literalP("Not"))
        .field("expression", windowsExpressionP)
        .map(uncurry2(type -> expr -> new Not(expr)));
  }

  private static JsonParser<Expression<Windows>> ifThenF(final JsonParser<? extends Expression<Windows>> windowsExpressionP) {
    return productP
        .field("type", literalP("IfThen"))
        .field("condition", windowsExpressionP)
        .field("expression", windowsExpressionP)
        .map(uncurry3(type -> cond -> expr -> new Or(new Not(cond), expr)));
  }

  private static JsonParser<Expression<List<Violation>>> forEachActivityF(final JsonParser<Expression<List<Violation>>> violationListExpressionP) {
    return productP
        .field("type", literalP("ForEachActivity"))
        .field("activityType", stringP)
        .field("alias", stringP)
        .field("expression", violationListExpressionP)
        .map(uncurry4(type -> actType -> alias -> expression -> new ForEachActivity(actType, alias, expression)));
  }

  private static final JsonParser<Expression<Windows>> changedP =
      productP
          .field("type", literalP("Changed"))
          .field("expression", profileExpressionP)
          .map(uncurry2(type -> expression -> new Changed<>(expression)));

  static final JsonParser<Expression<LinearProfile>> linearProfileExpressionP = linearProfileExprP;
  static final JsonParser<Expression<DiscreteProfile>> discreteProfileExpressionP = discreteProfileExprP;

  static final JsonParser<Expression<Windows>> windowsExpressionP =
      recursiveP(selfP -> chooseP(
          duringP,
          startOfP,
          endOfP,
          changedP,
          lessThanP,
          lessThanOrEqualP,
          greaterThanOrEqualP,
          greaterThanP,
          transitionP,
          equalF(linearProfileExprP),
          equalF(discreteProfileExprP),
          notEqualF(linearProfileExprP),
          notEqualF(discreteProfileExprP),
          andF(selfP),
          orF(selfP),
          notF(selfP),
          ifThenF(selfP)));

  static final JsonParser<Expression<List<Violation>>> violationListExpressionP =
      recursiveP(selfP -> chooseP(
          forEachActivityF(selfP),
          windowsExpressionP.map(ViolationsOf::new)));

  public static final JsonParser<Expression<List<Violation>>> constraintP = violationListExpressionP;
}
