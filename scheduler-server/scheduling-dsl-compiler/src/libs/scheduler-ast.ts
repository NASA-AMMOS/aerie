import type * as WindowsExpressions from "./constraints-ast";

export interface ActivityTemplate {
  activityType: string,
  args: {[key: string]: any},
}

export interface ClosedOpenInterval {
  start: number
  end: number
}

export type CardinalityGoalArguments =
  |  { duration: number, occurrence: number }
  |  { duration: number }
  |  { occurrence: number }

export enum NodeKind {
  ActivityRecurrenceGoal = 'ActivityRecurrenceGoal',
  ActivityCoexistenceGoal = 'ActivityCoexistenceGoal',
  ActivityCardinalityGoal = 'ActivityCardinalityGoal',
  ActivityExpression = 'ActivityExpression',
  GoalAnd = 'GoalAnd',
  GoalOr = 'GoalOr'
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
  activityTemplate: ActivityTemplate,
  interval: number,
}

export interface ActivityCardinalityGoal {
  kind: NodeKind.ActivityCardinalityGoal,
  activityTemplate: ActivityTemplate,
  specification: CardinalityGoalArguments,
  inPeriod: ClosedOpenInterval,
}

export interface ActivityCoexistenceGoal {
  kind: NodeKind.ActivityCoexistenceGoal,
  activityTemplate: ActivityTemplate,
  forEach: WindowsExpressions.WindowsExpression | ActivityExpression
}

export interface ActivityExpression {
  kind: NodeKind.ActivityExpression
  type: string
}

export type GoalSpecifier =
  | Goal
  | GoalComposition
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
}

export interface GoalOr {
  kind: NodeKind.GoalOr,
  goals: GoalSpecifier[],
}
