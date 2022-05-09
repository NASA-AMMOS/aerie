import * as AST from './constraints-ast.js';

interface ViolationsOf extends Constraint {}
export class Constraint {
  private readonly constraint: AST.Constraint;

  private constructor(constraint: AST.Constraint) {
    this.constraint = constraint;
  }

  private static new(constraint: AST.Constraint): Constraint {
    return new Constraint(constraint);
  }

  private __serialize(): AST.Constraint {
    return this.constraint;
  }

  public static ViolationsOf(expression: WindowsExpression): ViolationsOf {
    return Constraint.new({
      kind: AST.NodeKind.ViolationsOf,
      expression: expression['__serialize']()
    });
  }
}

interface True extends WindowsExpression {}
export class WindowsExpression {
  private readonly expression: AST.WindowsExpression;

  private constructor(expression: AST.WindowsExpression) {
    this.expression = expression;
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
        this.expression,
        ...others.map(other => other.expression),
      ],
    });
  }

  public or(...others: WindowsExpression[]): WindowsExpression {
    return WindowsExpression.new({
      kind: AST.NodeKind.WindowsExpressionOr,
      expressions: [
        this.expression,
        ...others.map(other => other.expression),
      ],
    });
  }

  public static True(): True {
    return WindowsExpression.new({
      kind: AST.NodeKind.True
    });
  }
}

declare global {
  export class Constraint {
    public static ViolationsOf(expression: WindowsExpression): ViolationsOf
  }
  export class WindowsExpression {
    public and(...others: WindowsExpression[]): WindowsExpression
    public or(...others: WindowsExpression[]): WindowsExpression
    public static True(): True
  }
}

// Make Constraint available on the global object
Object.assign(globalThis, { Constraint, WindowsExpression });
