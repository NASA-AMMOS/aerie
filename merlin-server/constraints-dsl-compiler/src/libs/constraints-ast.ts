export enum NodeKind {
  DiscreteProfileResource = '.DiscreteResource',
  DiscreteProfileValue = '.DiscreteValue',
  DiscreteProfileParameter = '.DiscreteParameter',
  RealProfileResource = '.RealResource',
  RealProfileValue = '.RealValue',
  RealProfileParameter = '.RealParameter',
  RealProfilePlus = '.Plus',
  RealProfileTimes = '.Times',
  RealProfileRate = '.Rate',
  DiscreteProfileTransition = '.Transition',
  WindowsExpressionActivityWindow = '.ActivityWindow',
  WindowsExpressionStartOf = '.StartOf',
  WindowsExpressionEndOf = '.EndOf',
  WindowsExpressionLongerThan = '.LongerThan',
  WindowsExpressionShorterThan = '.ShorterThan',
  WindowsExpressionShiftBy = '.ShiftBy',
  ExpressionEqual = '.Equal',
  ExpressionNotEqual = '.NotEqual',
  RealProfileLessThan = '.LessThan',
  RealProfileLessThanOrEqual = '.LessThanOrEqual',
  RealProfileGreaterThan = '.GreaterThan',
  RealProfileGreaterThanOrEqual = '.GreaterThanOrEqual',
  WindowsExpressionAll = '.All',
  WindowsExpressionAny = '.Any',
  WindowsExpressionInvert = '.Invert',
  ForEachActivity = '.ForEachActivity',
  ProfileChanges = '.Changes',
  ViolationsOf = '.ViolationsOf',
}

export type Constraint = ViolationsOf | ForEachActivity | WindowsExpression;

export interface ViolationsOf {
  kind: NodeKind.ViolationsOf;
  expression: WindowsExpression;
}

export interface ForEachActivity {
  kind: NodeKind.ForEachActivity;
  activityType: string;
  alias: string;
  expression: Constraint;
}

export type WindowsExpression =
  | WindowsExpressionActivityWindow
  | WindowsExpressionStartOf
  | WindowsExpressionEndOf
  | ProfileChanges
  | RealProfileLessThan
  | RealProfileLessThanOrEqual
  | RealProfileGreaterThan
  | RealProfileGreaterThanOrEqual
  | DiscreteProfileTransition
  | ExpressionEqual<RealProfileExpression>
  | ExpressionEqual<DiscreteProfileExpression>
  | ExpressionNotEqual<RealProfileExpression>
  | ExpressionNotEqual<DiscreteProfileExpression>
  | WindowsExpressionAll
  | WindowsExpressionAny
  | WindowsExpressionLongerThan
  | WindowsExpressionShorterThan
  | WindowsExpressionInvert
  | WindowsExpressionShiftBy;

export interface ProfileChanges {
  kind: NodeKind.ProfileChanges;
  expression: ProfileExpression;
}

export interface WindowsExpressionInvert {
  kind: NodeKind.WindowsExpressionInvert;
  expression: WindowsExpression;
}

export type Duration = number

export interface WindowsExpressionShiftBy {
  kind: NodeKind.WindowsExpressionShiftBy,
  windows: WindowsExpression,
  fromStart: Duration,
  fromEnd: Duration,
}

export interface WindowsExpressionAny {
  kind: NodeKind.WindowsExpressionAny;
  expressions: WindowsExpression[];
}

export interface WindowsExpressionAll {
  kind: NodeKind.WindowsExpressionAll;
  expressions: WindowsExpression[];
}

export interface RealProfileGreaterThanOrEqual {
  kind: NodeKind.RealProfileGreaterThanOrEqual;
  left: RealProfileExpression;
  right: RealProfileExpression;
}

export interface RealProfileGreaterThan {
  kind: NodeKind.RealProfileGreaterThan;
  left: RealProfileExpression;
  right: RealProfileExpression;
}

export interface RealProfileLessThanOrEqual {
  kind: NodeKind.RealProfileLessThanOrEqual;
  left: RealProfileExpression;
  right: RealProfileExpression;
}

export interface RealProfileLessThan {
  kind: NodeKind.RealProfileLessThan;
  left: RealProfileExpression;
  right: RealProfileExpression;
}

export interface ExpressionNotEqual<T = ProfileExpression> {
  kind: NodeKind.ExpressionNotEqual;
  left: T;
  right: T;
}

export interface ExpressionEqual<T = ProfileExpression> {
  kind: NodeKind.ExpressionEqual;
  left: T;
  right: T;
}

export interface WindowsExpressionEndOf {
  kind: NodeKind.WindowsExpressionEndOf;
  activityAlias: string;
}

export interface WindowsExpressionStartOf {
  kind: NodeKind.WindowsExpressionStartOf;
  activityAlias: string;
}

export interface WindowsExpressionActivityWindow {
  kind: NodeKind.WindowsExpressionActivityWindow;
  activityAlias: string;
}

export interface WindowsExpressionShorterThan {
  kind: NodeKind.WindowsExpressionShorterThan,
  windows: WindowsExpression,
  duration: number
}

export interface WindowsExpressionLongerThan {
  kind: NodeKind.WindowsExpressionLongerThan,
  windows: WindowsExpression,
  duration: number
}

export interface DiscreteProfileTransition {
  kind: NodeKind.DiscreteProfileTransition;
  profile: DiscreteProfileExpression;
  oldState: any;
  newState: any;
}

export type ProfileExpression = RealProfileExpression | DiscreteProfileExpression;

export type RealProfileExpression =
  | RealProfileRate
  | RealProfileTimes
  | RealProfilePlus
  | RealProfileResource
  | RealProfileValue
  | RealProfileParameter;

export interface RealProfileRate {
  kind: NodeKind.RealProfileRate;
  profile: RealProfileExpression;
}

export interface RealProfileTimes {
  kind: NodeKind.RealProfileTimes;
  profile: RealProfileExpression;
  multiplier: number;
}

export interface RealProfilePlus {
  kind: NodeKind.RealProfilePlus;
  left: RealProfileExpression;
  right: RealProfileExpression;
}

export interface RealProfileResource {
  kind: NodeKind.RealProfileResource;
  name: string;
}

export interface RealProfileValue {
  kind: NodeKind.RealProfileValue;
  value: number;
}

export interface RealProfileParameter {
  kind: NodeKind.RealProfileParameter;
  activityAlias: string;
  parameterName: string;
}

export type DiscreteProfileExpression = DiscreteProfileResource | DiscreteProfileValue | DiscreteProfileParameter;

export interface DiscreteProfileResource {
  kind: NodeKind.DiscreteProfileResource;
  name: string;
}

export interface DiscreteProfileValue {
  kind: NodeKind.DiscreteProfileValue;
  value: any;
}

export interface DiscreteProfileParameter {
  kind: NodeKind.DiscreteProfileParameter;
  activityAlias: string;
  parameterName: string;
}
