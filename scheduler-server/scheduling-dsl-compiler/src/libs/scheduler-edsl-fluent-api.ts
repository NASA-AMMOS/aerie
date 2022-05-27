import * as AST from './scheduler-ast.js';
import * as WindowsEDSL from './windows-edsl-fluent-api'

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
  public static CoexistenceGoal(opts: { activityTemplate: ActivityTemplate, forEach: WindowsEDSL.WindowSet }): ActivityCoexistenceGoal {
    return Goal.new({
      kind: AST.NodeKind.ActivityCoexistenceGoal,
      activityTemplate: opts.activityTemplate,
      forEach: opts.forEach.__astnode,
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

declare global {
  class Goal {
    public readonly __astNode: AST.GoalSpecifier;
    public and(...others: Goal[]): Goal

    public or(...others: Goal[]): Goal

    public static ActivityRecurrenceGoal(opts: { activityTemplate: ActivityTemplate, interval: Duration }): ActivityRecurrenceGoal

    public static CoexistenceGoal(opts: { activityTemplate: ActivityTemplate, forEach: WindowsEDSL.WindowSet }): ActivityCoexistenceGoal

    public static CardinalityGoal(opts: { activityTemplate: ActivityTemplate, specification: AST.CardinalityGoalArguments, inPeriod: ClosedOpenInterval }): ActivityCardinalityGoal
  }
  type Duration = number;
  type Double = number;
  type Integer = number;
}

export interface ClosedOpenInterval extends AST.ClosedOpenInterval {}
export interface ActivityTemplate extends AST.ActivityTemplate {}

// Make Goal available on the global object
Object.assign(globalThis, { Goal });
