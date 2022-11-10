package gov.nasa.jpl.aerie.constraints.json;

import gov.nasa.jpl.aerie.constraints.model.DiscreteProfile;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.Profile;
import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.IntervalContainer;
import gov.nasa.jpl.aerie.constraints.time.Spans;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.*;
import gov.nasa.jpl.aerie.json.BasicParsers;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.json.Unit;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.List;

import static gov.nasa.jpl.aerie.constraints.json.SerializedValueJsonParser.serializedValueP;
import static gov.nasa.jpl.aerie.json.BasicParsers.chooseP;
import static gov.nasa.jpl.aerie.json.BasicParsers.doubleP;
import static gov.nasa.jpl.aerie.json.BasicParsers.enumP;
import static gov.nasa.jpl.aerie.json.BasicParsers.intP;
import static gov.nasa.jpl.aerie.json.BasicParsers.listP;
import static gov.nasa.jpl.aerie.json.BasicParsers.literalP;
import static gov.nasa.jpl.aerie.json.BasicParsers.longP;
import static gov.nasa.jpl.aerie.json.BasicParsers.productP;
import static gov.nasa.jpl.aerie.json.BasicParsers.recursiveP;
import static gov.nasa.jpl.aerie.json.BasicParsers.stringP;
import static gov.nasa.jpl.aerie.json.Uncurry.tuple;
import static gov.nasa.jpl.aerie.json.Uncurry.untuple;

public final class ConstraintParsers {
  private ConstraintParsers() {}

  static final JsonParser<Interval.Inclusivity> inclusivityP =
      enumP(Interval.Inclusivity.class, Enum::name);

  static <P extends Profile<P>> JsonParser<AssignGaps<P>> assignGapsF(final JsonParser<Expression<P>> profileParser) {
    return productP
        .field("kind", literalP("AssignGapsExpression"))
        .field("originalProfile", profileParser)
        .field("defaultProfile", profileParser)
        .map(
            untuple((kind, originalProfile, defaultProfile) -> new AssignGaps<P>(originalProfile, defaultProfile)),
            $ -> tuple(Unit.UNIT, $.originalProfile(), $.defaultProfile())
        );
  }

  static final JsonParser<DiscreteResource> discreteResourceP =
      productP
          .field("kind", literalP("DiscreteProfileResource"))
          .field("name", stringP)
          .map(
              untuple((kind, name) -> new DiscreteResource(name)),
              $ -> tuple(Unit.UNIT, $.name));

  static final JsonParser<DiscreteValue> discreteValueP =
      productP
          .field("kind", literalP("DiscreteProfileValue"))
          .field("value", serializedValueP)
          .map(
              untuple((kind, value) -> new DiscreteValue(value)),
              $ -> tuple(Unit.UNIT, $.value));

  static final JsonParser<DiscreteParameter> discreteParameterP =
      productP
          .field("kind", literalP("DiscreteProfileParameter"))
          .field("alias", stringP)
          .field("name", stringP)
          .map(
              untuple((kind, alias, name) -> new DiscreteParameter(alias, name)),
              $ -> tuple(Unit.UNIT, $.activityAlias, $.parameterName));

  static final JsonParser<Expression<DiscreteProfile>> discreteProfileExprP =
      recursiveP(selfP -> chooseP(
          discreteResourceP,
          discreteValueP,
          discreteParameterP,
          assignGapsF(selfP)
      ));

  static final JsonParser<RealResource> realResourceP =
      productP
          .field("kind", literalP("RealProfileResource"))
          .field("name", stringP)
          .map(
              untuple((kind, name) -> new RealResource(name)),
              $ -> tuple(Unit.UNIT, $.name));

  static final JsonParser<RealValue> realValueP =
      productP
          .field("kind", literalP("RealProfileValue"))
          .field("value", doubleP)
          .map(
              untuple((kind, value) -> new RealValue(value)),
              $ -> tuple(Unit.UNIT, $.value));

  static final JsonParser<RealParameter> realParameterP =
      productP
          .field("kind", literalP("RealProfileParameter"))
          .field("alias", stringP)
          .field("name", stringP)
          .map(
              untuple((kind, alias, name) -> new RealParameter(alias, name)),
              $ -> tuple(Unit.UNIT, $.activityAlias, $.parameterName));

  static JsonParser<Plus> plusF(final JsonParser<Expression<LinearProfile>> linearProfileExpressionP) {
    return productP
        .field("kind", literalP("RealProfilePlus"))
        .field("left", linearProfileExpressionP)
        .field("right", linearProfileExpressionP)
        .map(
            untuple((kind, left, right) -> new Plus(left, right)),
            $ -> tuple(Unit.UNIT, $.left, $.right));
  }

  static JsonParser<Times> timesF(final JsonParser<Expression<LinearProfile>> linearProfileExpressionP) {
    return productP
        .field("kind", literalP("RealProfileTimes"))
        .field("profile", linearProfileExpressionP)
        .field("multiplier", doubleP)
        .map(
            untuple((kind, profile, multiplier) -> new Times(profile, multiplier)),
            $ -> tuple(Unit.UNIT, $.profile, $.multiplier));
  }

  static JsonParser<Rate> rateF(final JsonParser<Expression<LinearProfile>> linearProfileExpressionP) {
    return productP
        .field("kind", literalP("RealProfileRate"))
        .field("profile", linearProfileExpressionP)
        .map(
            untuple((kind, profile) -> new Rate(profile)),
            $ -> tuple(Unit.UNIT, $.profile));
  }

  static final JsonParser<Expression<LinearProfile>> linearProfileExprP =
      recursiveP(selfP -> chooseP(
          realResourceP,
          realValueP,
          realParameterP,
          plusF(selfP),
          timesF(selfP),
          rateF(selfP),
          assignGapsF(selfP)
      ));

  static final JsonParser<ProfileExpression<?>> profileExpressionP =
      BasicParsers.<ProfileExpression<?>>chooseP(
          linearProfileExprP.map(ProfileExpression::new, $ -> $.expression),
          discreteProfileExprP.map(ProfileExpression::new, $ -> $.expression));

  static final JsonParser<Transition> transitionP =
      productP
          .field("kind", literalP("DiscreteProfileTransition"))
          .field("profile", discreteProfileExprP)
          .field("from", serializedValueP)
          .field("to", serializedValueP)
          .map(
              untuple((kind, profile, from, to) -> new Transition(profile, from, to)),
              $ -> tuple(Unit.UNIT, $.profile, $.oldState, $.newState));

  static final JsonParser<ActivityWindow> activityWindowP =
      productP
          .field("kind", literalP("WindowsExpressionActivityWindow"))
          .field("alias", stringP)
          .map(
              untuple((kind, alias) -> new ActivityWindow(alias)),
              $ -> tuple(Unit.UNIT, $.activityAlias));

  static final JsonParser<ActivitySpan> activitySpanP =
      productP
          .field("kind", literalP("SpansExpressionActivitySpan"))
          .field("alias", stringP)
          .map(
              untuple((kind, alias) -> new ActivitySpan(alias)),
              $ -> tuple(Unit.UNIT, $.activityAlias));
  static final JsonParser<StartOf> startOfP =
      productP
          .field("kind", literalP("WindowsExpressionStartOf"))
          .field("alias", stringP)
          .map(
              untuple((kind, alias) -> new StartOf(alias)),
              $ -> tuple(Unit.UNIT, $.activityAlias));

  static final JsonParser<Duration> durationP =
      longP
          . map(
              microseconds -> Duration.of(microseconds, Duration.MICROSECONDS),
              duration -> duration.in(Duration.MICROSECONDS));

  static JsonParser<ShiftBy> shiftByF(JsonParser<Expression<Windows>> windowsExpressionP) {
    return productP
        .field("kind", literalP("WindowsExpressionShiftBy"))
        .field("windowExpression", windowsExpressionP)
        .field("fromStart", durationP)
        .field("fromEnd", durationP)
        .map(
            untuple((kind, windowsExpression, fromStart, fromEnd) -> new ShiftBy(windowsExpression, fromStart, fromEnd)),
            $ -> tuple(Unit.UNIT, $.windows, $.fromStart, $.fromEnd));
  }
  static final JsonParser<EndOf> endOfP =
      productP
          .field("kind", literalP("WindowsExpressionEndOf"))
          .field("alias", stringP)
          .map(
              untuple((kind, alias) -> new EndOf(alias)),
              $ -> tuple(Unit.UNIT, $.activityAlias));

  static <P extends Profile<P>> JsonParser<Equal<P>> equalF(final JsonParser<Expression<P>> expressionParser) {
    return productP
        .field("kind", literalP("ExpressionEqual"))
        .field("left", expressionParser)
        .field("right", expressionParser)
        .map(
            untuple((kind, left, right) -> new Equal<>(left, right)),
            $ -> tuple(Unit.UNIT, $.left, $.right));
  }

  static <P extends Profile<P>> JsonParser<NotEqual<P>> notEqualF(final JsonParser<Expression<P>> expressionParser) {
    return productP
        .field("kind", literalP("ExpressionNotEqual"))
        .field("left", expressionParser)
        .field("right", expressionParser)
        .map(
            untuple((kind, left, right) -> new NotEqual<>(left, right)),
            $ -> tuple(Unit.UNIT, $.left, $.right));
  }

  static final JsonParser<LessThan> lessThanP =
      productP
          .field("kind", literalP("RealProfileLessThan"))
          .field("left", linearProfileExprP)
          .field("right", linearProfileExprP)
          .map(
              untuple((kind, left, right) -> new LessThan(left, right)),
              $ -> tuple(Unit.UNIT, $.left, $.right));

  static JsonParser<LongerThan> longerThanP(JsonParser<Expression<Windows>> windowsExpressionP) {
    return productP
            .field("kind", literalP("WindowsExpressionLongerThan"))
            .field("windowExpression", windowsExpressionP)
            .field("duration", durationP)
            .map(
                untuple((kind, windowsExpression, duration) -> new LongerThan(windowsExpression, duration)),
                $ -> tuple(Unit.UNIT, $.windows, $.duration));
  }

  static JsonParser<ShorterThan> shorterThanP(JsonParser<Expression<Windows>> windowsExpressionP) {
    return productP
        .field("kind", literalP("WindowsExpressionShorterThan"))
        .field("windowExpression", windowsExpressionP)
        .field("duration", durationP)
        .map(
            untuple((kind, windowsExpression, duration) -> new ShorterThan(windowsExpression, duration)),
            $ -> tuple(Unit.UNIT, $.windows, $.duration));
  }

  static final JsonParser<LessThanOrEqual> lessThanOrEqualP =
      productP
          .field("kind", literalP("RealProfileLessThanOrEqual"))
          .field("left", linearProfileExprP)
          .field("right", linearProfileExprP)
          .map(
              untuple((kind, left, right) -> new LessThanOrEqual(left, right)),
              $ -> tuple(Unit.UNIT, $.left, $.right));

  static final JsonParser<GreaterThan> greaterThanP =
      productP
          .field("kind", literalP("RealProfileGreaterThan"))
          .field("left", linearProfileExprP)
          .field("right", linearProfileExprP)
          .map(
              untuple((kind, left, right) -> new GreaterThan(left, right)),
              $ -> tuple(Unit.UNIT, $.left, $.right));

  static final JsonParser<GreaterThanOrEqual> greaterThanOrEqualP =
      productP
          .field("kind", literalP("RealProfileGreaterThanOrEqual"))
          .field("left", linearProfileExprP)
          .field("right", linearProfileExprP)
          .map(
              untuple((kind, left, right) -> new GreaterThanOrEqual(left, right)),
              $ -> tuple(Unit.UNIT, $.left, $.right));

  static JsonParser<And> andF(final JsonParser<Expression<Windows>> windowsExpressionP) {
    return productP
        .field("kind", literalP("WindowsExpressionAnd"))
        .field("expressions", listP(windowsExpressionP))
        .map(
            untuple((kind, expressions) -> new And(expressions)),
            $ -> tuple(Unit.UNIT, $.expressions));
  }

  static JsonParser<Or> orF(final JsonParser<Expression<Windows>> windowsExpressionP) {
    return productP
        .field("kind", literalP("WindowsExpressionOr"))
        .field("expressions", listP(windowsExpressionP))
        .map(
            untuple((kind, expressions) -> new Or(expressions)),
            $ -> tuple(Unit.UNIT, $.expressions));
  }

  static JsonParser<Not> notF(final JsonParser<Expression<Windows>> windowsExpressionP) {
    return productP
        .field("kind", literalP("WindowsExpressionNot"))
        .field("expression", windowsExpressionP)
        .map(
            untuple((kind, expr) -> new Not(expr)),
            $ -> tuple(Unit.UNIT, $.expression));
  }

  static <I extends IntervalContainer<I>> JsonParser<Starts<I>> startsF(final JsonParser<Expression<I>> intervalExpressionP) {
    return productP
        .field("kind", literalP("IntervalsExpressionStarts"))
        .field("expression", intervalExpressionP)
        .map(
            untuple((kind, expr) -> new Starts<I>(expr)),
            $ -> tuple(Unit.UNIT, $.expression));
  }

  static <I extends IntervalContainer<I>> JsonParser<Ends<I>> endsF(final JsonParser<Expression<I>> intervalsExpressionP) {
    return productP
        .field("kind", literalP("IntervalsExpressionEnds"))
        .field("expression", intervalsExpressionP)
        .map(
            untuple((kind, expr) -> new Ends<I>(expr)),
            $ -> tuple(Unit.UNIT, $.expression));
  }

  static <I extends IntervalContainer<I>> JsonParser<Split<I>> splitF(final JsonParser<Expression<I>> intervalExpressionP) {
    return productP
        .field("kind", literalP("SpansExpressionSplit"))
        .field("intervals", intervalExpressionP)
        .field("numberOfSubIntervals", intP)
        .field("internalStartInclusivity", inclusivityP)
        .field("internalEndInclusivity", inclusivityP)
        .map(
            untuple((kind, expr, numberOfSubWindows, internalStartInclusivity, internalEndInclusivity) -> new Split<I>(
                expr, numberOfSubWindows, internalStartInclusivity, internalEndInclusivity)
            ),
            $ -> tuple(Unit.UNIT, $.intervals, $.numberOfSubIntervals, $.internalStartInclusivity, $.internalEndInclusivity));
  }

  static JsonParser<WindowsFromSpans> windowsFromSpansF(final JsonParser<Expression<Spans>> spansExpressionP) {
    return productP
        .field("kind", literalP("WindowsExpressionFromSpans"))
        .field("spansExpression", spansExpressionP)
        .map(
            untuple((kind, expr) -> new WindowsFromSpans(expr)),
            $ -> tuple(Unit.UNIT, $.expression()));
  }

  static JsonParser<ForEachActivitySpans> forEachActivitySpansF(final JsonParser<Expression<Spans>> spansExpressionP) {
    return productP
        .field("kind", literalP("ForEachActivitySpans"))
        .field("activityType", stringP)
        .field("alias", stringP)
        .field("expression", spansExpressionP)
        .map(
            untuple((kind, actType, alias, expression) -> new ForEachActivitySpans(actType, alias, expression)),
            $ -> tuple(Unit.UNIT, $.activityType, $.alias, $.expression));
  }

  static JsonParser<ForEachActivityViolations> forEachActivityViolationsF(final JsonParser<Expression<List<Violation>>> violationListExpressionP) {
    return productP
        .field("kind", literalP("ForEachActivityViolations"))
        .field("activityType", stringP)
        .field("alias", stringP)
        .field("expression", violationListExpressionP)
        .map(
            untuple((kind, actType, alias, expression) -> new ForEachActivityViolations(actType, alias, expression)),
            $ -> tuple(Unit.UNIT, $.activityType, $.alias, $.expression));
  }

  static final JsonParser<Changes<?>> changesP =
      productP
          .field("kind", literalP("ProfileChanges"))
          .field("expression", profileExpressionP)
          .map(
              untuple((kind, expression) -> new Changes<>(expression)),
              $ -> tuple(Unit.UNIT, $.expression));

  private static JsonParser<Expression<Windows>> windowsExpressionF(JsonParser<Expression<Spans>> spansP) {
    return recursiveP(selfP -> chooseP(
        startOfP,
        endOfP,
        changesP,
        lessThanP,
        lessThanOrEqualP,
        longerThanP(selfP),
        shorterThanP(selfP),
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
        shiftByF(selfP),
        startsF(selfP),
        endsF(selfP),
        windowsFromSpansF(spansP),
        activityWindowP,
        assignGapsF(selfP)
    ));
  }

  static JsonParser<SpansFromWindows> spansFromWindowsF(JsonParser<Expression<Windows>> windowsExpressionP) {
    return productP
        .field("kind", literalP("SpansExpressionFromWindows"))
        .field("windowsExpression", windowsExpressionP)
        .map(
            untuple((kind, expr) -> new SpansFromWindows(expr)),
            $ -> tuple(Unit.UNIT, $.expression()));
  }

  private static JsonParser<Expression<Spans>> spansExpressionF(JsonParser<Expression<Windows>> windowsP) {
      return recursiveP(selfP -> chooseP(
          startsF(selfP),
          endsF(selfP),
          splitF(selfP),
          splitF(windowsP),
          spansFromWindowsF(windowsP),
          forEachActivitySpansF(selfP),
          activitySpanP
          ));
  }

  public static final JsonParser<Expression<Windows>> windowsExpressionP = recursiveP(selfP -> windowsExpressionF(spansExpressionF(selfP)));

  public static final JsonParser<Expression<Spans>> spansExpressionP = recursiveP(selfP -> spansExpressionF(windowsExpressionF(selfP)));

  static final JsonParser<ViolationsOfWindows> violationsOfP =
      productP
          .field("kind", literalP("ViolationsOf"))
          .field("expression", windowsExpressionP)
          .map(
              untuple((kind, expression) -> new ViolationsOfWindows(expression)),
              $ -> tuple(Unit.UNIT, $.expression));

  public static final JsonParser<Expression<List<Violation>>> constraintP =
      recursiveP(selfP -> chooseP(
          forEachActivityViolationsF(selfP),
          windowsExpressionP.map(ViolationsOfWindows::new, $ -> $.expression),
          violationsOfP));
}
