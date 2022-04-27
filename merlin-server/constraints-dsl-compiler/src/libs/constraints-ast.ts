export enum NodeKind {
  ConstraintAnd = 'ConstraintAnd',
  ConstraintOr = 'ConstraintOr'
}

export type Constraint = void;
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

