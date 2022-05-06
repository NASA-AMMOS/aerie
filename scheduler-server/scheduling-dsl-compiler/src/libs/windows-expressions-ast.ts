export enum NodeKind {
  ActivityExpression = 'ActivityExpression',
}

export interface ActivityExpression {
  kind: NodeKind.ActivityExpression
  type: string
}

export type WindowsExpression =
    | ActivityExpression

