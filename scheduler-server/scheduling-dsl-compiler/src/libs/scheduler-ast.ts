export interface ActivityTemplate {
  name: string,
  activityType: string,
  args: {[key: string]: any},
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
  ;
// TODO coexistence goal
// TODO cardinality goal

export interface ActivityRecurrenceGoal {
  kind: 'ActivityRecurrenceGoal',
  activityTemplate: ActivityTemplate,
  interval: number,
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
  kind: 'GoalAnd',
  goals: GoalSpecifier[],
}

export interface GoalOr {
  kind: 'GoalOr',
  goals: GoalSpecifier[],
}

