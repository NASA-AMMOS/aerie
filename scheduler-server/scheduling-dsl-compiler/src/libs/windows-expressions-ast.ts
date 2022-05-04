export enum NodeKind {
  ActivityExpression = 'ActivityExpression',
  WindowsExpressionGreaterThan = 'WindowsExpressionGreaterThan',
  WindowsExpressionLessThan = 'WindowsExpressionLessThan',
  WindowsExpressionEqualLinear = 'WindowsExpressionEqualLinear',
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

export interface WindowsExpressionLessThan {
  kind: NodeKind.WindowsExpressionLessThan,
  left: LinearResource,
  right: number
}

export interface WindowsExpressionEqualLinear {
  kind: NodeKind.WindowsExpressionEqualLinear,
  left: LinearResource,
  right: any
}

export type WindowsExpression =
    | ActivityExpression
    | WindowsExpressionGreaterThan
    | WindowsExpressionLessThan
    | WindowsExpressionEqualLinear

