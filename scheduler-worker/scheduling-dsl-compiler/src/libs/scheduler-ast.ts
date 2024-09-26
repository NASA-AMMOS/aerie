import type * as WindowsExpressions from "./constraints-ast.js";
import type * as ConstraintEDSL from "./constraints-edsl-fluent-api.js";

export interface ActivityTemplate<A extends ConstraintEDSL.Gen.ActivityType> {
  activityType: A,
  args: ConstraintEDSL.Gen.ActivityTypeParameterMap[A]
}

export interface ClosedOpenInterval {
  start: Temporal.Duration
  end: Temporal.Duration
}

export type CardinalityGoalArguments =
  |  { duration: Temporal.Duration, occurrence: number }
  |  { duration: Temporal.Duration }
  |  { occurrence: number }

export enum NodeKind {
  ActivityRecurrenceGoal = 'ActivityRecurrenceGoal',
  ActivityCoexistenceGoal = 'ActivityCoexistenceGoal',
  ActivityCardinalityGoal = 'ActivityCardinalityGoal',
  ActivityExpression = 'ActivityExpression',
  GoalAnd = 'GoalAnd',
  GoalOr = 'GoalOr',
  ApplyWhen = 'ApplyWhen',
  GlobalSchedulingCondition = 'GlobalSchedulingCondition',
  GlobalSchedulingConditionAnd = 'GlobalSchedulingConditionAnd'
}

export interface GlobalSchedulingCondition {
  kind : NodeKind.GlobalSchedulingCondition;
  expression : WindowsExpressions.WindowsExpression;
  activityTypes : ConstraintEDSL.Gen.ActivityType[]
}

export type GlobalSchedulingConditionSpecifier = GlobalSchedulingCondition | GlobalSchedulingConditionAnd

export interface GlobalSchedulingConditionAnd {
  kind: NodeKind.GlobalSchedulingConditionAnd,
  conditions: GlobalSchedulingConditionSpecifier[],
}

/**
 * Goal
 *
 * Action(Window[]) => void
 *
 * Operates on windows and causes some plan mutation
 */
export type Goal =
  | ActivityRecurrenceGoal
  | ActivityCoexistenceGoal
  | ActivityCardinalityGoal
  ;

export interface ActivityRecurrenceGoal {
  kind: NodeKind.ActivityRecurrenceGoal,
  activityTemplate: ActivityTemplate<any>,
  activityFinder: ActivityExpression<any> | undefined,
  separatedByAtLeast: Temporal.Duration,
  separatedByAtMost: Temporal.Duration,
  previousActivityStartedAt: Temporal.Duration | undefined,
  shouldRollbackIfUnsatisfied: boolean
}

export interface ActivityCardinalityGoal {
  kind: NodeKind.ActivityCardinalityGoal,
  activityTemplate: ActivityTemplate<any>,
  activityFinder: ActivityExpression<any> | undefined,
  specification: CardinalityGoalArguments,
  shouldRollbackIfUnsatisfied: boolean
}

export interface ActivityCoexistenceGoal {
  kind: NodeKind.ActivityCoexistenceGoal,
  activityTemplate: ActivityTemplate<any>,
  persistentAnchor: PersistentTimeAnchor | undefined,
  activityFinder: ActivityExpression<any> | undefined,
  alias: string,
  forEach: WindowsExpressions.WindowsExpression | ActivityExpression<any>,
  startConstraint: ActivityTimingConstraintSingleton | ActivityTimingConstraintRange | ActivityTimingConstraintInterval | undefined,
  endConstraint: ActivityTimingConstraintSingleton | ActivityTimingConstraintRange | ActivityTimingConstraintInterval | undefined,
  shouldRollbackIfUnsatisfied: boolean
}

export interface ActivityExpression<A extends ConstraintEDSL.Gen.ActivityType> {
  kind: NodeKind.ActivityExpression,
  type: string,
  matchingArguments: ConstraintEDSL.Gen.ActivityTypeParameterMapWithUndefined[A] | undefined
}

export type TimeExpression =
    | TimeExpressionRelativeSimple

export enum TimeExpressionOperator {
  Plus = "TimeExpressionOperatorPlus",
  Minus = "TimeExpressionOperatorMinus"
}

export enum TimeAnchor {
  Start = "Start",
  End = "End"
}

export enum PersistentTimeAnchor {
  DISABLED = "DISABLED",
  START = "START",
  END = "END"
}

export interface TimeExpressionRelativeSimple {
  anchor: TimeAnchor,
  fixed: boolean  // true means op(anchor, operand) is exactly the time, false means it's a range between op(anchor, operand) and anchor
  operation: {
    operator: TimeExpressionOperator,
    operand: Temporal.Duration
  }
}

export enum ActivityTimeProperty {
  START = 'START',
  END = 'END'
}

export enum WindowProperty {
  START = 'START',
  END = 'END'
}

export enum TimingConstraintOperator {
  PLUS = 'PLUS',
  MINUS = 'MINUS'
}

export interface ActivityTimingConstraintSingleton {
  windowProperty: WindowProperty;
  operator: TimingConstraintOperator;
  operand: Temporal.Duration
  singleton: true
}

export interface ActivityTimingConstraintRange {
  windowProperty: WindowProperty;
  operator: TimingConstraintOperator;
  operand: Temporal.Duration
  singleton: false
}

export interface ActivityTimingConstraintInterval {
  lowerBound: ActivityTimingConstraintSingleton;
  upperBound: ActivityTimingConstraintSingleton;
  singleton: false;
}
export type GoalSpecifier =
  | Goal
  | GoalComposition
  | GoalQualification
  ;


/**
 * Goal Composition
 *
 * Compose goals together
 */
export type GoalComposition =
  | GoalAnd
  | GoalOr
  ;

export interface GoalAnd {
  kind: NodeKind.GoalAnd,
  goals: GoalSpecifier[],
  shouldRollbackIfUnsatisfied: boolean
}

export interface GoalOr {
  kind: NodeKind.GoalOr,
  goals: GoalSpecifier[],
  shouldRollbackIfUnsatisfied: boolean
}

/**
 * Goal Qualification
 *
 * Modify goals
 */
export type GoalQualification = ApplyWhen;

export interface ApplyWhen {
  kind: NodeKind.ApplyWhen,
  goal: GoalSpecifier,
  window: WindowsExpressions.WindowsExpression
}
