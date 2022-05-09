export enum NodeKind {
  ViolationsOf = 'ViolationsOf',
  WindowsExpressionAnd = 'WindowsExpressionAnd',
  WindowsExpressionOr = 'WindowsExpressionOr',
  WindowsExpressionTrue = 'WindowsExpressionTrue'
}

/**
 * Top-level constraints that directly produce violations
 * when evaluated.
 */
export type Constraint = | ViolationsOf;

/**
 * Generates violations whenever its expression generates a window.
 */
export interface ViolationsOf {
  kind: NodeKind.ViolationsOf,
  expression: WindowsExpression,
}

/**
 * Operations that produce Windows when evaluated.
 *
 * These are not valid top-level constraints, and
 * typically need to be wrapped in `Constraint.ViolationsOf`.
 */
export type WindowsExpression =
  | And
  | Or
  | True;

/**
 * Generates a violation when all of its sub-expressions generate windows.
 */
export interface And {
  kind: NodeKind.WindowsExpressionAnd,
  expressions: WindowsExpression[],
}

/**
 * Generates a violation when any of its sub-expressions generate windows.
 */
export interface Or {
  kind: NodeKind.WindowsExpressionOr,
  expressions: WindowsExpression[],
}

/**
 * Always generates a window.
 *
 * Currently this only exists for testing, it isn't intended to be useful.
 */
export interface True {
  kind: NodeKind.WindowsExpressionTrue
}

