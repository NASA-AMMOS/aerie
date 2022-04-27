export enum NodeKind {
  DummyConstraint = 'DummyConstraint',
  ConstraintAnd = 'ConstraintAnd',
  ConstraintOr = 'ConstraintOr'
}

export type Constraint = | DummyConstraint;

export interface DummyConstraint {
  kind: NodeKind.DummyConstraint,
  someNumber: number,
}

export type ConstraintSpecifier =
    | Constraint
    | ConstraintComposition
    ;

/**
 * Constraint Composition
 *
 * Compose constraints together
 */
export type ConstraintComposition =
    | ConstraintAnd
    | ConstraintOr
    ;

export interface ConstraintAnd {
  kind: NodeKind.ConstraintAnd,
  constraints: ConstraintSpecifier[],
}

export interface ConstraintOr {
  kind: NodeKind.ConstraintOr,
  constraints: ConstraintSpecifier[],
}

