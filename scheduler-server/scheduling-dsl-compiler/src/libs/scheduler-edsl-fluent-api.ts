import * as AST from './scheduler-ast.js'

// For accessing the private field of WindowSet from Goal
const GET_INTERNAL_SYMBOL = Symbol('GET_INTERNAL_SYMBOL');

interface ActivityRecurrenceGoal extends Goal {}
export class Goal {
  private readonly goalSpecifier: AST.GoalSpecifier;

  private constructor(goalSpecifier: AST.GoalSpecifier) {
    this.goalSpecifier = goalSpecifier;
  }

  private static new(goalSpecifier: AST.GoalSpecifier): Goal {
    return new Goal(goalSpecifier);
  }

  public get [GET_INTERNAL_SYMBOL](): AST.GoalSpecifier {
    return this.goalSpecifier;
  }

  public and(...others: Goal[]): Goal {
    return Goal.new({
      kind: 'GoalAnd',
      goals: [
        this.goalSpecifier,
        ...others.map(other => other.goalSpecifier),
      ],
    });
  }

  public or(...others: Goal[]): Goal {
    return Goal.new({
      kind: 'GoalOr',
      goals: [
        this.goalSpecifier,
        ...others.map(other => other.goalSpecifier),
      ],
    });
  }

  public static ActivityRecurrenceGoal(opts: { activityTemplate: ActivityTemplate, interval: Integer }): ActivityRecurrenceGoal {
    return Goal.new({
      kind: 'ActivityRecurrenceGoal',
      activityTemplate: opts.activityTemplate,
      interval: opts.interval,
    });
  }
}

interface ActivityTemplate extends AST.ActivityTemplate {}

export function serializeGoal(goal: Goal): AST.GoalSpecifier {
  return goal[GET_INTERNAL_SYMBOL];
}

