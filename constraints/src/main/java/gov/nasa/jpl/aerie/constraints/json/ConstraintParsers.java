package gov.nasa.jpl.aerie.constraints.json;

import gov.nasa.jpl.aerie.constraints.model.DiscreteProfile;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.Profile;
import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.And;
import gov.nasa.jpl.aerie.constraints.tree.Changed;
import gov.nasa.jpl.aerie.constraints.tree.DiscreteParameter;
import gov.nasa.jpl.aerie.constraints.tree.DiscreteResource;
import gov.nasa.jpl.aerie.constraints.tree.DiscreteValue;
import gov.nasa.jpl.aerie.constraints.tree.During;
import gov.nasa.jpl.aerie.constraints.tree.EndOf;
import gov.nasa.jpl.aerie.constraints.tree.Equal;
import gov.nasa.jpl.aerie.constraints.tree.Expression;
import gov.nasa.jpl.aerie.constraints.tree.ForEachActivity;
import gov.nasa.jpl.aerie.constraints.tree.ForbiddenActivityOverlap;
import gov.nasa.jpl.aerie.constraints.tree.GreaterThan;
import gov.nasa.jpl.aerie.constraints.tree.GreaterThanOrEqual;
import gov.nasa.jpl.aerie.constraints.tree.IfThen;
import gov.nasa.jpl.aerie.constraints.tree.LessThan;
import gov.nasa.jpl.aerie.constraints.tree.LessThanOrEqual;
import gov.nasa.jpl.aerie.constraints.tree.Not;
import gov.nasa.jpl.aerie.constraints.tree.NotEqual;
import gov.nasa.jpl.aerie.constraints.tree.Or;
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
import gov.nasa.jpl.aerie.json.BasicParsers;
import gov.nasa.jpl.aerie.json.Iso;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.json.Unit;

import java.util.List;

import static gov.nasa.jpl.aerie.constraints.json.SerializedValueJsonParser.serializedValueP;
import static gov.nasa.jpl.aerie.json.BasicParsers.chooseP;
import static gov.nasa.jpl.aerie.json.BasicParsers.doubleP;
import static gov.nasa.jpl.aerie.json.BasicParsers.listP;
import static gov.nasa.jpl.aerie.json.BasicParsers.literalP;
import static gov.nasa.jpl.aerie.json.BasicParsers.productP;
import static gov.nasa.jpl.aerie.json.BasicParsers.recursiveP;
import static gov.nasa.jpl.aerie.json.BasicParsers.stringP;
import static gov.nasa.jpl.aerie.json.Uncurry.tuple2;
import static gov.nasa.jpl.aerie.json.Uncurry.tuple3;
import static gov.nasa.jpl.aerie.json.Uncurry.tuple4;
import static gov.nasa.jpl.aerie.json.Uncurry.uncurry2;
import static gov.nasa.jpl.aerie.json.Uncurry.uncurry3;
import static gov.nasa.jpl.aerie.json.Uncurry.uncurry4;

public final class ConstraintParsers {
  private ConstraintParsers() {}

  private static final JsonParser<DiscreteResource> discreteResourceP =
      productP
          .field("type", literalP("DiscreteResource"))
          .field("name", stringP)
          .map(Iso.of(
              uncurry2(type -> name -> new DiscreteResource(name)),
              $ -> tuple2(Unit.UNIT, $.name)));

  private static final JsonParser<RealResource> realResourceP =
      productP
          .field("type", literalP("RealResource"))
          .field("name", stringP)
          .map(Iso.of(
              uncurry2(type -> name -> new RealResource(name)),
              $ -> tuple2(Unit.UNIT, $.name)));

  private static final JsonParser<DiscreteValue> discreteValueP =
      productP
          .field("type", literalP("DiscreteValue"))
          .field("value", serializedValueP)
          .map(Iso.of(
              uncurry2(type -> value -> new DiscreteValue(value)),
              $ -> tuple2(Unit.UNIT, $.value)));

  private static final JsonParser<DiscreteParameter> discreteParameterP =
      productP
          .field("type", literalP("DiscreteParameter"))
          .field("alias", stringP)
          .field("name", stringP)
          .map(Iso.of(
              uncurry3(type -> alias -> name -> new DiscreteParameter(alias, name)),
              $ -> tuple3(Unit.UNIT, $.activityAlias, $.parameterName)));

  private static final JsonParser<Expression<DiscreteProfile>> discreteProfileExprP =
      recursiveP(selfP -> chooseP(
          discreteResourceP,
          discreteValueP,
          discreteParameterP));

  private static final JsonParser<RealValue> realValueP =
      productP
          .field("type", literalP("RealValue"))
          .field("value", doubleP)
          .map(Iso.of(
              uncurry2(type -> value -> new RealValue(value)),
              $ -> tuple2(Unit.UNIT, $.value)));

  private static JsonParser<Plus> plusF(final JsonParser<Expression<LinearProfile>> linearProfileExpressionP) {
      return productP
          .field("type", literalP("Plus"))
          .field("left", linearProfileExpressionP)
          .field("right", linearProfileExpressionP)
          .map(Iso.of(
              uncurry3(type -> left -> right -> new Plus(left, right)),
              $ -> tuple3(Unit.UNIT, $.left, $.right)));
  }

  private static JsonParser<Times> timesF(final JsonParser<Expression<LinearProfile>> linearProfileExpressionP) {
    return productP
        .field("type", literalP("Times"))
        .field("profile", linearProfileExpressionP)
        .field("multiplier", doubleP)
        .map(Iso.of(
            uncurry3(type -> profile -> multiplier -> new Times(profile, multiplier)),
            $ -> tuple3(Unit.UNIT, $.profile, $.multiplier)));
  }

  private static JsonParser<Rate> rateF(final JsonParser<Expression<LinearProfile>> linearProfileExpressionP) {
    return productP
        .field("type", literalP("Rate"))
        .field("profile", linearProfileExpressionP)
        .map(Iso.of(
            uncurry2(type -> profile -> new Rate(profile)),
            $ -> tuple2(Unit.UNIT, $.profile)));
  }

  private final static JsonParser<RealParameter> realParameterP =
      productP
          .field("type", literalP("RealParameter"))
          .field("alias", stringP)
          .field("name", stringP)
          .map(Iso.of(
              uncurry3(type -> alias -> name -> new RealParameter(alias, name)),
              $ -> tuple3(Unit.UNIT, $.activityAlias, $.parameterName)));

  private static final JsonParser<Expression<LinearProfile>> linearProfileExprP =
      recursiveP(selfP -> chooseP(
          realResourceP,
          realValueP,
          realParameterP,
          plusF(selfP),
          timesF(selfP),
          rateF(selfP)));

  private static final JsonParser<ProfileExpression<?>> profileExpressionP =
      BasicParsers.<ProfileExpression<?>>chooseP(
          linearProfileExprP.map(Iso.of(ProfileExpression::new, $ -> $.expression)),
          discreteProfileExprP.map(Iso.of(ProfileExpression::new, $ -> $.expression)));

  private static final JsonParser<Transition> transitionP =
      productP
          .field("type", literalP("Transition"))
          .field("profile", discreteProfileExprP)
          .field("from", serializedValueP)
          .field("to", serializedValueP)
          .map(Iso.of(
              uncurry4(type -> profile -> from -> to -> new Transition(profile, from, to)),
              $ -> tuple4(Unit.UNIT, $.profile, $.oldState, $.newState)));

  private static final JsonParser<During> duringP =
      productP
          .field("type", literalP("During"))
          .field("alias", stringP)
          .map(Iso.of(
              uncurry2(type -> alias -> new During(alias)),
              $ -> tuple2(Unit.UNIT, $.activityAlias)));

  private static final JsonParser<StartOf> startOfP =
      productP
          .field("type", literalP("StartOf"))
          .field("alias", stringP)
          .map(Iso.of(
              uncurry2(type -> alias -> new StartOf(alias)),
              $ -> tuple2(Unit.UNIT, $.activityAlias)));

  private static final JsonParser<EndOf> endOfP =
      productP
          .field("type", literalP("EndOf"))
          .field("alias", stringP)
          .map(Iso.of(
              uncurry2(type -> alias -> new EndOf(alias)),
              $ -> tuple2(Unit.UNIT, $.activityAlias)));

  private static <P extends Profile<P>> JsonParser<Equal<P>> equalF(final JsonParser<Expression<P>> expressionParser) {
    return productP
        .field("type", literalP("Equal"))
        .field("left", expressionParser)
        .field("right", expressionParser)
        .map(Iso.of(
            uncurry3(type -> left -> right -> new Equal<>(left, right)),
            $ -> tuple3(Unit.UNIT, $.left, $.right)));
  }

  private static <P extends Profile<P>> JsonParser<NotEqual<P>> notEqualF(final JsonParser<Expression<P>> expressionParser) {
    return productP
        .field("type", literalP("NotEqual"))
        .field("left", expressionParser)
        .field("right", expressionParser)
        .map(Iso.of(
            uncurry3(type -> left -> right -> new NotEqual<>(left, right)),
            $ -> tuple3(Unit.UNIT, $.left, $.right)));
  }

  private static final JsonParser<LessThan> lessThanP =
      productP
          .field("type", literalP("LessThan"))
          .field("left", linearProfileExprP)
          .field("right", linearProfileExprP)
          .map(Iso.of(
              uncurry3(type -> left -> right -> new LessThan(left, right)),
              $ -> tuple3(Unit.UNIT, $.left, $.right)));

  private static final JsonParser<LessThanOrEqual> lessThanOrEqualP =
      productP
          .field("type", literalP("LessThanOrEqual"))
          .field("left", linearProfileExprP)
          .field("right", linearProfileExprP)
          .map(Iso.of(
              uncurry3(type -> left -> right -> new LessThanOrEqual(left, right)),
              $ -> tuple3(Unit.UNIT, $.left, $.right)));

  private static final JsonParser<GreaterThan> greaterThanP =
      productP
          .field("type", literalP("GreaterThan"))
          .field("left", linearProfileExprP)
          .field("right", linearProfileExprP)
          .map(Iso.of(
              uncurry3(type -> left -> right -> new GreaterThan(left, right)),
              $ -> tuple3(Unit.UNIT, $.left, $.right)));

  private static final JsonParser<GreaterThanOrEqual> greaterThanOrEqualP =
      productP
          .field("type", literalP("GreaterThanOrEqual"))
          .field("left", linearProfileExprP)
          .field("right", linearProfileExprP)
          .map(Iso.of(
              uncurry3(type -> left -> right -> new GreaterThanOrEqual(left, right)),
              $ -> tuple3(Unit.UNIT, $.left, $.right)));

  private static JsonParser<And> andF(final JsonParser<Expression<Windows>> windowsExpressionP) {
    return productP
        .field("type", literalP("And"))
        .field("expressions", listP(windowsExpressionP))
        .map(Iso.of(
            uncurry2(type -> expressions -> new And(expressions)),
            $ -> tuple2(Unit.UNIT, $.expressions)));
  }

  private static JsonParser<Or> orF(final JsonParser<Expression<Windows>> windowsExpressionP) {
    return productP
        .field("type", literalP("Or"))
        .field("expressions", listP(windowsExpressionP))
        .map(Iso.of(
            uncurry2(type -> expressions -> new Or(expressions)),
            $ -> tuple2(Unit.UNIT, $.expressions)));
  }

  private static JsonParser<Not> notF(final JsonParser<Expression<Windows>> windowsExpressionP) {
    return productP
        .field("type", literalP("Not"))
        .field("expression", windowsExpressionP)
        .map(Iso.of(
            uncurry2(type -> expr -> new Not(expr)),
            $ -> tuple2(Unit.UNIT, $.expression)));
  }

  private static JsonParser<IfThen> ifThenF(final JsonParser<Expression<Windows>> windowsExpressionP) {
    return productP
        .field("type", literalP("IfThen"))
        .field("condition", windowsExpressionP)
        .field("expression", windowsExpressionP)
        .map(Iso.of(
            uncurry3(type -> cond -> expr -> new IfThen(cond, expr)),
            $ -> tuple3(Unit.UNIT, $.condition, $.expression)));
  }

  private static JsonParser<ForEachActivity> forEachActivityF(final JsonParser<Expression<List<Violation>>> violationListExpressionP) {
    return productP
        .field("type", literalP("ForEachActivity"))
        .field("activityType", stringP)
        .field("alias", stringP)
        .field("expression", violationListExpressionP)
        .map(Iso.of(
            uncurry4(type -> actType -> alias -> expression -> new ForEachActivity(actType, alias, expression)),
            $ -> tuple4(Unit.UNIT, $.activityType, $.alias, $.expression)));
  }

  private static final JsonParser<Changed<?>> changedP =
      productP
          .field("type", literalP("Changed"))
          .field("expression", profileExpressionP)
          .map(Iso.of(
              uncurry2(type -> expression -> new Changed<>(expression)),
              $ -> tuple2(Unit.UNIT, $.expression)));

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

  private static final JsonParser<ForbiddenActivityOverlap> forbiddenActivityOverlapP =
      productP
          .field("type", literalP("ForbiddenActivityOverlap"))
          .field("activityType1", stringP)
          .field("activityType2", stringP)
          .map(Iso.of(
              uncurry3(type -> act1 -> act2 -> new ForbiddenActivityOverlap(act1, act2)),
              $ -> tuple3(Unit.UNIT, $.activityType1, $.activityType2)));

  static final JsonParser<Expression<List<Violation>>> violationListExpressionP =
      recursiveP(selfP -> chooseP(
          forEachActivityF(selfP),
          windowsExpressionP.map(Iso.of(ViolationsOf::new, $ -> $.expression)),
          forbiddenActivityOverlapP));

  public static final JsonParser<Expression<List<Violation>>> constraintP = violationListExpressionP;
}
