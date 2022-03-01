import * as AST from './scheduler-ast.js'

// For accessing the private field of WindowSet from Goal
const NEW_SYMBOL = Symbol('NEW_SYMBOL');
const GET_INTERNAL_SYMBOL = Symbol('GET_INTERNAL_SYMBOL');

export class WindowSetSpecifier {
  protected readonly windowSpecifier: AST.WindowSetSpecifier;

  protected constructor(windowSpecifier: AST.WindowSetSpecifier) {
    this.windowSpecifier = windowSpecifier;
  }

  protected static new(windowSpecifier: AST.WindowSetSpecifier): WindowSetSpecifier {
    return new WindowSetSpecifier(windowSpecifier);
  }

  public get [GET_INTERNAL_SYMBOL]() {
    return this.windowSpecifier;
  }
}

export class WindowSet extends WindowSetSpecifier {

  protected static new(windowSpecifier: AST.WindowSetSpecifier): WindowSet {
    return new WindowSet(windowSpecifier);
  }

  public get combine(): {
    intersection: (...others: WindowSet[]) => WindowSet,
    union: (...others: WindowSet[]) => WindowSet,
    difference: (...others: WindowSet[]) => WindowSet,
  } {
    return {
      intersection: (...others: WindowSet[]): WindowSet => {
        return WindowSet.intersection_internal(this, ...others);
      },
      union: (...others: WindowSet[]): WindowSet => {
        return WindowSet.union_internal(this, ...others);
      },
      difference: (other: WindowSet): WindowSet => {
        return WindowSet.difference_internal(this, other);
      },
    }
  }

  public get filter(): WindowSetFilter  {
    return WindowSetFilter[NEW_SYMBOL](this.windowSpecifier as AST.WindowSetFilterOperator);
  }

  public get map(): {
    split: (windows: WindowSet, duration: AST.Duration) => WindowSet,
    shift: (windows: WindowSet, duration: AST.Duration) => WindowSet,
    trim: (windows: WindowSet, duration: AST.Duration) => WindowSet,
    extend: (windows: WindowSet, duration: AST.Duration) => WindowSet,
  } {
    return {
      split: (windows: WindowSet, duration: AST.Duration): WindowSet => {
        return WindowSet.new({
          kind: 'WindowSetMapOperatorSplit',
          windows: this.windowSpecifier,
          duration,
        });
      },
      shift: (windows: WindowSet, duration: AST.Duration): WindowSet => {
        return WindowSet.new({
          kind: 'WindowSetMapOperatorShift',
          windows: this.windowSpecifier,
          duration,
        });
      },
      trim: (windows: WindowSet, duration: AST.Duration): WindowSet => {
        return WindowSet.new({
          kind: 'WindowSetMapOperatorTrim',
          windows: this.windowSpecifier,
          duration,
        });
      },
      extend: (windows: WindowSet, duration: AST.Duration): WindowSet => {
        return WindowSet.new({
          kind: 'WindowSetMapOperatorExtend',
          windows: this.windowSpecifier,
          duration,
        });
      },
    };
  }

  public static eq<T extends AST.ResourceValueTypes>(resource: ResourceValue<T>, value: T): WindowSet {
    return WindowSet.new({
      kind: 'ConstraintOperatorEqual',
      left: String(resource),
      right: value,
    });
  }

  public static lt<T extends AST.ResourceValueTypes>(resource: ResourceValue<T>, value: T): WindowSet {
    return WindowSet.new({
      kind: 'ConstraintOperatorLessThan',
      left: String(resource),
      right: value,
    });
  }

  // public static lte<T extends AST.ResourceValueTypes>(resource: ResourceValue<T>, value: T): WindowSet {
  //   return WindowSet.union_internal(
  //     WindowSet.eq(resource, value),
  //     WindowSet.lt(resource, value),
  //   );
  // }

  public static gt<T extends AST.ResourceValueTypes>(resource: ResourceValue<T>, value: T): WindowSet {
    return WindowSet.new({
      kind: 'ConstraintOperatorGreaterThan',
      left: String(resource),
      right: value,
    });
  }

  // public static gte<T extends AST.ResourceValueTypes>(resource: ResourceValue<T>, value: T): WindowSet {
  //   return WindowSet.union_internal(
  //     WindowSet.eq(resource, value),
  //     WindowSet.gt(resource, value),
  //   );
  // }

  public static get entirePlanWindow(): WindowSet {
    return WindowSet.new({
      kind: 'ConstraintOperatorEntirePlanWindow',
    });
  }

  private static intersection_internal(...windowSets: WindowSet[]): WindowSet {
    return WindowSet.new({
      kind: 'WindowSetCombineOperatorIntersection',
      expressions: [
        ...windowSets.map(windowSet => windowSet.windowSpecifier),
      ],
    });
  }

  private static union_internal(...windowSets: WindowSet[]): WindowSet {
    return WindowSet.new({
      kind: 'WindowSetCombineOperatorUnion',
      expressions: [
        ...windowSets.map(windowSet => windowSet.windowSpecifier),
      ],
    });
  }

  private static difference_internal(left: WindowSet, right: WindowSet): WindowSet {
    return WindowSet.new({
      kind: 'WindowSetCombineOperatorDifference',
      left: left.windowSpecifier,
      right: right.windowSpecifier,
    });
  }
}

export class WindowSetFilter extends WindowSet {

  protected windowSpecifier: AST.WindowSetFilterOperator | AST.WindowSetFilterModificationOperator;

  protected constructor(windowSpecifier: AST.WindowSetFilterOperator | AST.WindowSetFilterModificationOperator) {
    super(windowSpecifier);
    this.windowSpecifier = windowSpecifier;
  }

  protected static new(windowSpecifier: AST.WindowSetFilterOperator | AST.WindowSetFilterModificationOperator): WindowSetFilter {
    return new WindowSetFilter(windowSpecifier);
  }

  public static [NEW_SYMBOL](windowSpecifier: AST.WindowSetFilterOperator | AST.WindowSetFilterModificationOperator): WindowSetFilter {
    return WindowSetFilter.new(windowSpecifier);
  }

  public eq<T extends AST.WindowPropertyValueTypes>(property: WindowPropertyValue<T>, value: T): WindowSetFilter {
    return WindowSetFilter.new({
      kind: 'WindowSetFilterOperatorEqual',
      windows: this.windowSpecifier,
      left: String(property),
      right: value,
    });
  }

  public lt<T extends AST.WindowPropertyValueTypes>(property: WindowPropertyValue<T>, value: T): WindowSetFilter {
    return WindowSetFilter.new({
      kind: 'WindowSetFilterOperatorLessThan',
      windows: this.windowSpecifier,
      left: String(property),
      right: value,
    });
  }

  // public lte<T extends AST.WindowPropertyValueTypes>(property: WindowPropertyValue<T>, value: T): WindowSetFilter {
  //   return this.eq(property, value).or(this.lt(property, value));
  // }

  public gt<T extends AST.WindowPropertyValueTypes>(property: WindowPropertyValue<T>, value: T): WindowSetFilter {
    return WindowSetFilter.new({
      kind: 'WindowSetFilterOperatorGreaterThan',
      windows: this.windowSpecifier,
      left: String(property),
      right: value,
    });
  }

  // public gte<T extends AST.WindowPropertyValueTypes>(property: WindowPropertyValue<T>, value: T): WindowSetFilter {
  //   return this.eq(property, value).or(this.gt(property, value));
  // }


  public get not(): {
    eq: (property: WindowPropertyValue, value: AST.WindowPropertyValueTypes) => WindowSetFilter,
    lt: (property: WindowPropertyValue, value: AST.WindowPropertyValueTypes) => WindowSetFilter,
    // lte: (property: WindowPropertyValue, value: AST.WindowPropertyValueTypes) => WindowSetFilter,
    gt: (property: WindowPropertyValue, value: AST.WindowPropertyValueTypes) => WindowSetFilter,
    // gte: (property: WindowPropertyValue, value: AST.WindowPropertyValueTypes) => WindowSetFilter,
  } {
    return {
      eq: (property: WindowPropertyValue, value: AST.WindowPropertyValueTypes): WindowSetFilter => {
        return WindowSetFilter.filter_not_internal(this.eq(property, value));
      },
      lt: (property: WindowPropertyValue, value: AST.WindowPropertyValueTypes): WindowSetFilter => {
        return WindowSetFilter.filter_not_internal(this.lt(property, value));
      },
      // lte: (property: WindowPropertyValue, value: AST.WindowPropertyValueTypes): WindowSetFilter => {
      //   return WindowSetFilter.filter_not_internal(this.lte(property, value));
      // },
      gt: (property: WindowPropertyValue, value: AST.WindowPropertyValueTypes): WindowSetFilter => {
        return WindowSetFilter.filter_not_internal(this.gt(property, value));
      },
      // gte: (property: WindowPropertyValue, value: AST.WindowPropertyValueTypes): WindowSetFilter => {
      //   return WindowSetFilter.filter_not_internal(this.gte(property, value));
      // },
    };
  }

  public and(...others: WindowSetFilter[]): WindowSetFilter {
    return WindowSetFilter.new({
      kind: 'WindowSetFilterModificationOperatorAnd',
      expressions: [
        this.windowSpecifier as AST.WindowSetFilterOperator,
        ...others.map(other => other.windowSpecifier) as AST.WindowSetFilterOperator[],
      ],
    });
  }

  public or(...others: WindowSetFilter[]): WindowSetFilter {
    return WindowSetFilter.new({
      kind: 'WindowSetFilterModificationOperatorOr',
      expressions: [
        this.windowSpecifier as AST.WindowSetFilterOperator,
        ...others.map(other => other.windowSpecifier) as AST.WindowSetFilterOperator[],
      ],
    });
  }

  private static filter_not_internal(filter: WindowSetFilter): WindowSetFilter {
    return WindowSetFilter.new({
      kind: 'WindowSetFilterModificationOperatorNot',
      expression: filter.windowSpecifier as AST.WindowSetFilterOperator,
    });
  }

}

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

  public static ActivityRecurrenceGoal(opts: { windowSet: WindowSet, activityTemplate: ActivityTemplate, interval: AST.Integer }): ActivityRecurrenceGoal {
    return Goal.new({
      kind: 'ActivityRecurrenceGoal',
      windows: opts.windowSet[GET_INTERNAL_SYMBOL],
      activityTemplate: opts.activityTemplate,
      interval: opts.interval,
    });
  }
}

interface WindowPropertyValue<T = AST.WindowPropertyValueTypes> extends Symbol {}
interface DurationWindowProperty extends WindowPropertyValue<AST.Duration> {}
export const WindowProperties = {
  Duration: Symbol('WindowProperty::Duration') as DurationWindowProperty,
};

interface ResourceValue<T = AST.ResourceValueTypes> extends Symbol {}

/** Start Codegen */
interface FruitResourceValue extends ResourceValue<AST.Integer> {}
export const Resources = {
  Fruit: Symbol('Resource::Fruit') as FruitResourceValue,
};
/** End Codegen */

interface ActivityTemplate extends AST.ActivityTemplate {}
/** Start Codegen */
interface PeelBanana extends ActivityTemplate {}
export const ActivityTemplates = {
  PeelBanana: function PeelBanana(name: string, args: { peelDirection: 'fromStem' | 'fromTip' }): PeelBanana {
    return {
      name,
      activityType: 'PeelBanana',
      arguments: args,
    };
  },
};
/** End Codegen */

/** Utility Functions */
export function serializeWindowSetSpecifier(windowSetSpecifier: WindowSetSpecifier): AST.WindowSetSpecifier {
  return windowSetSpecifier[GET_INTERNAL_SYMBOL];
}
export function serializeGoal(goal: Goal): AST.GoalSpecifier {
  return goal[GET_INTERNAL_SYMBOL];
}

// declare global {
//
//   const WindowProperties: {
//     Duration: WindowPropertyValue<double>
//   };
//
//   class WindowSet {
//     combine: {
//       union: (...windows: WindowSet[]) => WindowSet,
//       intersection: (...windows: WindowSet[]) => WindowSet,
//       complement: (window: WindowSet) => WindowSet,
//     };
//     filter: {
//       eq: <T extends AST.WindowPropertyValueTypes>(property: WindowPropertyValue<T>, value: T) => WindowSet,
//       lt: <T extends AST.WindowPropertyValueTypes>(property: WindowPropertyValue<T>, value: T) => WindowSet,
//       gt: <T extends AST.WindowPropertyValueTypes>(property: WindowPropertyValue<T>, value: T) => WindowSet,
//     };
//     map: {
//       split: (windows: WindowSet, duration: duration) => WindowSet,
//     };
//     static eq<T extends AST.ResourceValueTypes>(resource: ResourceValue<T>, value: T): WindowSet;
//     static lt<T extends AST.ResourceValueTypes>(resource: ResourceValue<T>, value: T): WindowSet;
//     static lte<T extends AST.ResourceValueTypes>(resource: ResourceValue<T>, value: T): WindowSet;
//     static gt<T extends AST.ResourceValueTypes>(resource: ResourceValue<T>, value: T): WindowSet;
//     static gte<T extends AST.ResourceValueTypes>(resource: ResourceValue<T>, value: T): WindowSet;
//   }
//
//   class Goal {
//     public and(...others: Goal[]): Goal;
//     public or(...others: Goal[]): Goal;
//     public static ActivityRecurrenceGoal(windowSet: WindowSet, activityTemplate: ActivityTemplate, rangeToGenerate: [integer, integer]): Goal;
//   }
// }
