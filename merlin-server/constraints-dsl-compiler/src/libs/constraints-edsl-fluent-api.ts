/**
 * This module contains the elements you need to write constraints via {@link Constraint}, window expressions via {@link Windows} and {@link Spans}.
 * Resource can be accessed via {@link Real} or {@link Discrete} depending on their types.
 *
 * As activity types and resources are generated per mission model, some elements here are shown here with generic non-accessible types.
 *
 * @module Constraint eDSL
 * @packageDocumentation
 */

import * as AST from './constraints-ast.js';
import type * as Gen from './mission-model-generated-code.js';
import {ActivityType, ActivityTypeParameterMap} from "./mission-model-generated-code.js";

export class Constraint {
  /** Internal AST node */
  /** @internal **/
  public readonly __astNode: AST.Constraint;

  /** @internal **/
  private static __numGeneratedAliases: number = 0;

  /** @internal **/
  public constructor(astNode: AST.Constraint) {
    this.__astNode = astNode;
  }

  /**
   * Forbid instances of two activity types from overlapping with each other.
   * @param activityType1
   * @param activityType2
   * @constructor
   */
  public static ForbiddenActivityOverlap(activityType1: Gen.ActivityType, activityType2: Gen.ActivityType): Constraint {
    return Constraint.ForEachActivity(
        activityType1,
        activity1 => Constraint.ForEachActivity(
            activityType2,
            activity2 => Windows.And(activity1.window(), activity2.window()).not()
        )
    )
  }

  /**
   * Check a constraint for each instance of an activity type.
   *
   * @param activityType activity type to check
   * @param expression function of an activity instance that returns a Constraint
   * @constructor
   */
  public static ForEachActivity<A extends Gen.ActivityType>(
    activityType: A,
    expression: (instance: ActivityInstance<A>) => Constraint,
  ): Constraint {
    let alias = 'activity alias ' + Constraint.__numGeneratedAliases;
    Constraint.__numGeneratedAliases += 1;
    return new Constraint({
      kind: AST.NodeKind.ForEachActivity,
      activityType,
      alias,
      expression: expression(new ActivityInstance(activityType, alias)).__astNode,
    });
  }
}

export class Windows {
  /** Internal AST node */
  /** @internal **/
  public readonly __astNode: AST.WindowsExpression;

  /** @internal **/
  public constructor(expression: AST.WindowsExpression) {
    this.__astNode = expression;
  }

  /**
   * Produce a window when all arguments produce a window.
   *
   * Performs the intersection of the argument windows.
   *
   * @param windows any number of windows expressions
   */
  public static And(...windows: Windows[]): Windows {
    return new Windows({
      kind: AST.NodeKind.WindowsExpressionAnd,
      expressions: [...windows.map(other => other.__astNode)],
    });
  }

  /**
   * Produce a window when any argument produces a window.
   *
   * Performs the union of the argument windows.
   *
   * @param windows one or more windows expressions
   */
  public static Or(...windows: Windows[]): Windows {
    return new Windows({
      kind: AST.NodeKind.WindowsExpressionOr,
      expressions: [...windows.map(other => other.__astNode)],
    });
  }

  /**
   * Only check this expression of the condition argument produces a window.
   *
   * @param condition
   */
  public if(condition: Windows): Windows {
    return new Windows({
      kind: AST.NodeKind.WindowsExpressionOr,
      expressions: [
        {
          kind: AST.NodeKind.WindowsExpressionNot,
          expression: condition.__astNode,
        },
        this.__astNode,
      ],
    });
  }

  /**
   * Invert all the windows produced this.
   */
  public and(...windows: Windows[]): Windows {
    return Windows.And(this, ...windows);
  }

  public or(...windows: Windows[]): Windows {
    return Windows.Or(this, ...windows);
  }
   */
  public not(): Windows {
    return new Windows({
      kind: AST.NodeKind.WindowsExpressionNot,
      expression: this.__astNode,
    });
  }

  /**
   * Produce a constraint violation whenever this does NOT produce a window.
   *
   * Essentially, express the condition you want to be satisfied, then use
   * this method to produce a violation whenever it is NOT satisfied.
   */
  public violations(): Constraint {
    return new Constraint({
      kind: AST.NodeKind.ViolationsOf,
      expression: this.__astNode,
    });
  }

  /**
   *  Return all windows with a duration longer than the argument
   * @param duration the duration
   */
  public longerThan(duration: Duration) : Windows {
    return new Windows({
      kind: AST.NodeKind.WindowsExpressionLongerThan,
      windowExpression: this.__astNode,
      duration: duration
    })
  }

  /**
   * Returns all windows with a duration shorter than the argument
   * @param duration the duration
   */
  public shorterThan(duration: Duration) : Windows {
    return new Windows({
      kind: AST.NodeKind.WindowsExpressionShorterhan,
      windowExpression: this.__astNode,
      duration: duration
    })
  }

  /**
   * Shift start and end of all windows by a duration
   * @param fromStart duration to add from the start of the window
   * @param fromEnd duration to add from the end of the window
   */
  public shiftBy(fromStart: Duration, fromEnd: Duration) : Windows {
    return new Windows({
      kind: AST.NodeKind.WindowsExpressionShiftBy,
      windowExpression: this.__astNode,
      fromStart: fromStart,
      fromEnd: fromEnd
    })
  }

  /**
   * Splits each window into equal sized sub-intervals. Returns a Spans object.
   *
   * For `.split(N)`, N sub-windows will be created by removing N-1 points in the middle.
   *
   * @throws UnsplittableSpanException during backend evaluation if the duration of a window is fewer microseconds than N.
   * @throws UnsplittableSpanException if any window is unbounded (i.e. contains MIN_VALUE or MAX_VALUE)
   * @throws InvalidGapsException if this contains any gaps.
   *
   * @param numberOfSubSpans how many sub-windows to split each window into
   * @param internalStartInclusivity Inclusivity for any newly generated span start points (default Inclusive).
   * @param internalEndInclusivity Inclusivity for any newly generated span end points (default Exclusive).
   */
  public split(
      numberOfSubSpans: number,
      internalStartInclusivity: Inclusivity = Inclusivity.Inclusive,
      internalEndInclusivity: Inclusivity = Inclusivity.Exclusive
  ): Spans {
    if (numberOfSubSpans < 1) {
      throw RangeError(".split numberOfSubSpans cannot be less than 1, but was: " + numberOfSubSpans);
    }
    return new Spans({
      kind: AST.NodeKind.SpansExpressionSplit,
      intervals: this.__astNode,
      numberOfSubIntervals: numberOfSubSpans,
      internalStartInclusivity: internalStartInclusivity,
      internalEndInclusivity: internalEndInclusivity
    })
  }

  /**
   * Replaces each window with its start point.
   *
   * Since gaps represent "unknown", true segments that come after a gap don't have a known start point.
   * So instead their first known point is unset and the rest is set to false.
   *
   * True segments that explicitly come directly after false and include their start point have all except their
   * start point set to false. If they don't include the start point, then the whole interval is set to false and the
   * start point is set true.
   */
  public starts(): Windows {
    return new Windows({
      kind: AST.NodeKind.IntervalsExpressionStarts,
      expression: this.__astNode
    })
  }

  /**
   * Replaces each window with its end point.
   *
   * Since gaps represent "unknown", true segments that come before a gap don't have a known end point.
   * So instead their last known point is unset and the rest is set to false.
   *
   * True segments that explicitly come directly before false and include their end point have all except their
   * end point set to false. If they don't include the end point, then the whole interval is set to false and the
   * end point is set true.
   */
  public ends(): Windows {
    return new Windows({
      kind: AST.NodeKind.IntervalsExpressionEnds,
      expression: this.__astNode
    })
  }

  /**
   * Convert this into a set of Spans.
   *
   * @throws InvalidGapsException if this contains any gaps.
   */
  public spans(): Spans {
    return new Spans({
      kind: AST.NodeKind.SpansExpressionFromWindows,
      windowsExpression: this.__astNode
    })
  }
}

export class Spans {
  /** @internal **/
  public readonly __astNode: AST.SpansExpression;

  /** @internal **/
  public constructor(expression: AST.SpansExpression) {
    this.__astNode = expression;
  }
  /**
   * Splits each span into equal sized sub-spans.
   *
   * For `.split(N)`, N sub-spans will be created by removing N-1 points in the middle.
   *
   * @throws UnsplittableIntervalException during backend evaluation if the duration of a span is fewer microseconds than N.
   * @param numberOfSubSpans how many sub-spans to split each span into
   * @param internalStartInclusivity Inclusivity for any newly generated span start points (default Inclusive).
   * @param internalEndInclusivity Inclusivity for any newly generated span end points (default Exclusive).
   */
  public split(
      numberOfSubSpans: number,
      internalStartInclusivity: Inclusivity = Inclusivity.Inclusive,
      internalEndInclusivity: Inclusivity = Inclusivity.Exclusive
  ): Spans {

    if (numberOfSubSpans < 1) {
      throw RangeError(".split numberOfSubSpans cannot be less than 1, but was: " + numberOfSubSpans);
    }
    return new Spans({
      kind: AST.NodeKind.SpansExpressionSplit,
      intervals: this.__astNode,
      numberOfSubIntervals: numberOfSubSpans,
      internalStartInclusivity: internalStartInclusivity,
      internalEndInclusivity: internalEndInclusivity
    })
  }

  /**
   * Replaces each Span with its start point.
   */
  public starts(): Spans {
    return new Spans({
      kind: AST.NodeKind.IntervalsExpressionStarts,
      expression: this.__astNode
    })
  }

  /**
   * Replaces each Span with its end point.
   */
  public ends(): Spans {
    return new Spans({
      kind: AST.NodeKind.IntervalsExpressionEnds,
      expression: this.__astNode
    })
  }

  /**
   * Convert this into a set of Windows.
   *
   * This is a lossy operation.
   * If any spans overlap or touch, they will be coalesced into a single window.
   */
  public windows(): Windows {
    return new Windows({
      kind: AST.NodeKind.WindowsExpressionFromSpans,
      spansExpression: this.__astNode
    })
  }
}

export class Real {
  /** @internal **/
  public readonly __astNode: AST.RealProfileExpression;

  /** @internal **/
  public constructor(profile: AST.RealProfileExpression) {
    this.__astNode = profile;
  }

  /**
   * Reference the real profile associated with a resource.
   * @param name
   * @constructor
   */
  public static Resource(name: Gen.RealResourceName): Real {
    return new Real({
      kind: AST.NodeKind.RealProfileResource,
      name,
    });
  }

  /**
   * Create a constant real profile for all time.
   * @param value
   * @constructor
   */
  public static Value(value: number): Real {
    return new Real({
      kind: AST.NodeKind.RealProfileValue,
      value,
    });
  }

  /**
   * Create a real profile from this profile's derivative.
   */
  public rate(): Real {
    return new Real({
      kind: AST.NodeKind.RealProfileRate,
      profile: this.__astNode,
    });
  }

  /**
   * Create a real profile by multiplying this profile by a constant
   * @param multiplier
   */
  public times(multiplier: number): Real {
    return new Real({
      kind: AST.NodeKind.RealProfileTimes,
      multiplier,
      profile: this.__astNode,
    });
  }

  /**
   * Create a real profile by adding this and another real profile together.
   * @param other
   */
  public plus(other: Real | number): Real {
    if (!(other instanceof Real)) {
      other = Real.Value(other);
    }
    return new Real({
      kind: AST.NodeKind.RealProfilePlus,
      left: this.__astNode,
      right: other.__astNode,
    });
  }

  /**
   * Produce a window whenever this profile is less than another real profile.
   * @param other
   */
  public lessThan(other: Real | number): Windows {
    if (!(other instanceof Real)) {
      other = Real.Value(other);
    }
    return new Windows({
      kind: AST.NodeKind.RealProfileLessThan,
      left: this.__astNode,
      right: other.__astNode,
    });
  }

  /**
   * Produce a window whenever this profile is less than or equal to another real profile.
   * @param other
   */
  public lessThanOrEqual(other: Real | number): Windows {
    if (!(other instanceof Real)) {
      other = Real.Value(other);
    }
    return new Windows({
      kind: AST.NodeKind.RealProfileLessThanOrEqual,
      left: this.__astNode,
      right: other.__astNode,
    });
  }

  /**
   * Produce a window whenever this profile is greater than to another real profile.
   * @param other
   */
  public greaterThan(other: Real | number): Windows {
    if (!(other instanceof Real)) {
      other = Real.Value(other);
    }
    return new Windows({
      kind: AST.NodeKind.RealProfileGreaterThan,
      left: this.__astNode,
      right: other.__astNode,
    });
  }

  /**
   * Produce a window whenever this profile is greater than or equal to another real profile.
   * @param other
   */
  public greaterThanOrEqual(other: Real | number): Windows {
    if (!(other instanceof Real)) {
      other = Real.Value(other);
    }
    return new Windows({
      kind: AST.NodeKind.RealProfileGreaterThanOrEqual,
      left: this.__astNode,
      right: other.__astNode,
    });
  }

  /**
   * Produce a window whenever this profile is equal to another real profile.
   * @param other
   */
  public equal(other: Real | number): Windows {
    if (!(other instanceof Real)) {
      other = Real.Value(other);
    }
    return new Windows({
      kind: AST.NodeKind.ExpressionEqual,
      left: this.__astNode,
      right: other.__astNode,
    });
  }

  /**
   * Produce a window whenever this profile is not equal to another real profile.
   * @param other
   */
  public notEqual(other: Real | number): Windows {
    if (!(other instanceof Real)) {
      other = Real.Value(other);
    }
    return new Windows({
      kind: AST.NodeKind.ExpressionNotEqual,
      left: this.__astNode,
      right: other.__astNode,
    });
  }

  /**
   * Produce an instantaneous window whenever this profile changes.
   */
  public changes(): Windows {
    return new Windows({
      kind: AST.NodeKind.ProfileChanges,
      expression: this.__astNode,
    });
  }
}

export class Discrete<Schema> {
  /** @internal **/
  public readonly __astNode: AST.DiscreteProfileExpression;

  /** @internal **/
  public constructor(profile: AST.DiscreteProfileExpression) {
    this.__astNode = profile;
  }

  /**
   * Reference the discrete profile associated with a resource.
   * @param name
   * @constructor
   */
  public static Resource<R extends Gen.ResourceName>(name: R): Discrete<Gen.Resource[R]> {
    return new Discrete({
      kind: AST.NodeKind.DiscreteProfileResource,
      name,
    });
  }

  /**
   * Create a constant discrete profile for all time.
   * @param value
   * @constructor
   */
  public static Value<Schema>(value: Schema): Discrete<Schema> {
    return new Discrete({
      kind: AST.NodeKind.DiscreteProfileValue,
      value,
    });
  }

  /**
   * Produce an instantaneous window whenever this profile makes a specific transition.
   *
   * @param from initial value
   * @param to final value
   */
  public transition(from: Schema, to: Schema): Windows {
    return new Windows({
      kind: AST.NodeKind.DiscreteProfileTransition,
      profile: this.__astNode,
      from,
      to,
    });
  }

  /**
   * Produce a window whenever this profile is equal to another discrete profile.
   * @param other
   */
  public equal(other: Schema | Discrete<Schema>): Windows {
    if (!(other instanceof Discrete)) {
      other = Discrete.Value(other);
    }
    return new Windows({
      kind: AST.NodeKind.ExpressionEqual,
      left: this.__astNode,
      right: other.__astNode,
    });
  }

  /**
   * Produce a window whenever this profile is not equal to another discrete profile.
   * @param other
   */
  public notEqual(other: Schema | Discrete<Schema>): Windows {
    if (!(other instanceof Discrete)) {
      other = Discrete.Value(other);
    }
    return new Windows({
      kind: AST.NodeKind.ExpressionNotEqual,
      left: this.__astNode,
      right: other.__astNode,
    });
  }

  /**
   * Produce an instantaneous window whenever this profile changes.
   */
  public changes(): Windows {
    return new Windows({
      kind: AST.NodeKind.ProfileChanges,
      expression: this.__astNode,
    });
  }
}

export class ActivityInstance<A extends ActivityType> {

  private readonly __activityType: A;
  private readonly __alias: string;
  public readonly parameters: ReturnType<typeof ActivityTypeParameterMap[A]>;

  constructor(activityType: A, alias: string) {
    this.__activityType = activityType;
    this.__alias = alias;
    this.parameters = ActivityTypeParameterMap[activityType](alias) as ReturnType<typeof ActivityTypeParameterMap[A]>;
  }

  /**
   * Produces a window for the duration of the activity.
   */
  public window(): Windows {
    return new Windows({
      kind: AST.NodeKind.WindowsExpressionActivityWindow,
      alias: this.__alias
    });
  }
  /**
   * Produces an instantaneous window at the start of the activity.
   */
  public start(): Windows {
    return new Windows({
      kind: AST.NodeKind.WindowsExpressionStartOf,
      alias: this.__alias
    });
  }
  /**
   * Produces an instantaneous window at the end of the activity.
   */
  public end(): Windows {
    return new Windows({
      kind: AST.NodeKind.WindowsExpressionEndOf,
      alias: this.__alias
    });
  }
}

export enum Inclusivity {
  Inclusive = "Inclusive",
  Exclusive = "Exclusive"
}

declare global {
  export class Constraint {
    /** Internal AST Node */
    public readonly __astNode: AST.Constraint;

    /**
     * Forbid instances of two activity types from overlapping with each other.
     * @param activityType1
     * @param activityType2
     * @constructor
     */
    public static ForbiddenActivityOverlap(
      activityType1: Gen.ActivityType,
      activityType2: Gen.ActivityType,
    ): Constraint;

    /**
     * Check a constraint for each instance of an activity type.
     *
     * @param activityType activity type to check
     * @param expression function of an activity instance that returns a Constraint
     * @constructor
     */
    public static ForEachActivity<A extends Gen.ActivityType>(
      activityType: A,
      expression: (instance: ActivityInstance<A>) => Constraint,
    ): Constraint;
  }

  /**
   * A set of intervals that coalesces overlapping intervals.
   */
  export class Windows {
    /** Internal AST Node */
    public readonly __astNode: AST.WindowsExpression;

    /**
     * Produce a window when all arguments produce a window.
     *
     * Performs the intersection of the argument windows.
     *
     * @param windows any number of windows expressions
     */
    public static And(...windows: Windows[]): Windows;

    /**
     * Produce a window when any argument produces a window.
     *
     * Performs the union of the argument windows.
     *
     * @param windows one or more windows expressions
     */
    public static Or(...windows: Windows[]): Windows;

    /**
     * Only check this expression of the condition argument produces a window.
     *
     * @param condition
     */
    public if(condition: Windows): Windows;

    /**
     * Invert all the windows produced this.
     */
    public and(...windows: Windows[]): Windows;

    public or(...windows: Windows[]): Windows;

    public not(): Windows;

    /**
     * Produce a constraint violation whenever this does NOT produce a window.
     *
     * Essentially, express the condition you want to be satisfied, then use
     * this method to produce a violation whenever it is NOT satisfied.
     */
    public violations(): Constraint;

    /**
     * Shift start and end of all windows by a duration
     * @param fromStart duration to add from the start of the window
     * @param fromEnd duration to add from the end of the window
     */
    public shiftBy(fromStart: number, fromEnd: number): Windows;

    /**
     * Return all windows with a duration longer than the argument
     * @param duration the duration
     */
    public longerThan(duration: Duration): Windows;

    /**
     * Returns all windows with a duration shorter than the argument
     * @param duration the duration
     */
    public shorterThan(duration: Duration): Windows;

    /**
     * Splits each window into equal sized sub-intervals. Returns a Spans object.
     *
     * For `.split(N)`, N sub-windows will be created by removing N-1 points in the middle.
     *
     * @throws UnsplittableSpanException during backend evaluation if the duration of a window is fewer microseconds than N.
     * @throws UnsplittableSpanException if any window is unbounded (i.e. contains MIN_VALUE or MAX_VALUE)
     * @throws InvalidGapsException if this contains any gaps.
     *
     * @param numberOfSubSpans how many sub-windows to split each window into
     * @param internalStartInclusivity Inclusivity for any newly generated span start points (default Inclusive).
     * @param internalEndInclusivity Inclusivity for any newly generated span end points (default Exclusive).
     */
    public split(numberOfSubSpans: number, internalStartInclusivity?: Inclusivity, internalEndInclusivity?: Inclusivity): Spans;

    /**
     * Replaces each window with its start point.
     *
     * Since gaps represent "unknown", true segments that come after a gap don't have a known start point.
     * So instead their first known point is unset and the rest is set to false.
     *
     * True segments that explicitly come directly after false and include their start point have all except their
     * start point set to false. If they don't include the start point, then the whole interval is set to false and the
     * start point is set true.
     */
    public starts(): Windows;

    /**
     * Replaces each window with its end point.
     *
     * Since gaps represent "unknown", true segments that come before a gap don't have a known end point.
     * So instead their last known point is unset and the rest is set to false.
     *
     * True segments that explicitly come directly before false and include their end point have all except their
     * end point set to false. If they don't include the end point, then the whole interval is set to false and the
     * end point is set true.
     */
    public ends(): Windows;

    /**
     * Convert this into a set of Spans.
     *
     * @throws InvalidGapsException if this contains any gaps.
     */
    public spans(): Spans;
  }

  /**
   * A set of intervals that can overlap without being coaleseced together.
   */
  export class Spans {
    public readonly __astNode: AST.SpansExpression;

    /**
     * Returns the instantaneous start points of the these spans.
     */
    public starts(): Spans;

    /**
     * Returns the instantaneous end points of the these spans.
     */
    public ends(): Spans;

    /**
     * Splits each span into equal sized sub-spans.
     *
     * For `.split(N)`, N sub-spans will be created by removing N-1 points in the middle.
     *
     * @throws UnsplittableIntervalException during backend evaluation if the duration of a span is fewer microseconds than N.
     * @param numberOfSubSpans how many sub-spans to split each span into
     * @param internalStartInclusivity Inclusivity for any newly generated span start points (default Inclusive).
     * @param internalEndInclusivity Inclusivity for any newly generated span end points (default Exclusive).
     */
    public split(numberOfSubSpans: number, internalStartInclusivity?: Inclusivity, internalEndInclusivity?: Inclusivity): Spans;

    /**
     * Convert this into a set of Windows.
     *
     * This is a lossy operation.
     * If any spans overlap or touch, they will be coalesced into a single window.
     */
    public windows(): Windows;
  }

  export class Real {
    /** Internal AST Node */
    public readonly __astNode: AST.RealProfileExpression;

    /**
     * Reference the real profile associated with a resource.
     * @param name
     * @constructor
     */
    public static Resource(name: Gen.RealResourceName): Real;

    /**
     * Create a constant real profile for all time.
     * @param value
     * @constructor
     */
    public static Value(value: number): Real;

    /**
     * Create a real profile from this profile's derivative.
     */
    public rate(): Real;

    /**
     * Create a real profile by multiplying this profile by a constant
     * @param multiplier
     */
    public times(multiplier: number): Real;

    /**
     * Create a real profile by adding this and another real profile together.
     * @param other
     */
    public plus(other: Real | number): Real;

    /**
     * Produce a window whenever this profile is less than another real profile.
     * @param other
     */
    public lessThan(other: Real | number): Windows;

    /**
     * Produce a window whenever this profile is less than or equal to another real profile.
     * @param other
     */
    public lessThanOrEqual(other: Real | number): Windows;

    /**
     * Produce a window whenever this profile is greater than to another real profile.
     * @param other
     */
    public greaterThan(other: Real | number): Windows;

    /**
     * Produce a window whenever this profile is greater than or equal to another real profile.
     * @param other
     */
    public greaterThanOrEqual(other: Real | number): Windows;

    /**
     * Produce a window whenever this profile is equal to another real profile.
     * @param other
     */
    public equal(other: Real | number): Windows;

    /**
     * Produce a window whenever this profile is not equal to another real profile.
     * @param other
     */
    public notEqual(other: Real | number): Windows;

    /**
     * Produce an instantaneous window whenever this profile changes.
     */
    public changes(): Windows;
  }

  export class Discrete<Schema> {
    /** Internal AST Node */
    public readonly __astNode: AST.DiscreteProfileExpression;

    /**
     * Internal instance of the Schema type, for type checking.
     *
     * It is never assigned or accessed, and is discarded by the end.
     * This field should remain `undefined` for the full runtime.
     * It does not even exist in the above implementation class
     * for Discrete.
     *
     * Don't remove it though, it'll break the tests.
     * @private
     */
    private readonly __schemaInstance: Schema;

    /**
     * Reference the discrete profile associated with a resource.
     * @param name
     * @constructor
     */
    public static Resource<R extends Gen.ResourceName>(name: R): Discrete<Gen.Resource[R]>;

    /**
     * Create a constant discrete profile for all time.
     * @param value
     * @constructor
     */
    public static Value<Schema>(value: Schema): Discrete<Schema>;

    /**
     * Produce an instantaneous window whenever this profile makes a specific transition.
     *
     * @param from initial value
     * @param to final value
     */
    public transition(from: Schema, to: Schema): Windows;

    /**
     * Produce a window whenever this profile is equal to another discrete profile.
     * @param other
     */
    public equal(other: Schema | Discrete<Schema>): Windows;

    /**
     * Produce a window whenever this profile is not equal to another discrete profile.
     * @param other
     */
    public notEqual(other: Schema | Discrete<Schema>): Windows;

    /**
     * Produce an instantaneous window whenever this profile changes.
     */
    public changes(): Windows;
  }

  type Duration = number;

  enum Inclusivity {
    Inclusive = "Inclusive",
    Exclusive = "Exclusive"
  }
}

// Make Constraint available on the global object
Object.assign(globalThis, { Constraint, Windows, Spans, Real, Discrete, Inclusivity });
