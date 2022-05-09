export enum NodeKind {
  ActivityExpression = 'ActivityExpression',
  WindowsExpressionGreaterThan = 'WindowsExpressionGreaterThan',
}

export interface ActivityExpression {
  kind: NodeKind.ActivityExpression
  type: string
}

export type DiscreteResource = string
export type LinearResource = string

export interface WindowsExpressionGreaterThan {
  kind: NodeKind.WindowsExpressionGreaterThan,
  left: LinearResource,
  right: number
}

export type WindowsExpression =
    | ActivityExpression
    | WindowsExpressionGreaterThan

