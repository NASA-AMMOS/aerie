import * as AST from './constraints-ast.js';

interface ViolationsOf extends Constraint {}

export class Constraint {
  /** Internal AST node */
  public readonly __astNode: AST.Constraint;

  private constructor(astNode: AST.Constraint) {
    this.__astNode = astNode;
  }

  private static new(astNode: AST.Constraint): Constraint {
    return new Constraint(astNode);
  }

  public static ViolationsOf(expression: WindowsExpression): ViolationsOf {
    return Constraint.new({
      kind: AST.NodeKind.ViolationsOf,
      expression: expression.__astNode,
    });
  }
}

interface True extends WindowsExpression {}

export class WindowsExpression {
  /** Internal AST node */
  public readonly __astNode: AST.WindowsExpression;

  private constructor(expression: AST.WindowsExpression) {
    this.__astNode = expression;
  }

  private static new(expression: AST.WindowsExpression): WindowsExpression {
    return new WindowsExpression(expression);
  }

  public static True(): True {
    return WindowsExpression.new({
      kind: AST.NodeKind.WindowsExpressionTrue
    });
  }

  public and(...others: WindowsExpression[]): WindowsExpression {
    return WindowsExpression.new({
      kind: AST.NodeKind.WindowsExpressionAnd,
      expressions: [
        this.__astNode,
        ...others.map(other => other.__astNode),
      ],
    });
  }

  public or(...others: WindowsExpression[]): WindowsExpression {
    return WindowsExpression.new({
      kind: AST.NodeKind.WindowsExpressionOr,
      expressions: [
        this.__astNode,
        ...others.map(other => other.__astNode),
      ],
    });
  }
}

declare global {
  export class Constraint {
    /** Internal AST Node */
    public readonly __astNode: AST.Constraint;

    public static ViolationsOf(expression: WindowsExpression): ViolationsOf
  }

  export class WindowsExpression {
    /** Internal AST Node */
    public readonly __astNode: AST.WindowsExpression;

    public static True(): True

    public and(...others: WindowsExpression[]): WindowsExpression

    public or(...others: WindowsExpression[]): WindowsExpression
  }
}

// Make Constraint available on the global object
Object.assign(globalThis, {Constraint, WindowsExpression});
