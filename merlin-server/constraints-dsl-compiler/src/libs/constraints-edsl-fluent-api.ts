import * as AST from './constraints-ast.js';

export class Constraint {
  private readonly constraintSpecifier: AST.ConstraintSpecifier;

  private constructor(constraintSpecifier: AST.ConstraintSpecifier) {
    this.constraintSpecifier = constraintSpecifier;
  }

  private static new(constraintSpecifier: AST.ConstraintSpecifier): Constraint {
    return new Constraint(constraintSpecifier);
  }

  private __serialize(): AST.ConstraintSpecifier {
    return this.constraintSpecifier;
  }

  public and(...others: Constraint[]): Constraint {
    return Constraint.new({
      kind: AST.NodeKind.ConstraintAnd,
      constraints: [
        this.constraintSpecifier,
        ...others.map(other => other.constraintSpecifier),
      ],
    });
  }

  public or(...others: Constraint[]): Constraint {
    return Constraint.new({
      kind: AST.NodeKind.ConstraintOr,
      constraints: [
        this.constraintSpecifier,
        ...others.map(other => other.constraintSpecifier),
      ],
    });
  }
}

declare global {
  export class Constraint {
    public and(...others: Constraint[]): Constraint
    public or(...others: Constraint[]): Constraint
  }
  type Duration = number;
  type Double = number;
  type Integer = number;
}

// Make Constraint available on the global object
Object.assign(globalThis, { Constraint });
