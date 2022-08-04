export enum NodeKind {
  DiscreteProfileResource = 'DiscreteProfileResource',
  DiscreteProfileValue = 'DiscreteProfileValue',
  DiscreteProfileParameter = 'DiscreteProfileParameter',
  RealProfileResource = 'RealProfileResource',
  RealProfileValue = 'RealProfileValue',
  RealProfileParameter = 'RealProfileParameter',
  RealProfilePlus = 'RealProfilePlus',
  RealProfileTimes = 'RealProfileTimes',
  RealProfileRate = 'RealProfileRate',
  DiscreteProfileTransition = 'DiscreteProfileTransition',
  WindowsExpressionActivityWindow = 'WindowsExpressionActivityWindow',
  WindowsExpressionStartOf = 'WindowsExpressionStartOf',
  WindowsExpressionEndOf = 'WindowsExpressionEndOf',
  WindowsExpressionLongerThan = 'WindowsExpressionLongerThan',
  WindowsExpressionShorterhan = 'WindowsExpressionShorterThan',
  WindowsExpressionShiftBy = 'WindowsExpressionShiftBy',
  WindowsExpressionFromSpans = 'WindowsExpressionFromSpans',
  SpansExpressionFromWindows = 'SpansExpressionFromWindows',
  ExpressionEqual = 'ExpressionEqual',
  ExpressionNotEqual = 'ExpressionNotEqual',
  RealProfileLessThan = 'RealProfileLessThan',
  RealProfileLessThanOrEqual = 'RealProfileLessThanOrEqual',
  RealProfileGreaterThan = 'RealProfileGreaterThan',
  RealProfileGreaterThanOrEqual = 'RealProfileGreaterThanOrEqual',
  WindowsExpressionAll = 'WindowsExpressionAll',
  WindowsExpressionAny = 'WindowsExpressionAny',
  WindowsExpressionInvert = 'WindowsExpressionInvert',
  IntervalsExpressionSplit = 'IntervalsExpressionSplit',
  ForEachActivity = 'ForEachActivity',
  ProfileChanges = 'ProfileChanges',
  ViolationsOf = 'ViolationsOf',
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
  | WindowsExpressionShiftBy
  | WindowsExpressionFromSpans
  | IntervalsExpressionSplit;

export type SpansExpression =
  | IntervalsExpressionSplit
  | SpansExpressionFromWindows;

export type IntervalsExpression =
  | WindowsExpression
  | SpansExpression;

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
  windowExpression: WindowsExpression,
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
  alias: string;
}

export interface WindowsExpressionStartOf {
  kind: NodeKind.WindowsExpressionStartOf;
  alias: string;
}

export interface WindowsExpressionActivityWindow {
  kind: NodeKind.WindowsExpressionActivityWindow;
  alias: string;
}

export interface WindowsExpressionShorterThan {
  kind: NodeKind.WindowsExpressionShorterhan,
  windowExpression: WindowsExpression,
  duration: number
}

export interface WindowsExpressionLongerThan {
  kind: NodeKind.WindowsExpressionLongerThan,
  windowExpression: WindowsExpression,
  duration: number
}

export interface IntervalsExpressionSplit {
  kind: NodeKind.IntervalsExpressionSplit,
  intervals: IntervalsExpression,
  numberOfSubIntervals: number
}

export interface WindowsExpressionFromSpans {
  kind: NodeKind.WindowsExpressionFromSpans,
  spansExpression: SpansExpression
}

export interface SpansExpressionFromWindows {
  kind: NodeKind.SpansExpressionFromWindows,
  windowsExpression: WindowsExpression
}

export interface DiscreteProfileTransition {
  kind: NodeKind.DiscreteProfileTransition;
  profile: DiscreteProfileExpression;
  from: any;
  to: any;
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
  alias: string;
  name: string;
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
  alias: string;
  name: string;
}
