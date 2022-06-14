import * as AST from './scheduler-ast.js';
import type * as WindowsEDSL from './constraints-edsl-fluent-api'
import type {ActivityType} from "./scheduler-mission-model-generated-code";

type WindowProperty = AST.WindowProperty
type TimingConstraintOperator = AST.TimingConstraintOperator

interface ActivityRecurrenceGoal extends Goal {}
interface ActivityCoexistenceGoal extends Goal {}
interface ActivityCardinalityGoal extends Goal {}

export class Goal {
  public readonly __astNode: AST.GoalSpecifier;

  private constructor(__astNode: AST.GoalSpecifier) {
    this.__astNode = __astNode;
  }

  private static new(__astNode: AST.GoalSpecifier): Goal {
    return new Goal(__astNode);
  }

  public and(...others: Goal[]): Goal {
    return Goal.new({
      kind: AST.NodeKind.GoalAnd,
      goals: [
        this.__astNode,
        ...others.map(other => other.__astNode),
      ],
    });
  }

  public or(...others: Goal[]): Goal {
    return Goal.new({
      kind: AST.NodeKind.GoalOr,
      goals: [
        this.__astNode,
        ...others.map(other => other.__astNode),
      ],
    });
  }

  public static ActivityRecurrenceGoal(opts: { activityTemplate: ActivityTemplate, interval: Duration }): ActivityRecurrenceGoal {
    return Goal.new({
      kind: AST.NodeKind.ActivityRecurrenceGoal,
      activityTemplate: opts.activityTemplate,
      interval: opts.interval,
    });
  }
  public static CoexistenceGoal(opts: {
    activityTemplate: ActivityTemplate,
    forEach: WindowsEDSL.Windows | ActivityExpression,
  } & CoexistenceGoalTimingConstraints): ActivityCoexistenceGoal {
    return Goal.new({
      kind: AST.NodeKind.ActivityCoexistenceGoal,
      activityTemplate: opts.activityTemplate,
      forEach: opts.forEach.__astNode,
      startConstraint: (("startsAt" in opts) ? opts.startsAt.__astNode : ("startsWithin" in opts) ? opts.startsWithin.__astNode : undefined),
      endConstraint: (("endsAt" in opts) ? opts.endsAt.__astNode : ("endsWithin" in opts) ? opts.endsWithin.__astNode : undefined),
    });
  }
  public static CardinalityGoal(opts: { activityTemplate: ActivityTemplate, specification: AST.CardinalityGoalArguments, inPeriod: ClosedOpenInterval }): ActivityCardinalityGoal {
    return Goal.new({
      kind: AST.NodeKind.ActivityCardinalityGoal,
      activityTemplate: opts.activityTemplate,
      specification: opts.specification,
      inPeriod : opts.inPeriod
    });
  }
}

type StartTimingConstraint = { startsAt: SingletonTimingConstraint } | { startsWithin: RangeTimingConstraint }
type EndTimingConstraint = { endsAt: SingletonTimingConstraint } | {endsWithin: RangeTimingConstraint }
type CoexistenceGoalTimingConstraints = StartTimingConstraint | EndTimingConstraint | (StartTimingConstraint & EndTimingConstraint)

class ActivityExpression {
  public readonly __astNode: AST.ActivityExpression;

  private constructor(__astNode: AST.ActivityExpression) {
    this.__astNode = __astNode;
  }

  private static new(__astNode: AST.ActivityExpression): ActivityExpression {
    return new ActivityExpression(__astNode);
  }

  public static ofType(activityType: ActivityType): ActivityExpression {
    return ActivityExpression.new({
      kind: AST.NodeKind.ActivityExpression,
      type: activityType
    })
  }
}

class TimingConstraint {
  public static singleton(windowProperty: WindowProperty, operator: TimingConstraintOperator, operand: Duration): SingletonTimingConstraint {
    return SingletonTimingConstraint.new({
      windowProperty,
      operator,
      operand,
      singleton: true
    })
  }
  public static range(windowProperty: WindowProperty, operator: TimingConstraintOperator, operand: Duration): RangeTimingConstraint {
    return RangeTimingConstraint.new({
      windowProperty,
      operator,
      operand,
      singleton: false
    })
  }
}

class SingletonTimingConstraint {
  public readonly __astNode: AST.ActivityTimingConstraintSingleton
  private constructor(__astNode: AST.ActivityTimingConstraintSingleton) {
    this.__astNode = __astNode;
  }
  public static new(__astNode: AST.ActivityTimingConstraintSingleton): SingletonTimingConstraint {
    return new SingletonTimingConstraint(__astNode);
  }
}

class RangeTimingConstraint {
  public readonly __astNode: AST.ActivityTimingConstraintRange
  private constructor(__astNode: AST.ActivityTimingConstraintRange) {
    this.__astNode = __astNode;
  }
  public static new(__astNode: AST.ActivityTimingConstraintRange): RangeTimingConstraint {
    return new RangeTimingConstraint(__astNode);
  }
}

declare global {
  class Goal {
    public readonly __astNode: AST.GoalSpecifier;
    public and(...others: Goal[]): Goal

    public or(...others: Goal[]): Goal

    public static ActivityRecurrenceGoal(opts: { activityTemplate: ActivityTemplate, interval: Duration }): ActivityRecurrenceGoal

    /**
     * The CoexistenceGoal places one activity (defined by activityTemplate) per window (defined by forEach).
     * The activity is placed such that it starts at (startsAt) or ends at (endsAt) a certain offset from the window
     */
    public static CoexistenceGoal(opts: {
      activityTemplate: ActivityTemplate,
      forEach: WindowsEDSL.Windows | ActivityExpression,
    } & CoexistenceGoalTimingConstraints): ActivityCoexistenceGoal

    public static CardinalityGoal(opts: { activityTemplate: ActivityTemplate, specification: AST.CardinalityGoalArguments, inPeriod: ClosedOpenInterval }): ActivityCardinalityGoal
  }
  class ActivityExpression {
    public static ofType(activityType: ActivityType): ActivityExpression
  }
  class TimingConstraint {
    public static singleton(windowProperty: WindowProperty, operator: TimingConstraintOperator, operand: Duration): SingletonTimingConstraint
    public static range(windowProperty: WindowProperty, operator: TimingConstraintOperator, operand: Duration): RangeTimingConstraint
  }
  var WindowProperty: typeof AST.WindowProperty
  var Operator: typeof AST.TimingConstraintOperator

  type Double = number;
  type Integer = number;
}

export interface ClosedOpenInterval extends AST.ClosedOpenInterval {}
export interface ActivityTemplate extends AST.ActivityTemplate {}

// Make Goal available on the global object
Object.assign(globalThis, { Goal, ActivityExpression, TimingConstraint: TimingConstraint, WindowProperty: AST.WindowProperty, Operator: AST.TimingConstraintOperator });
