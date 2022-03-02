export type U<BitLength extends 8 | 16 | 32 | 64> = number;
export type U8 = U<8>;
export type U16 = U<16>;
export type U32 = U<32>;
export type U64 = U<64>;
export type I<BitLength extends 8 | 16 | 32 | 64> = number;
export type I8 = I<8>;
export type I16 = I<16>;
export type I32 = I<32>;
export type I64 = string;
export type VarString<PrefixBitLength extends number, MaxBitLength extends number> = string;
export type F<BitLength extends 32 | 64> = number;
export type F32 = F<32>;
export type F64 = F<64>;
export type Duration = F64;
export type Integer = I32;
export type Double = F64;
export type Time = string;

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
  interval: Integer,
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

