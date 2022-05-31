import * as AST from './constraints-ast.js';
import * as Gen from './mission-model-generated-code.js';

export class Constraint {
  /** Internal AST node */
  public readonly __astNode: AST.Constraint;

  public constructor(astNode: AST.Constraint) {
    this.__astNode = astNode;
  }

  public static ForbiddenActivityOverlap(activityType1: Gen.ActivityTypeName, activityType2: Gen.ActivityTypeName): Constraint {
    return new Constraint({
      kind: AST.NodeKind.ForbiddenActivityOverlap,
      activityType1,
      activityType2
    })
  }

  public static ForEachActivity(activityType: Gen.ActivityTypeName, alias: string, constraint: Constraint): Constraint {
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

  public static Resource(name: Gen.RealResourceName): Real {
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
  public plus(other: Real | number): Real {
    if (!(other instanceof Real)) {
      other = Real.Value(other);
    }
    return new Real({
      kind: AST.NodeKind.RealProfilePlus,
      left: this.__astNode,
      right: other.__astNode
    })
  }

  public lessThan(other: Real | number): Windows {
    if (!(other instanceof Real)) {
      other = Real.Value(other);
    }
    return new Windows({
      kind: AST.NodeKind.RealProfileLessThan,
      left: this.__astNode,
      right: other.__astNode
    })
  }
  public lessThanOrEqual(other: Real | number): Windows {
    if (!(other instanceof Real)) {
      other = Real.Value(other);
    }
    return new Windows({
      kind: AST.NodeKind.RealProfileLessThanOrEqual,
      left: this.__astNode,
      right: other.__astNode
    })
  }
  public greaterThan(other: Real | number): Windows {
    if (!(other instanceof Real)) {
      other = Real.Value(other);
    }
    return new Windows({
      kind: AST.NodeKind.RealProfileGreaterThan,
      left: this.__astNode,
      right: other.__astNode
    })
  }
  public greaterThanOrEqual(other: Real | number): Windows {
    if (!(other instanceof Real)) {
      other = Real.Value(other);
    }
    return new Windows({
      kind: AST.NodeKind.RealProfileGreaterThanOrEqual,
      left: this.__astNode,
      right: other.__astNode
    })
  }

  public equal(other: Real | number): Windows {
    if (!(other instanceof Real)) {
      other = Real.Value(other);
    }
    return new Windows({
      kind: AST.NodeKind.ExpressionEqual,
      left: this.__astNode,
      right: other.__astNode
    })
  }
  public notEqual(other: Real | number): Windows {
    if (!(other instanceof Real)) {
      other = Real.Value(other);
    }
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

export class Discrete<Schema> {
  public readonly __astNode: AST.DiscreteProfileExpression
  private readonly __schemaInstance: Schema;

  public constructor(profile: AST.DiscreteProfileExpression, template: Schema) {
    this.__astNode = profile;
    this.__schemaInstance = template;
  }

  public static Resource<R extends Gen.ResourceName>(name: R): Discrete<Gen.DiscreteResourceSchema<R>> {
    return new Discrete({
      kind: AST.NodeKind.DiscreteProfileResource,
      name
    }, Gen.discreteResourceSchemaDummyValue<R>(name))
  }
  public static Value<Schema>(value: Schema): Discrete<Schema> {
    return new Discrete({
      kind: AST.NodeKind.DiscreteProfileValue,
      value
    }, value)
  }
  public static Parameter<Schema>(alias: string, name: string): Discrete<Schema> {
    return new Discrete({
      kind: AST.NodeKind.DiscreteProfileParameter,
      alias,
      name
    }, 5 as unknown as Schema) // will fix in next commit
  }

  public transition(from: Schema, to: Schema): Windows {
    return new Windows({
      kind: AST.NodeKind.DiscreteProfileTransition,
      profile: this.__astNode,
      from,
      to
    })
  }

  public equal(other: Schema | Discrete<Schema>): Windows {
    if (!(other instanceof Discrete)) {
      other = Discrete.Value(other);
    }
    return new Windows({
      kind: AST.NodeKind.ExpressionEqual,
      left: this.__astNode,
      right: other.__astNode
    })
  }
  public notEqual(other: Schema | Discrete<Schema>): Windows {
    if (!(other instanceof Discrete)) {
      other = Discrete.Value(other);
    }
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

    /**
     * Forbid instances of two activity types from overlapping with each other.
     * @param activityType1
     * @param activityType2
     * @constructor
     */
    public static ForbiddenActivityOverlap(activityType1: Gen.ActivityTypeName, activityType2: Gen.ActivityTypeName): Constraint

    /**
     * Check a constraint for each instance of an activity type.
     *
     * @param activityType activity type to check
     * @param alias aliased name of activity instances, referenced by the constraint argument
     * @param constraint constraint to apply
     * @constructor
     */
    public static ForEachActivity(activityType: Gen.ActivityTypeName, alias: string, constraint: Constraint): Constraint
  }

  export class Windows {
    /** Internal AST Node */
    public readonly __astNode: AST.WindowsExpression;

    /**
     * Produce a window for the duration of the aliased activity instance.
     *
     * @param alias
     * @constructor
     */
    public static During(alias: string): Windows

    /**
     * Produce an instantaneous window at the start of the aliased activity instance.
     *
     * @param alias
     * @constructor
     */
    public static StartOf(alias: string): Windows

    /**
     * Produce an instantaneous window at the end of the aliased activity instance.
     * @param alias
     * @constructor
     */
    public static EndOf(alias: string): Windows

    /**
     * Produce a window when all arguments produce a window.
     *
     * Performs the intersection of the argument windows.
     *
     * @param windows any number of windows expressions
     */
    public static All(...windows: Windows[]): Windows

    /**
     * Produce a window when any argument produces a window.
     *
     * Performs the union of the argument windows.
     *
     * @param windows one or more windows expressions
     */
    public static Any(...windows: Windows[]): Windows

    /**
     * Only check this expression of the condition argument produces a window.
     *
     * @param condition
     */
    public if(condition: Windows): Windows

    /**
     * Negate all the windows produced this.
     */
    public not(): Windows

    /**
     * Produce a constraint violation whenever this does NOT produce a window.
     *
     * Essentially, express the condition you want to be satisfied, then use
     * this method to produce a violation whenever it is NOT satisfied.
     */
    public violations(): Constraint
  }

  export class Real {
    /** Internal AST Node */
    public readonly __astNode: AST.RealProfileExpression;

    /**
     * Reference the real profile associated with a resource.
     * @param name
     * @constructor
     */
    public static Resource(name: Gen.RealResourceName): Real

    /**
     * Create a constant real profile for all time.
     * @param value
     * @constructor
     */
    public static Value(value: number): Real

    /**
     * Reference the value of an activity instance parameter as a constant real profile.
     *
     * Only valid for the duration of the activity instance.
     *
     * @param alias alias of the activity instance
     * @param name name of the parameter
     * @constructor
     */
    public static Parameter(alias: string, name: string): Real

    /**
     * Create a real profile from this profile's derivative.
     */
    public rate(): Real

    /**
     * Create a real profile by multiplying this profile by a constant
     * @param multiplier
     */
    public times(multiplier: number): Real

    /**
     * Create a real profile by adding this and another real profile together.
     * @param other
     */
    public plus(other: Real | number): Real

    /**
     * Produce a window whenever this profile is less than another real profile.
     * @param other
     */
    public lessThan(other: Real | number): Windows

    /**
     * Produce a window whenever this profile is less than or equal to another real profile.
     * @param other
     */
    public lessThanOrEqual(other: Real | number): Windows

    /**
     * Produce a window whenever this profile is greater than to another real profile.
     * @param other
     */
    public greaterThan(other: Real | number): Windows

    /**
     * Produce a window whenever this profile is greater than or equal to another real profile.
     * @param other
     */
    public greaterThanOrEqual(other: Real | number): Windows

    /**
     * Produce a window whenever this profile is equal to another real profile.
     * @param other
     */
    public equal(other: Real | number): Windows

    /**
     * Produce a window whenever this profile is not equal to another real profile.
     * @param other
     */
    public notEqual(other: Real | number): Windows

    /**
     * Produce an instantaneous window whenever this profile changes.
     */
    public changed(): Windows
  }

  export class Discrete<Schema> {
    /** Internal AST Node */
    public readonly __astNode: AST.DiscreteProfileExpression
    /** Internal instance of the schema to enforce type checking. */
    private readonly __schemaInstance: Schema;

    /**
     * Reference the discrete profile associated with a resource.
     * @param name
     * @constructor
     */
    public static Resource<R extends Gen.ResourceName>(name: R): Discrete<Gen.DiscreteResourceSchema<R>>

    /**
     * Create a constant discrete profile for all time.
     * @param value
     * @constructor
     */
    public static Value<Schema>(value: Schema): Discrete<Schema>

    /**
     * Reference the value of an activity instance parameter as a constant discrete profile.
     *
     * Only valid for the duration of the activity instance.
     *
     * @param alias alias of the activity instance
     * @param name name of the parameter
     * @constructor
     */
    public static Parameter<Schema>(alias: string, name: string): Discrete<Schema>

    /**
     * Produce an instantaneous window whenever this profile makes a specific transition.
     *
     * @param from initial value
     * @param to final value
     */
    public transition(from: Schema, to: Schema): Windows

    /**
     * Produce a window whenever this profile is equal to another discrete profile.
     * @param other
     */
    public equal(other: Schema | Discrete<Schema>): Windows

    /**
     * Produce a window whenever this profile is not equal to another discrete profile.
     * @param other
     */
    public notEqual(other: Schema | Discrete<Schema>): Windows

    /**
     * Produce an instantaneous window whenever this profile changes.
     */
    public changed(): Windows
  }
}

// Make Constraint available on the global object
Object.assign(globalThis, {Constraint, Windows, Real, Discrete});
