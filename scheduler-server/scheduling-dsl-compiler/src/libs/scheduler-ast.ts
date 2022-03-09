declare global {
  type U<BitLength extends 8 | 16 | 32 | 64> = number;
  type U8 = U<8>;
  type U16 = U<16>;
  type U32 = U<32>;
  type U64 = U<64>;
  type I<BitLength extends 8 | 16 | 32 | 64> = number;
  type I8 = I<8>;
  type I16 = I<16>;
  type I32 = I<32>;
  type I64 = string;
  type VarString<PrefixBitLength extends number, MaxBitLength extends number> = string;
  type F<BitLength extends 32 | 64> = number;
  type F32 = F<32>;
  type F64 = F<64>;
  type Duration = F64;
  type Integer = I32;
  type Double = F64;
  type Time = string;
}

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

