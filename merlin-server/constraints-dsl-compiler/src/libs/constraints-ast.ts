export enum NodeKind {
  ViolationsOf = 'ViolationsOf',
  WindowsExpressionAnd = 'WindowsExpressionAnd',
  WindowsExpressionOr = 'WindowsExpressionOr',
  WindowsExpressionTrue = 'WindowsExpressionTrue'
}

export type Constraint = | ViolationsOf;

export interface ViolationsOf {
  kind: NodeKind.ViolationsOf,
  expression: WindowsExpression,
}

export type WindowsExpression =
  | And
  | Or
  | True;

export interface And {
  kind: NodeKind.WindowsExpressionAnd,
  expressions: WindowsExpression[],
}

export interface Or {
  kind: NodeKind.WindowsExpressionOr,
  expressions: WindowsExpression[],
}

export interface True {
  kind: NodeKind.WindowsExpressionTrue
}

