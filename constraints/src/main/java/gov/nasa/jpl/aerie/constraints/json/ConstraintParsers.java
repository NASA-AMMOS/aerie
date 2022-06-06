package gov.nasa.jpl.aerie.constraints.json;

import gov.nasa.jpl.aerie.constraints.model.DiscreteProfile;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.Profile;
import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.*;
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
import static gov.nasa.jpl.aerie.json.Uncurry.tuple;
import static gov.nasa.jpl.aerie.json.Uncurry.untuple;

public final class ConstraintParsers {
  private ConstraintParsers() {}

  static final JsonParser<DiscreteResource> discreteResourceP =
      productP
          .field("kind", literalP("DiscreteProfileResource"))
          .field("name", stringP)
          .map(Iso.of(
              untuple((kind, name) -> new DiscreteResource(name)),
              $ -> tuple(Unit.UNIT, $.name)));

  static final JsonParser<DiscreteValue> discreteValueP =
      productP
          .field("kind", literalP("DiscreteProfileValue"))
          .field("value", serializedValueP)
          .map(Iso.of(
              untuple((kind, value) -> new DiscreteValue(value)),
              $ -> tuple(Unit.UNIT, $.value)));

  static final JsonParser<DiscreteParameter> discreteParameterP =
      productP
          .field("kind", literalP("DiscreteProfileParameter"))
          .field("alias", stringP)
          .field("name", stringP)
          .map(Iso.of(
              untuple((kind, alias, name) -> new DiscreteParameter(alias, name)),
              $ -> tuple(Unit.UNIT, $.activityAlias, $.parameterName)));

  static final JsonParser<Expression<DiscreteProfile>> discreteProfileExprP =
      recursiveP(selfP -> chooseP(
          discreteResourceP,
          discreteValueP,
          discreteParameterP));

  static final JsonParser<RealResource> realResourceP =
      productP
          .field("kind", literalP("RealProfileResource"))
          .field("name", stringP)
          .map(Iso.of(
              untuple((kind, name) -> new RealResource(name)),
              $ -> tuple(Unit.UNIT, $.name)));

  static final JsonParser<RealValue> realValueP =
      productP
          .field("kind", literalP("RealProfileValue"))
          .field("value", doubleP)
          .map(Iso.of(
              untuple((kind, value) -> new RealValue(value)),
              $ -> tuple(Unit.UNIT, $.value)));

  static final JsonParser<RealParameter> realParameterP =
      productP
          .field("kind", literalP("RealProfileParameter"))
          .field("alias", stringP)
          .field("name", stringP)
          .map(Iso.of(
              untuple((kind, alias, name) -> new RealParameter(alias, name)),
              $ -> tuple(Unit.UNIT, $.activityAlias, $.parameterName)));

  static JsonParser<Plus> plusF(final JsonParser<Expression<LinearProfile>> linearProfileExpressionP) {
    return productP
        .field("kind", literalP("RealProfilePlus"))
        .field("left", linearProfileExpressionP)
        .field("right", linearProfileExpressionP)
        .map(Iso.of(
            untuple((kind, left, right) -> new Plus(left, right)),
            $ -> tuple(Unit.UNIT, $.left, $.right)));
  }

  static JsonParser<Times> timesF(final JsonParser<Expression<LinearProfile>> linearProfileExpressionP) {
    return productP
        .field("kind", literalP("RealProfileTimes"))
        .field("profile", linearProfileExpressionP)
        .field("multiplier", doubleP)
        .map(Iso.of(
            untuple((kind, profile, multiplier) -> new Times(profile, multiplier)),
            $ -> tuple(Unit.UNIT, $.profile, $.multiplier)));
  }

  static JsonParser<Rate> rateF(final JsonParser<Expression<LinearProfile>> linearProfileExpressionP) {
    return productP
        .field("kind", literalP("RealProfileRate"))
        .field("profile", linearProfileExpressionP)
        .map(Iso.of(
            untuple((kind, profile) -> new Rate(profile)),
            $ -> tuple(Unit.UNIT, $.profile)));
  }

  static final JsonParser<Expression<LinearProfile>> linearProfileExprP =
      recursiveP(selfP -> chooseP(
          realResourceP,
          realValueP,
          realParameterP,
          plusF(selfP),
          timesF(selfP),
          rateF(selfP)));

  static final JsonParser<ProfileExpression<?>> profileExpressionP =
      BasicParsers.<ProfileExpression<?>>chooseP(
          linearProfileExprP.map(Iso.of(ProfileExpression::new, $ -> $.expression)),
          discreteProfileExprP.map(Iso.of(ProfileExpression::new, $ -> $.expression)));

  static final JsonParser<Transition> transitionP =
      productP
          .field("kind", literalP("DiscreteProfileTransition"))
          .field("profile", discreteProfileExprP)
          .field("from", serializedValueP)
          .field("to", serializedValueP)
          .map(Iso.of(
              untuple((kind, profile, from, to) -> new Transition(profile, from, to)),
              $ -> tuple(Unit.UNIT, $.profile, $.oldState, $.newState)));

  static final JsonParser<ActivityWindow> activityWindowP =
      productP
          .field("kind", literalP("WindowsExpressionActivityWindow"))
          .field("alias", stringP)
          .map(Iso.of(
              untuple((kind, alias) -> new ActivityWindow(alias)),
              $ -> tuple(Unit.UNIT, $.activityAlias)));

  static final JsonParser<StartOf> startOfP =
      productP
          .field("kind", literalP("WindowsExpressionStartOf"))
          .field("alias", stringP)
          .map(Iso.of(
              untuple((kind, alias) -> new StartOf(alias)),
              $ -> tuple(Unit.UNIT, $.activityAlias)));

  static final JsonParser<EndOf> endOfP =
      productP
          .field("kind", literalP("WindowsExpressionEndOf"))
          .field("alias", stringP)
          .map(Iso.of(
              untuple((kind, alias) -> new EndOf(alias)),
              $ -> tuple(Unit.UNIT, $.activityAlias)));

  static <P extends Profile<P>> JsonParser<Equal<P>> equalF(final JsonParser<Expression<P>> expressionParser) {
    return productP
        .field("kind", literalP("ExpressionEqual"))
        .field("left", expressionParser)
        .field("right", expressionParser)
        .map(Iso.of(
            untuple((kind, left, right) -> new Equal<>(left, right)),
            $ -> tuple(Unit.UNIT, $.left, $.right)));
  }

  static <P extends Profile<P>> JsonParser<NotEqual<P>> notEqualF(final JsonParser<Expression<P>> expressionParser) {
    return productP
        .field("kind", literalP("ExpressionNotEqual"))
        .field("left", expressionParser)
        .field("right", expressionParser)
        .map(Iso.of(
            untuple((kind, left, right) -> new NotEqual<>(left, right)),
            $ -> tuple(Unit.UNIT, $.left, $.right)));
  }

  static final JsonParser<LessThan> lessThanP =
      productP
          .field("kind", literalP("RealProfileLessThan"))
          .field("left", linearProfileExprP)
          .field("right", linearProfileExprP)
          .map(Iso.of(
              untuple((kind, left, right) -> new LessThan(left, right)),
              $ -> tuple(Unit.UNIT, $.left, $.right)));

  static final JsonParser<LessThanOrEqual> lessThanOrEqualP =
      productP
          .field("kind", literalP("RealProfileLessThanOrEqual"))
          .field("left", linearProfileExprP)
          .field("right", linearProfileExprP)
          .map(Iso.of(
              untuple((kind, left, right) -> new LessThanOrEqual(left, right)),
              $ -> tuple(Unit.UNIT, $.left, $.right)));

  static final JsonParser<GreaterThan> greaterThanP =
      productP
          .field("kind", literalP("RealProfileGreaterThan"))
          .field("left", linearProfileExprP)
          .field("right", linearProfileExprP)
          .map(Iso.of(
              untuple((kind, left, right) -> new GreaterThan(left, right)),
              $ -> tuple(Unit.UNIT, $.left, $.right)));

  static final JsonParser<GreaterThanOrEqual> greaterThanOrEqualP =
      productP
          .field("kind", literalP("RealProfileGreaterThanOrEqual"))
          .field("left", linearProfileExprP)
          .field("right", linearProfileExprP)
          .map(Iso.of(
              untuple((kind, left, right) -> new GreaterThanOrEqual(left, right)),
              $ -> tuple(Unit.UNIT, $.left, $.right)));

  static JsonParser<All> allF(final JsonParser<Expression<Windows>> windowsExpressionP) {
    return productP
        .field("kind", literalP("WindowsExpressionAll"))
        .field("expressions", listP(windowsExpressionP))
        .map(Iso.of(
            untuple((kind, expressions) -> new All(expressions)),
            $ -> tuple(Unit.UNIT, $.expressions)));
  }

  static JsonParser<Any> anyF(final JsonParser<Expression<Windows>> windowsExpressionP) {
    return productP
        .field("kind", literalP("WindowsExpressionAny"))
        .field("expressions", listP(windowsExpressionP))
        .map(Iso.of(
            untuple((kind, expressions) -> new Any(expressions)),
            $ -> tuple(Unit.UNIT, $.expressions)));
  }

  static JsonParser<Invert> invertF(final JsonParser<Expression<Windows>> windowsExpressionP) {
    return productP
        .field("kind", literalP("WindowsExpressionInvert"))
        .field("expression", windowsExpressionP)
        .map(Iso.of(
            untuple((kind, expr) -> new Invert(expr)),
            $ -> tuple(Unit.UNIT, $.expression)));
  }

  static JsonParser<ForEachActivity> forEachActivityF(final JsonParser<Expression<List<Violation>>> violationListExpressionP) {
    return productP
        .field("kind", literalP("ForEachActivity"))
        .field("activityType", stringP)
        .field("alias", stringP)
        .field("expression", violationListExpressionP)
        .map(Iso.of(
            untuple((kind, actType, alias, expression) -> new ForEachActivity(actType, alias, expression)),
            $ -> tuple(Unit.UNIT, $.activityType, $.alias, $.expression)));
  }

  static final JsonParser<Changed<?>> changedP =
      productP
          .field("kind", literalP("ProfileChanged"))
          .field("expression", profileExpressionP)
          .map(Iso.of(
              untuple((kind, expression) -> new Changed<>(expression)),
              $ -> tuple(Unit.UNIT, $.expression)));

  public static final JsonParser<Expression<Windows>> windowsExpressionP =
      recursiveP(selfP -> chooseP(
          activityWindowP,
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
          allF(selfP),
          anyF(selfP),
          invertF(selfP)));

  static final JsonParser<ViolationsOf> violationsOfP =
      productP
          .field("kind", literalP("ViolationsOf"))
          .field("expression", windowsExpressionP)
          .map(Iso.of(
              untuple((kind, expression) -> new ViolationsOf(expression)),
              $ -> tuple(Unit.UNIT, $.expression)
          ));


  public static final JsonParser<Expression<List<Violation>>> constraintP =
      recursiveP(selfP -> chooseP(
          forEachActivityF(selfP),
          windowsExpressionP.map(Iso.of(ViolationsOf::new, $ -> $.expression)),
          violationsOfP));
}
