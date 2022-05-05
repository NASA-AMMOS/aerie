import * as AST from './scheduler-ast.js';
import { ActivityType } from "./mission-model-generated-code";

interface ActivityRecurrenceGoal extends Goal {}
interface ActivityCoexistenceGoal extends Goal {}

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
  public static CoexistenceGoal(opts: { activityTemplate: ActivityTemplate, forEach: ActivityType }): ActivityCoexistenceGoal {
    return Goal.new({
      kind: AST.NodeKind.ActivityCoexistenceGoal,
      activityTemplate: opts.activityTemplate,
      forEach: {
        type: opts.forEach
      },
    });
  }
}

declare global {
  export class Goal {
    public readonly __astNode: AST.GoalSpecifier;
    public and(...others: Goal[]): Goal

    public or(...others: Goal[]): Goal

    public static ActivityRecurrenceGoal(opts: { activityTemplate: ActivityTemplate, interval: Duration }): ActivityRecurrenceGoal

    public static CoexistenceGoal(opts: { activityTemplate: ActivityTemplate, forEach: ActivityType }): ActivityCoexistenceGoal
  }
  type Duration = number;
  type Double = number;
  type Integer = number;
}

export interface ActivityTemplate extends AST.ActivityTemplate {}

// Make Goal available on the global object
Object.assign(globalThis, { Goal });
