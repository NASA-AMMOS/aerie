import * as AST from './scheduler-ast.js';
import type * as WindowsEDSL from './constraints-edsl-fluent-api'
import type {ActivityType} from "./scheduler-mission-model-generated-code";

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
  public static CoexistenceGoal(opts: { activityTemplate: ActivityTemplate, forEach: WindowsEDSL.Windows | ActivityExpression }): ActivityCoexistenceGoal {
    return Goal.new({
      kind: AST.NodeKind.ActivityCoexistenceGoal,
      activityTemplate: opts.activityTemplate,
      forEach: opts.forEach.__astNode
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


declare global {
  class Goal {
    public readonly __astNode: AST.GoalSpecifier;
    public and(...others: Goal[]): Goal

    public or(...others: Goal[]): Goal

    public static ActivityRecurrenceGoal(opts: { activityTemplate: ActivityTemplate, interval: Duration }): ActivityRecurrenceGoal

    public static CoexistenceGoal(opts: { activityTemplate: ActivityTemplate, forEach: WindowsEDSL.Windows | ActivityExpression }): ActivityCoexistenceGoal

    public static CardinalityGoal(opts: { activityTemplate: ActivityTemplate, specification: AST.CardinalityGoalArguments, inPeriod: ClosedOpenInterval }): ActivityCardinalityGoal
  }
  class ActivityExpression {
    public static ofType(activityType: ActivityType): ActivityExpression
  }
  type Double = number;
  type Integer = number;
}

export interface ClosedOpenInterval extends AST.ClosedOpenInterval {}
export interface ActivityTemplate extends AST.ActivityTemplate {}

// Make Goal available on the global object
Object.assign(globalThis, { Goal, ActivityExpression });
