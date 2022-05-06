import * as WindowsExpressions from "./windows-expressions-ast";

export interface ActivityTemplate {
  activityType: string,
  args: {[key: string]: any},
}

export enum NodeKind {
  ActivityRecurrenceGoal = 'ActivityRecurrenceGoal',
  ActivityCoexistenceGoal = 'ActivityCoexistenceGoal',
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
  ;
// TODO cardinality goal

export interface ActivityRecurrenceGoal {
  kind: NodeKind.ActivityRecurrenceGoal,
  activityTemplate: ActivityTemplate,
  interval: number,
}

export interface ActivityCoexistenceGoal {
  kind: NodeKind.ActivityCoexistenceGoal,
  activityTemplate: ActivityTemplate,
  forEach: WindowsExpressions.WindowsExpression
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
