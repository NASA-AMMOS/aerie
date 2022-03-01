export type ResourceValueTypes = Integer | Double | string | boolean;
export type WindowPropertyValueTypes = Integer | Double | string | boolean;


export interface Resource<Type extends ResourceValueTypes> extends String {}
export interface WindowProperty<Type extends WindowPropertyValueTypes> extends String {}

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

/**
 * Constraint Operators
 *
 * ConstraintOperator(Resource) => WindowSet
 *
 * Operates on symbols representing resources and "returns" a WindowSet
 */
export type ConstraintOperator<T extends ResourceValueTypes = ResourceValueTypes> =
  | ConstraintOperatorEqual<T>
  | ConstraintOperatorLessThan<T>
  | ConstraintOperatorGreaterThan<T>
  | ConstraintOperatorConstantValuedWindow<T>
  | ConstraintOperatorEntirePlanWindow
  ;

export interface ConstraintOperatorEqual<T extends ResourceValueTypes> {
  kind: 'ConstraintOperatorEqual',
  left: Resource<T>,
  right: T,
}

export interface ConstraintOperatorLessThan<T extends ResourceValueTypes> {
  kind: 'ConstraintOperatorLessThan',
  left: Resource<T>,
  right: T,
}

export interface ConstraintOperatorGreaterThan<T extends ResourceValueTypes> {
  kind: 'ConstraintOperatorGreaterThan',
  left: Resource<T>,
  right: T,
}

export interface ConstraintOperatorConstantValuedWindow<T extends ResourceValueTypes> {
  kind: 'ConstraintOperatorConstantValuedWindow',
  resource: Resource<T>,
}

export interface ConstraintOperatorEntirePlanWindow {
  kind: 'ConstraintOperatorEntirePlanWindow'
}


/**
 * Window Set Operators
 *
 * WindowSetOperator(WindowSet) => WindowSet
 *
 * Operates on window sets and produces a new window set
 */
export type WindowSetCombineOperator =
  | WindowSetCombineOperatorIntersection
  | WindowSetCombineOperatorUnion
  | WindowSetCombineOperatorDifference
  ;

export interface WindowSetCombineOperatorIntersection {
  kind: 'WindowSetCombineOperatorIntersection',
  expressions: WindowSetSpecifier[]
}

export interface WindowSetCombineOperatorUnion {
  kind: 'WindowSetCombineOperatorUnion',
  expressions: WindowSetSpecifier[]
}

export interface WindowSetCombineOperatorDifference {
  kind: 'WindowSetCombineOperatorDifference',
  left: WindowSetSpecifier,
  right: WindowSetSpecifier,
}

/**
 * Window Filter Operators
 *
 * WindowFilterOperator(WindowSet) => WindowSet
 *
 * Operates on windows and produces a new window set
 */
export type WindowSetFilterOperator<T extends WindowPropertyValueTypes = ResourceValueTypes> =
  | WindowSetFilterOperatorEqual<T>
  | WindowSetFilterOperatorLessThan<T>
  | WindowSetFilterOperatorGreaterThan<T>
  ;

// latching filter


export interface WindowSetFilterOperatorEqual<T extends WindowPropertyValueTypes> {
  kind: 'WindowSetFilterOperatorEqual',
  windows: WindowSetSpecifier,
  left: WindowProperty<T>,
  right: T,
}

export interface WindowSetFilterOperatorLessThan<T extends WindowPropertyValueTypes> {
  kind: 'WindowSetFilterOperatorLessThan',
  windows: WindowSetSpecifier,
  left: WindowProperty<T>,
  right: T,
}

export interface WindowSetFilterOperatorGreaterThan<T extends WindowPropertyValueTypes> {
  kind: 'WindowSetFilterOperatorGreaterThan',
  windows: WindowSetSpecifier,
  left: WindowProperty<T>,
  right: T,
}

export type WindowSetFilterModificationOperator =
  | WindowSetFilterModificationOperatorAnd
  | WindowSetFilterModificationOperatorOr
  | WindowSetFilterModificationOperatorNot
  ;

export interface WindowSetFilterModificationOperatorAnd {
  kind: 'WindowSetFilterModificationOperatorAnd',
  expressions: WindowSetFilterOperator[],
}

export interface WindowSetFilterModificationOperatorOr {
  kind: 'WindowSetFilterModificationOperatorOr',
  expressions: WindowSetFilterOperator[],
}

export interface WindowSetFilterModificationOperatorNot {
  kind: 'WindowSetFilterModificationOperatorNot',
  expression: WindowSetFilterOperator,
}

/**
 * Window Map Operators
 *
 * WindowMapOperator(WindowSet) => WindowSet
 */
export type WindowSetMapOperator =
  | WindowSetMapOperatorSplit
  | WindowSetMapOperatorShift
  | WindowSetMapOperatorTrim
  | WindowSetMapOperatorExtend
  ;

export interface WindowSetMapOperatorSplit {
  kind: 'WindowSetMapOperatorSplit',
  windows: WindowSetSpecifier,
  duration: DurationSpecifier,
}

export interface WindowSetMapOperatorShift {
  kind: 'WindowSetMapOperatorShift',
  windows: WindowSetSpecifier,
  duration: DurationSpecifier,
}

export interface WindowSetMapOperatorTrim {
  kind: 'WindowSetMapOperatorTrim',
  windows: WindowSetSpecifier,
  duration: DurationSpecifier,
}

export interface WindowSetMapOperatorExtend {
  kind: 'WindowSetMapOperatorExtend',
  windows: WindowSetSpecifier,
  duration: DurationSpecifier,
}

/**
 * Window Reduce Operators
 *
 * WindowReduceOperator(WindowSet) => WindowSet
 */
export type WindowSetReduceOperator =
  | unknown
  ;

/** Anything that produces a window set */
export type WindowSetSpecifier =
  | ConstraintOperator
  | WindowSetCombineOperator
  | WindowSetFilterOperator
  | WindowSetMapOperator
  // | WindowSetReduceOperator
  | WindowSetFilterModificationOperator
  ;


export interface ActivityTemplate {
  name: string,
  activityType: string,
  arguments: {[key: string]: any},
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
// coexistance goal
// cardinality goal

export interface ActivityRecurrenceGoal {
  kind: 'ActivityRecurrenceGoal',
  windows: WindowSetSpecifier,
  activityTemplate: ActivityTemplate,
  interval: Integer,
}

export interface CoexistenceGoal {
  kind: 'CoexistenceGoal',
  windows: WindowSetSpecifier,
  //...
  // activity: ActivityType,
  // resource: ResourceType,
}

export interface CardinalityGoal {
  kind: 'CardinalityGoal',
  windows: WindowSetSpecifier,
  //....
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

// Time Expressions - mapping and activity templates
export type TimeSpecifier =
  | TimeOperator
  | Time
  ;

export type TimeOperator =
  | TimeOperatorAdd
  | TimeOperatorActivityStartTime
  | TimeOperatorActivityEndTime
  ;

export interface TimeOperatorAdd {
  kind: 'TimeOperatorAdd',
  left: TimeSpecifier,
  right: DurationSpecifier,
}

export interface TimeOperatorActivityStartTime {
  kind: 'TimeOperatorActivityStartTime',
}

export interface TimeOperatorActivityEndTime {
  kind: 'TimeOperatorActivityEndTime',
}

// Time Expressions
export type DurationSpecifier =
  | DurationOperator
  | Duration
  ;

export type DurationOperator =
  | DurationOperatorAdd
  | DurationOperatorMultiply
  ;

export interface DurationOperatorAdd {
  kind: 'DurationOperatorAdd',
  left: DurationSpecifier
  right: DurationSpecifier,
}

export interface DurationOperatorMultiply {
  kind: 'DurationOperatorMultiply',
  left: DurationSpecifier
  right: DurationSpecifier,
}

// Global constraints
// - resource limits
// - mutex constraints
