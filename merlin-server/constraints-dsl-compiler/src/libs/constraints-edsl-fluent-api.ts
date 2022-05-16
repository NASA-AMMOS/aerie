import * as AST from './constraints-ast.js';

export class Constraint {
  /** Internal AST node */
  public readonly __astNode: AST.Constraint;

  public constructor(astNode: AST.Constraint) {
    this.__astNode = astNode;
  }

  public static ForbiddenActivityOverlap(activityType1: string, activityType2: string): Constraint {
    return new Constraint({
      kind: AST.NodeKind.ForbiddenActivityOverlap,
      activityType1,
      activityType2
    })
  }

  public static ForEachActivity(activityType: string, alias: string, constraint: Constraint): Constraint {
    return new Constraint({
      kind: AST.NodeKind.ForEachActivity,
      activityType,
      alias,
      expression: constraint.__astNode
    })
  }
}

export class Windows {
  /** Internal AST node */
  public readonly __astNode: AST.WindowsExpression;

  public constructor(expression: AST.WindowsExpression) {
    this.__astNode = expression;
  }

  public static During(alias: string): Windows {
    return new Windows({
      kind: AST.NodeKind.WindowsExpressionDuring,
      alias
    })
  }
  public static StartOf(alias: string): Windows {
    return new Windows({
      kind: AST.NodeKind.WindowsExpressionStartOf,
      alias
    })
  }
  public static EndOf(alias: string): Windows {
    return new Windows({
      kind: AST.NodeKind.WindowsExpressionEndOf,
      alias
    })
  }

  public static All(...windows: Windows[]): Windows {
    return new Windows({
      kind: AST.NodeKind.WindowsExpressionAll,
      expressions: [
        ...windows.map(other => other.__astNode),
      ],
    });
  }
  public static Any(...windows: Windows[]): Windows {
    return new Windows({
      kind: AST.NodeKind.WindowsExpressionAny,
      expressions: [
        ...windows.map(other => other.__astNode),
      ],
    });
  }

  public if(condition: Windows): Windows {
    return new Windows({
      kind: AST.NodeKind.WindowsExpressionAny,
      expressions: [
        {
          kind: AST.NodeKind.WindowsExpressionNot,
          expression: condition.__astNode
        },
        this.__astNode
      ]
    })
  }

  public not(): Windows {
    return new Windows({
      kind: AST.NodeKind.WindowsExpressionNot,
      expression: this.__astNode
    })
  }

  public violations(): Constraint {
    return new Constraint({
      kind: AST.NodeKind.ViolationsOf,
      expression: this.__astNode
    })
  }
}

export class Real {
  public readonly __astNode: AST.RealProfileExpression;

  public constructor(profile: AST.RealProfileExpression) {
    this.__astNode = profile;
  }

  public static Resource(name: string): Real {
    return new Real({
      kind: AST.NodeKind.RealProfileResource,
      name
    })
  }
  public static Value(value: number): Real {
    return new Real({
      kind: AST.NodeKind.RealProfileValue,
      value
    })
  }
  public static Parameter(alias: string, name: string): Real {
    return new Real({
      kind: AST.NodeKind.RealProfileParameter,
      alias,
      name
    })
  }

  public rate(): Real {
    return new Real({
      kind: AST.NodeKind.RealProfileRate,
      profile: this.__astNode
    })
  }
  public times(multiplier: number): Real {
    return new Real({
      kind: AST.NodeKind.RealProfileTimes,
      multiplier,
      profile: this.__astNode
    })
  }
  public plus(other: Real): Real {
    return new Real({
      kind: AST.NodeKind.RealProfilePlus,
      left: this.__astNode,
      right: other.__astNode
    })
  }

  public lessThan(other: Real): Windows {
    return new Windows({
      kind: AST.NodeKind.RealProfileLessThan,
      left: this.__astNode,
      right: other.__astNode
    })
  }
  public lessThanOrEqual(other: Real): Windows {
    return new Windows({
      kind: AST.NodeKind.RealProfileLessThanOrEqual,
      left: this.__astNode,
      right: other.__astNode
    })
  }
  public greaterThan(other: Real): Windows {
    return new Windows({
      kind: AST.NodeKind.RealProfileGreaterThan,
      left: this.__astNode,
      right: other.__astNode
    })
  }
  public greaterThanOrEqual(other: Real): Windows {
    return new Windows({
      kind: AST.NodeKind.RealProfileGreaterThanOrEqual,
      left: this.__astNode,
      right: other.__astNode
    })
  }

  public equal(other: Real): Windows {
    return new Windows({
      kind: AST.NodeKind.ExpressionEqual,
      left: this.__astNode,
      right: other.__astNode
    })
  }
  public notEqual(other: Real): Windows {
    return new Windows({
      kind: AST.NodeKind.ExpressionNotEqual,
      left: this.__astNode,
      right: other.__astNode
    })
  }

  public changed(): Windows {
    return new Windows({
      kind: AST.NodeKind.ProfileChanged,
      expression: this.__astNode
    })
  }
}

export class Discrete {
  public readonly __astNode: AST.DiscreteProfileExpression

  public constructor(profile: AST.DiscreteProfileExpression) {
    this.__astNode = profile;
  }

  public static Resource(name: string): Discrete {
    return new Discrete({
      kind: AST.NodeKind.DiscreteProfileResource,
      name
    })
  }
  public static Value(value: any): Discrete {
    return new Discrete({
      kind: AST.NodeKind.DiscreteProfileValue,
      value
    })
  }
  public static Parameter(alias: string, name: string): Discrete {
    return new Discrete({
      kind: AST.NodeKind.DiscreteProfileParameter,
      alias,
      name
    })
  }

  public transition(from: any, to: any): Windows {
    return new Windows({
      kind: AST.NodeKind.DiscreteProfileTransition,
      profile: this.__astNode,
      from,
      to
    })
  }

  public equal(other: Discrete): Windows {
    return new Windows({
      kind: AST.NodeKind.ExpressionEqual,
      left: this.__astNode,
      right: other.__astNode
    })
  }
  public notEqual(other: Discrete): Windows {
    return new Windows({
      kind: AST.NodeKind.ExpressionNotEqual,
      left: this.__astNode,
      right: other.__astNode
    })
  }

  public changed(): Windows {
    return new Windows({
      kind: AST.NodeKind.ProfileChanged,
      expression: this.__astNode
    })
  }
}

declare global {
  export class Constraint {
    /** Internal AST Node */
    public readonly __astNode: AST.Constraint;

    public static ForbiddenActivityOverlap(activityType1: string, activityType2: string): Constraint

    public static ForEachActivity(activityType: string, alias: string, constraint: Constraint): Constraint
  }

  export class Windows {
    /** Internal AST Node */
    public readonly __astNode: AST.WindowsExpression;

    public static During(alias: string): Windows
    public static StartOf(alias: string): Windows
    public static EndOf(alias: string): Windows

    /**
     * Only check this expression of the condition argument produces a window.
     *
     * @param condition
     */
    public static All(...windows: Windows[]): Windows

    /**
     * Produce a window when this and the arguments all produce a window.
     * @param others one or more windows expressions
     */
    public static Any(...windows: Windows[]): Windows

    /**
     * Produce a window when this or one of the arguments produces a window.
     * @param others one or more windows expressions
     */
    public if(condition: Windows): Windows

    /**
     * Negate all the windows produced this.
     */
    public not(): Windows

    public violations(): Constraint
  }

  export class Real {
    /** Internal AST Node */
    public readonly __astNode: AST.RealProfileExpression;

    public static Resource(name: string): Real
    public static Value(value: number): Real
    public static Parameter(alias: string, name: string): Real

    public rate(): Real
    public times(multiplier: number): Real
    public plus(other: Real): Real

    public lessThan(other: Real): Windows
    public lessThanOrEqual(other: Real): Windows
    public greaterThan(other: Real): Windows
    public greaterThanOrEqual(other: Real): Windows

    public equal(other: Real): Windows
    public notEqual(other: Real): Windows

    public changed(): Windows
  }

  export class Discrete {
    /** Internal AST Node */
    public readonly __astNode: AST.DiscreteProfileExpression

    public static Resource(name: string): Discrete
    public static Value(value: any): Discrete
    public static Parameter(alias: string, name: string): Discrete

    public transition<T>(from: T, to: T): Windows

    public equal(other: Discrete): Windows
    public notEqual(other: Discrete): Windows

    public changed(): Windows
  }
}

// Make Constraint available on the global object
Object.assign(globalThis, {Constraint, Windows, Real, Discrete});
