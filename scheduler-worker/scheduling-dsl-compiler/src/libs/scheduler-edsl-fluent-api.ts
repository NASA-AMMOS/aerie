/**
 * This module contains the elements you need to write scheduling goals. To start, navigate to {@link Goal } to find the goal constructors.
 *
 * @module Scheduling eDSL
 * @packageDocumentation
 */

import * as AST from "./scheduler-ast.js";
import  * as WindowsEDSL from "./constraints-edsl-fluent-api.js";
import {ActivityInstance} from "./constraints-edsl-fluent-api.js";
import * as ConstraintsAST from "./constraints-ast.js";

type WindowProperty = AST.WindowProperty
type TimingConstraintOperator = AST.TimingConstraintOperator

export type { CardinalityGoalArguments, WindowProperty, TimingConstraintOperator } from "./scheduler-ast.js";

export class GlobalSchedulingCondition {

  public readonly __astNode: AST.GlobalSchedulingConditionSpecifier

  /** @internal **/
  private constructor(__astNode: AST.GlobalSchedulingConditionSpecifier) {
    this.__astNode = __astNode;
  }

  /** @internal **/
  private static new(__astNode: AST.GlobalSchedulingConditionSpecifier): GlobalSchedulingCondition {
    return new GlobalSchedulingCondition(__astNode);
  }

  /**
   * Allows scheduling of any activity type only when passed expression evaluates to true
   * @param expression the expression
   */
  public static scheduleActivitiesOnlyWhen(expression: WindowsEDSL.Windows): GlobalSchedulingCondition {
    return GlobalSchedulingCondition.new({
          kind: AST.NodeKind.GlobalSchedulingCondition,
          expression: expression.__astNode,
          activityTypes: Object.values(WindowsEDSL.Gen.ActivityType)
        }
    )
  }

  /**
   * Allows scheduling of argument activity types only when passed expression evaluates to true
   * @param activityTypes the activity types
   * @param expression the expression
   */
  public static scheduleOnlyWhen(activityTypes: WindowsEDSL.Gen.ActivityType[], expression: WindowsEDSL.Windows): GlobalSchedulingCondition {
    return GlobalSchedulingCondition.new({
          kind: AST.NodeKind.GlobalSchedulingCondition,
          expression: expression.__astNode,
          activityTypes: activityTypes
        }
    )
  }

  /**
   * Creates a mutual exclusion global condition preventing instantiation of activity types from the first list from
   * overlapping instantiations of activity types from the second list. Relation is reflexive: mutex(A,B) = mutex(B,A).
   *
   * @param activityTypes1 a list of activity types
   * @param activityTypes2 a list of activity types
   */
  public static mutex(activityTypes1: WindowsEDSL.Gen.ActivityType[],
                      activityTypes2: WindowsEDSL.Gen.ActivityType[]): GlobalSchedulingCondition {
    return GlobalSchedulingCondition.new({
      kind: AST.NodeKind.GlobalSchedulingConditionAnd,
      conditions: [
        GlobalSchedulingCondition.new({
          kind: AST.NodeKind.GlobalSchedulingCondition,
          expression: WindowsEDSL.Windows.During(...activityTypes2).not().__astNode,
          activityTypes: activityTypes1
        }).__astNode,
        GlobalSchedulingCondition.new({
              kind: AST.NodeKind.GlobalSchedulingCondition,
              expression: WindowsEDSL.Windows.During(...activityTypes1).not().__astNode,
              activityTypes: activityTypes2
            }
        ).__astNode
      ]
    })
  }
}

/**
 * This class represents allows to represent and specify goals.
 */
export class Goal {
  /** @internal **/
  public readonly __astNode: AST.GoalSpecifier;
  private static __numGeneratedAliases: number = 0;

  /** @internal **/
  private constructor(__astNode: AST.GoalSpecifier) {
    this.__astNode = __astNode;
  }

  /** @internal **/
  private static new(__astNode: AST.GoalSpecifier): Goal {
    return new Goal(__astNode);
  }

  /**
   *
   * The AND Goal aggregates several goals together and specifies that at least one of them must be satisfied.
   *
   * #### Inputs
   * - **goals**: an ordered list of goals (here below referenced as the subgoals)
   *
   * #### Behavior
   * The scheduler will try to satisfy each subgoal in the list. If a subgoal is only partially satisfied, the scheduler will not backtrack and will let the inserted activities in the plan. If all the subgoals are satisfied, the AND goal will appear satisfied. If one or several subgoals have not been satisfied, the AND goal will appear unsatisfied.
   *
   * #### Examples
   *
   * ```typescript
   * export default function myGoal() {
   *     return Goal.CoexistenceGoal({
   *       forEach: Real.Resource("/fruit").equal(4.0),
   *       activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
   *       endsAt: TimingConstraint.singleton(WindowProperty.END).plus(Temporal.Duration.from({ minutes: 5 }))
   *      }).and(
   *       Goal.CardinalityGoal({
   *             activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
   *             specification: { occurrence : 10 }
   *        }))
   * }
   * ```
   * The AND goal above has two subgoals. The coexistence goal will place activities of type `PeelBanana` everytime the `/fruit` resource is equal to 4. The second goal will place 10 occurrences of the same kind of activities `PeelBanana`. The first subgoal will be evaluated first and will place a certain number of `PeelBanana` activities in the plan. When the second goal will be evaluated, it will count already present `PeelBanana` activities and insert the missing number. Imagine the first goals leads to inserting 2 activities. The second goal will then have to place 8 activities to be satisfied.
   * @param others the list of goals
   */
  public and(...others: Goal[]): Goal {
    return Goal.new({
      kind: AST.NodeKind.GoalAnd,
      goals: [
        this.__astNode,
        ...others.map(other => other.__astNode),
      ],
      shouldRollbackIfUnsatisfied: false
    });
  }

  /**
   *
   * The OR Goal aggregates several goals together and specifies that at least one of them must be satisfied.
   *
   * #### Inputs
   * - **goals**: a list of goals (here below referenced as the subgoals)
   *
   * #### Behavior
   * The scheduler will try to satisfy each subgoal in the list until one is satisfied. If a subgoal is only partially satisfied, the scheduler will not backtrack and will let the inserted activities in the plan.
   *
   * #### Examples
   *
   * ```typescript
   * export default function myGoal() {
   *     return Goal.CardinalityGoal({
   *              activityTemplate: ActivityTemplates.GrowBanana({
   *                quantity: 1,
   *                growingDuration: Temporal.Duration.from({ hours: 1 })
   *            }),
   *           specification: { occurrence : 10 }
   *           }).or(
   *            Goal.ActivityRecurrenceGoal({
   *             activityTemplate: ActivityTemplates.GrowBanana({
   *             quantity: 1,
   *             growingDuration: Temporal.Duration.from({ hours: 1 })
   *           }),
   *           interval: Temporal.Duration.from({ hours: 2 })
   *         }))
   * }
   * ```
   *
   * If the plan has a 24-hour planning horizon, the OR goal above will try placing activities of the `GrowBanana` type. The first subgoal will try placing 10 1-hour occurrences. If it fails to do so, because the planning horizon is maybe too short, it will then try to schedule 1 activity every 2 hours for the duration of the planning horizon.
   *
   * It may fail to achieve both subgoals but as the scheduler does not backtrack for now, activities inserted by any of the subgoals are kept in the plan.
   * @param others the list of goals
   */
  public or(...others: Goal[]): Goal {
    return Goal.new({
      kind: AST.NodeKind.GoalOr,
      goals: [
        this.__astNode,
        ...others.map(other => other.__astNode),
      ],
      shouldRollbackIfUnsatisfied: false
    });
  }

  /**
   * Restricts the windows on which a goal is applied
   *
   *
   * By default, a goal applies on the whole planning horizon. The Aerie scheduler provides support for restricting _when_ a goal applies with the `.applyWhen()` method in the `Goal` class. This node allows users to provide a set of windows (`Windows`, see [documentation](../../constraints-edsl-api/classes/Windows)) which could be a time or a resource-based window.
   *
   * The `.applyWhen()` method, takes one argument: the windows (in the form of an expression) that the goal should apply over. What follows is an example that applies a daily recurrence goal only when a given resource is greater than 2. If the resource is less than two, then the goal is no longer applied.
   *
   * ```typescript
   * export default function myGoal() {
   *     return Goal.ActivityRecurrenceGoal({
   *             activityTemplate: ActivityTemplates.GrowBanana({
   *             quantity: 1,
   *             growingDuration: Temporal.Duration.from({ hours: 1 })
   *           }),
   *           interval: Temporal.Duration.from({ hours: 2 })
   *         }).applyWhen(Real.Resource("/fruit").greaterThan(2))
   * }
   * ```
   *
   * #### Notes on boundaries:
   * * If you are trying to schedule an activity, or a recurrence within a window but that window cuts off either the activity or the recurrence interval (depending on the goal type), it will not be scheduled. For example, if you had a recurrence interval of 3 seconds, scheduling a 2 second activity each recurrence, and had the following window, you'd get the following:
   * ```
   * RECURRENCE INTERVAL: [++-++-++-]
   * GOAL WINDOW:         [+++++----]
   * RESULT:              [++-------]
   * ```
   * That, is, the second activity won't be scheduled as the goal window cuts off its recurrence interval.
   * * Scheduling is _local_, not global. This means for every window that is matched (as it is possible to have disjoint windows, imagine a resource that fluctuates upward and downward but only applying that goal when the resource is over a certain value), the goal is applied individually. So, for that same recurrence interval setup as before, we could have:
   * ```
   * RECURRENCE INTERVAL: [++-++-++-++-]
   * GOAL WINDOW:         [+++++--+++--]
   * RESULT:              [++-----++---] //(the second one is applied independently of the first!)
   * ```
   * * When mapping out a temporal window to apply a goal over, keep in mind that the ending boundary of the goal is _exclusive_, i.e. if I want to apply a goal in the window of 10-12 seconds, it will apply only on seconds 10 and 11. This is in line with the [fencepost problem](https://en.wikipedia.org/wiki/Off-by-one_error#Fencepost_error).
   *
   * @param windows the windows on which this goal applies
   * @returns a new goal applying on a restricted horizon
   */
  public applyWhen(windows: WindowsEDSL.Windows): Goal {
    return Goal.new({
      kind: AST.NodeKind.ApplyWhen,
      goal: this.__astNode,
      window: windows.__astNode
    });
  }

  /**
   * Creates an ActivityRecurrenceGoal
   *
   * The Activity Recurrence Goal (sometimes referred to as a "frequency goal") specifies that a certain activity should occur repeatedly throughout the plan, at some given interval.
   *
   * #### Inputs
   * - activityTemplate: the description of the activity whose recurrence we're interested in.
   * - interval: a Duration of time specifying how often this activity must occur
   *
   * #### Behavior
   * This interval is treated as an upper bound - so if the activity occurs more frequently, that is not considered a failure.
   *
   * The scheduler will find places in the plan where the given activity has not occurred within the given interval, and it will place an instance of that activity there.
   *
   * > Note: The interval is measured between the _start times_ of two activity instances. Neither the duration, nor the end time of the activity are examined by this goal.
   *
   * #### Example
   * ```typescript
   * export default function myGoal() {
   *     return Goal.ActivityRecurrenceGoal({
   *             activityTemplate: ActivityTemplates.GrowBanana({
   *             quantity: 1,
   *             growingDuration: Temporal.Duration.from({ hours: 1 })
   *           }),
   *           interval: Temporal.Duration.from({ hours: 2 })
   *         })
   * }
   * ```
   *
   * The goal above will place a `GrowBanana` activity in every 2-hour period of time that does not already contain one with the exact same parameters.
   *
   * @param opts an object containing the activity template and the interval at which the activities must be placed
   */
  public static ActivityRecurrenceGoal <A extends WindowsEDSL.Gen.ActivityType>(opts: { activityTemplate: ActivityTemplate<A>, interval: Temporal.Duration }): Goal {
    return Goal.new({
      kind: AST.NodeKind.ActivityRecurrenceGoal,
      activityTemplate: opts.activityTemplate,
      interval: opts.interval,
      shouldRollbackIfUnsatisfied: false
    });
  }

  /**
   * Specifies whether the scheduler should rollback or not if the goal is only partially satisfied.
   * Rolling back is removing all inserted activities to satisfy the goal as well as removing associations to existing activities
   * @param shouldRollbackIfUnsatisfied boolean specifying the behavior of the scheduler in terms of rollback
   */
  public shouldRollbackIfUnsatisfied(shouldRollbackIfUnsatisfied: boolean): Goal{
    if('shouldRollbackIfUnsatisfied' in this.__astNode){
      this.__astNode['shouldRollbackIfUnsatisfied'] = shouldRollbackIfUnsatisfied
    }
    return this
  }

  /**
   * Creates a coexistence goal. The coexistence goal places one activity for each passed window.
   * The start and end time of each activity can be parametrized relatively to its coexisting window with temporal constraints.
   *
   * The Coexistence Goal specifies that a certain activity should occur once **for each** occurrence of some condition.
   *
   * #### Inputs
   * - **forEach**: a set of time windows (`Windows`, see [documentation](../../constraints-edsl-api/classes/Windows) on how to produce such an expression) or a set of activities (`ActivityExpression`)
   * - **activityTemplate**: the description of the activity to insert after each activity identified by `forEach`
   * - **startsAt**: optionally specify a specific time when the activity should start relative to the window
   * - **startsWithin**: optionally specify a range when the activity should start relative to the window
   * - **endsAt**: optionally specify a specific time when the activity should end relative to the window
   * - **endsWithin**: optionally specify a range when the activity should end relative to the window
   *
   * NOTE: Either the start or end of the activity must be constrained. This means that at least **1** of the 4 properties `startsAt`, `startsWithin`, `endsAt`, `endsWithin` must be given.
   *
   *
   * #### Behavior
   * The scheduler will find places in the plan where the `forEach` condition is true, and if not, it will insert a new instance using the given `activityTemplate` and temporal constraints.
   *
   * #### Examples
   *
   * ```typescript
   * export default () => Goal.CoexistenceGoal({
   *   forEach: ActivityExpression.ofType(ActivityTypes.GrowBanana),
   *   activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
   *   startsAt: TimingConstraint.singleton(WindowProperty.END).plus(Temporal.Duration.from({ minutes: 5 }))
   * })
   * ```
   * Behavior: for each activity A of type `GrowBanana` present in the plan when the goal is evaluated, place an activity of type `PeelBanana` starting exactly at the end of A + 5 minutes.
   *
   * ```typescript
   * export default () => Goal.CoexistenceGoal({
   *   forEach: ActivityExpression.ofType(ActivityTypes.GrowBanana),
   *   activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
   *   startsWithin: TimingConstraint.range(WindowProperty.END, Operator.PLUS, Temporal.Duration.from({ minutes: 5 })),
   *   endsWithin: TimingConstraint.range(WindowProperty.END, Operator.PLUS, Temporal.Duration.from({ minutes: 6 }))
   * })
   * ```
   *
   * Behavior: for each activity A of type `GrowBanana` present in the plan when the goal is evaluated, place an activity of type `PeelBanana` starting in the interval [end of A, end of A + 5 minutes] and ending in the interval [end of A, end of A + 6 minutes].
   *
   *
   * ```typescript
   * export default () => Goal.CoexistenceGoal({
   *   forEach: Real.Resource("/fruit").equal(4.0),
   *   activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
   *   endsAt: TimingConstraint.singleton(WindowProperty.END).plus(Temporal.Duration.from({ minutes: 5 }))
   * })
   * ```
   * Behavior: for each continuous period of time during which the `/fruit` resource is equal to 4, place an activity of type `PeelBanana` ending exactly at the end of A + 6 minutes. Note that the scheduler will allow a default timing error of 500 milliseconds for temporal constraints. This parameter will be configurable in an upcoming release.
   *
   * KNOWN ISSUE: If the end is unconstrained while the activity has an uncontrollable duration, the scheduler may fail to place the activity. To work around this, add an `endsWithin` constraint that encompasses your expectation for the duration of the activity - this will help the scheduler narrow the search space.
   *
   * @param opts an object containing the activity template, a set of windows, and optionally temporal constraints.
   */

  public static CoexistenceGoal<T extends WindowsEDSL.Gen.ActivityType, S extends WindowsEDSL.Gen.ActivityType>(opts: ({
    activityTemplate: (( interval: WindowsEDSL.Interval ) => ActivityTemplate<S>) | ActivityTemplate<S>,
    forEach:  WindowsEDSL.Windows,
  } | {
    activityTemplate: (( span: ActivityInstance<T> ) => ActivityTemplate<S>) | ActivityTemplate<S>,
    forEach:  ActivityExpression<T>,
  }) & CoexistenceGoalTimingConstraints): Goal {

    let alias: string;

    if (opts.forEach instanceof WindowsEDSL.Windows) {
      alias = 'coexistence interval alias ' + Goal.__numGeneratedAliases;
    } else {
      alias = 'coexistence activity alias ' + Goal.__numGeneratedAliases;
    }
    Goal.__numGeneratedAliases += 1;

    let activityTemplate: ActivityTemplate<S>;

    if (opts.activityTemplate instanceof Function) {
      if (opts.forEach instanceof WindowsEDSL.Windows) {
        activityTemplate = (opts.activityTemplate as (i: WindowsEDSL.Interval) => ActivityTemplate<S>)(new WindowsEDSL.Interval({
          kind: ConstraintsAST.NodeKind.IntervalAlias,
          alias
        }));
      } else {
        activityTemplate = (opts.activityTemplate as (a: ActivityInstance<T>) => ActivityTemplate<S>)(new ActivityInstance((<ActivityExpression<T>>opts.forEach).activityType, alias));
      }
    } else {
      activityTemplate = opts.activityTemplate;
    }

    return Goal.new({
      kind: AST.NodeKind.ActivityCoexistenceGoal,
      alias,
      activityTemplate,
      forEach: opts.forEach.__astNode,
      startConstraint: (("startsAt" in opts) ? opts.startsAt.__astNode : ("startsWithin" in opts) ? opts.startsWithin.__astNode : undefined),
      endConstraint: (("endsAt" in opts) ? opts.endsAt.__astNode : ("endsWithin" in opts) ? opts.endsWithin.__astNode : undefined),
      shouldRollbackIfUnsatisfied: false
    });
  }

  /**
   * The Cardinality Goal specifies that a certain activity should occur in the plan either a certain number of times, or for a certain total duration.
   * #### Inputs
   * - **activityTemplate**: the description of the activity whose recurrence we're interested in.
   * - **specification**: an object with either an `occurrence` field, a `duration` field, or both (see examples below).
   *
   * #### Behavior
   * The duration and occurrence are treated as lower bounds - so if the activity occurs more times, or for a longer duration, that is not considered a failure, and the scheduler will not add any more activities.
   *
   * The scheduler will identify whether it not the plan has enough occurrences or total duration of the given activity template. If not, it will add activities until satisfaction.
   *
   * #### Examples
   *
   * Setting a lower bound on the total duration:
   *
   * ```typescript
   * export default function myGoal() {
   *     return Goal.CardinalityGoal({
   *         activityTemplate: ActivityTemplates.GrowBanana({
   *             quantity: 1,
   *             growingDuration: Temporal.Duration.from({ seconds: 1 }),
   *         }),
   *         specification: { duration: Temporal.Duration.from({ seconds: 10 }) }
   *     })
   * }
   * ```
   *
   * Setting a lower bound on the number of occurrences:
   *
   * ```typescript
   * export default function myGoal() {
   *     return Goal.CardinalityGoal({
   *         activityTemplate: ActivityTemplates.GrowBanana({
   *             quantity: 1,
   *             growingDuration: Temporal.Duration.from({ seconds: 1 }),
   *         }),
   *         specification: { occurrence: 10 }
   *     })
   * }
   * ```
   *
   * Combining the two:
   *
   * ```typescript
   * export default function myGoal() {
   *     return Goal.CardinalityGoal({
   *         activityTemplate: ActivityTemplates.GrowBanana({
   *             quantity: 1,
   *             growingDuration: Temporal.Duration.from({ seconds: 1 }),
   *         }),
   *         specification: { occurrence: 10, duration: Temporal.Duration.from({ seconds: 10 }) }
   *     })
   * }
   * ```
   *
   * NOTE: In order to avoid placing multiple activities at the same start time, the Cardinality goal introduces an assumed mutual exclusion constraint - namely that new activities will not be allowed to overlap with existing activities.
   * @param opts an object containing the activity template and a  {@link ActivityCardinalityGoal} specification of what kind of cardinality is considered
   */
  public static CardinalityGoal <A extends WindowsEDSL.Gen.ActivityType>(opts: { activityTemplate: ActivityTemplate<A>, specification: AST.CardinalityGoalArguments }): Goal {
    return Goal.new({
      kind: AST.NodeKind.ActivityCardinalityGoal,
      activityTemplate: opts.activityTemplate,
      specification: opts.specification,
      shouldRollbackIfUnsatisfied: false
    });
  }
}

/**
 * An StartTimingConstraint is a constraint applying on the start time of an activity template.
 */
export type StartTimingConstraint = { startsAt: SingletonTimingConstraint | SingletonTimingConstraintNoOperator } | { startsWithin: RangeTimingConstraint }
/**
 * An EndTimingConstraint is a constraint applying on the end time of an activity template.
 */
export type EndTimingConstraint = { endsAt: SingletonTimingConstraint | SingletonTimingConstraintNoOperator } | {endsWithin: RangeTimingConstraint }
/**
 * An CoexistenceGoalTimingConstraints is a constraint that can be used to constrain the start or end times of activities in coexistence goals.
 */
export type CoexistenceGoalTimingConstraints = StartTimingConstraint | EndTimingConstraint | (StartTimingConstraint & EndTimingConstraint)

export class ActivityExpression<T extends WindowsEDSL.Gen.ActivityType> {
  /** @internal **/
  public readonly __astNode: AST.ActivityExpression;

  public readonly activityType: T;

  /** @internal **/
  private constructor(__astNode: AST.ActivityExpression, activityType: T ) {
    this.__astNode = __astNode;
    this.activityType = activityType
  }

  /** @internal **/
  private static new<T extends WindowsEDSL.Gen.ActivityType>(__astNode: AST.ActivityExpression, activityType: T): ActivityExpression<T> {
    return new ActivityExpression(__astNode, activityType);
  }

  /**
   * Creates an actvity expression of a type
   * @param activityType the type
   */
  public static ofType<T extends WindowsEDSL.Gen.ActivityType>(activityType: T): ActivityExpression<T> {
    return ActivityExpression.new({
          kind: AST.NodeKind.ActivityExpression,
          type: activityType
        },
        activityType)
  }
}

export class TimingConstraint {
  /** @internal **/
  private constructor() {}
  /**
   * The singleton timing constraint represents a precise time point
   * at some offset from either the start or end of a window.
   * @param windowProperty either WindowProperty.START or WindowProperty.END
   */
  public static singleton(windowProperty: WindowProperty): SingletonTimingConstraintNoOperator {
    return SingletonTimingConstraintNoOperator.new(windowProperty);
  }
  /**
   * The range timing constraint represents a range of acceptable times
   * relative to either the start or end of the window. The range will
   * be between the window "anchor" and the new point defined by the operator
   * and the offset.
   * @param windowProperty either WindowProperty.START or WindowProperty.END
   * @param operator either Operator.PLUS or Operator.MINUS
   * @param operand the duration offset
   */
  public static range(windowProperty: WindowProperty, operator: TimingConstraintOperator, operand: Temporal.Duration): RangeTimingConstraint {
    return RangeTimingConstraint.new({
      windowProperty,
      operator,
      operand,
      singleton: false
    })
  }
}

/**
 * Represents an operation on a timepoint.
 */
export class SingletonTimingConstraintNoOperator {
  /** @internal **/
  public readonly __astNode: AST.ActivityTimingConstraintSingleton
  /** @internal **/
  private constructor(__astNode: AST.ActivityTimingConstraintSingleton) {
    this.__astNode = __astNode;
  }
  /** @internal **/
  public static new(windowProperty: WindowProperty): SingletonTimingConstraintNoOperator {
    return new SingletonTimingConstraintNoOperator({
      windowProperty,
      operator: AST.TimingConstraintOperator.PLUS,
      operand: Temporal.Duration.from({ milliseconds: 0 }),
      singleton: true
    });
  }

  /**
   * Adds a duration to a timepoint
   * @param operand the duration to add
   */
  public plus(operand: Temporal.Duration): SingletonTimingConstraint {
    return SingletonTimingConstraint.new({
      ...this.__astNode,
      operator: AST.TimingConstraintOperator.PLUS,
      operand
    })
  }

  /**
   * Subtract a duration from a timepoint
   * @param operand the duration to subtract
   */
  public minus(operand: Temporal.Duration): SingletonTimingConstraint {
    return SingletonTimingConstraint.new({
      ...this.__astNode,
      operator: AST.TimingConstraintOperator.MINUS,
      operand
    })
  }
}
/**
 * A singleton timing constraint specifies that the start or the end time of an activity must be exaxctly equal to a timepoint. Use {@link TimingConstraint.singleton} to create such a constraint.
 */
export class SingletonTimingConstraint {
  /** @internal **/
  public readonly __astNode: AST.ActivityTimingConstraintSingleton
  /** @internal **/
  private constructor(__astNode: AST.ActivityTimingConstraintSingleton) {
    this.__astNode = __astNode;
  }
  /** @internal **/
  public static new(__astNode: AST.ActivityTimingConstraintSingleton): SingletonTimingConstraint {
    return new SingletonTimingConstraint(__astNode);
  }
}

/**
 * A range timing constraint specifies that the start or the end time of an activity must be comprised in an interval. Use {@link TimingConstraint.range} to create such a constraint.
 */
export class RangeTimingConstraint {
  /** @internal **/
  public readonly __astNode: AST.ActivityTimingConstraintRange
  /** @internal **/
  private constructor(__astNode: AST.ActivityTimingConstraintRange) {
    this.__astNode = __astNode;
  }
  /** @internal **/
  public static new(__astNode: AST.ActivityTimingConstraintRange): RangeTimingConstraint {
    return new RangeTimingConstraint(__astNode);
  }
}

declare global {
  class GlobalSchedulingCondition {

    public readonly __astNode: AST.GlobalSchedulingCondition;

    /**
     * Allows scheduling of any activity type only when passed expression evaluates to true
     * @param expression the expression
     */
    public static scheduleActivitiesOnlyWhen(expression : WindowsEDSL.Windows) : GlobalSchedulingCondition;

    /**
     * Allows scheduling of argument activity types only when passed expression evaluates to true
     * @param activityTypes the activity types
     * @param expression the expression
     */
    public static scheduleOnlyWhen(activityTypes: WindowsEDSL.Gen.ActivityType[], expression: WindowsEDSL.Windows): GlobalSchedulingCondition;

    /**
     * Creates a mutual exclusion global condition preventing instantiation of activity types from the first list from
     * overlapping instantiations of activity types from the second list. Relation is reflexive: mutex(A,B) = mutex(B,A).
     *
     * @param activityTypes1 a list of activity types
     * @param activityTypes2 a list of activity types
     */
    public static mutex(activityTypes1: WindowsEDSL.Gen.ActivityType[],
                        activityTypes2: WindowsEDSL.Gen.ActivityType[]): GlobalSchedulingCondition;
  }

  class Goal {
    public readonly __astNode: AST.GoalSpecifier;

    public shouldRollbackIfUnsatisfied(shouldRollbackIfUnsatisfied: boolean): Goal;

      /**
     * Aggregates the goal with another list of goals to form a conjunction of goals
     *
     * All goals in the conjunction must be satisfied in order for the conjunction to be satisfied
     *
     * @param others the list of goals
     */
    public and(...others: Goal[]): Goal

    /**
     * Aggregates the goal with another list of goals to form a disjunction of goals
     *
     * If any subgoal is satisfied, the goal will stop processing and appear satisfied.
     *
     * @param others the list of goals
     */
    public or(...others: Goal[]): Goal

    /**
     * Restricts the windows on which a goal is applied
     * @param window the windows on which this goal applies
     * @returns a new goal applying on a restricted horizon
     */
    public applyWhen(window: WindowsEDSL.Windows): Goal

    /**
     * Creates an ActivityRecurrenceGoal
     * @param opts an object containing the activity template and the interval at which the activities must be placed
     */
    public static ActivityRecurrenceGoal <A extends WindowsEDSL.Gen.ActivityType>(opts: { activityTemplate: ActivityTemplate<A>, interval: Temporal.Duration }): Goal

    /**
     * The CoexistenceGoal places one activity (defined by activityTemplate) per window (defined by forEach).
     * The activity is placed such that it starts at (startsAt) or ends at (endsAt) a certain offset from the window
     */
    public static CoexistenceGoal <T extends WindowsEDSL.Gen.ActivityType, S extends WindowsEDSL.Gen.ActivityType>(opts: ({
      activityTemplate: (( interval: WindowsEDSL.Interval ) => ActivityTemplate<S>) | ActivityTemplate<S>,
      forEach:  WindowsEDSL.Windows,
    } | {
      activityTemplate: (( span: ActivityInstance<T> ) => ActivityTemplate<S>) | ActivityTemplate<S>,
      forEach:  ActivityExpression<T>,
    }) & CoexistenceGoalTimingConstraints): Goal

    /**
     * Creates a CardinalityGoal
     * @param opts an object containing the activity template and a specification of what type of CardinalityGoal is required
     * @constructor
     */
    public static CardinalityGoal <A extends WindowsEDSL.Gen.ActivityType>(opts: { activityTemplate: ActivityTemplate<A>, specification: AST.CardinalityGoalArguments }): Goal
  }
  export class ActivityExpression<T> {
    /**
     * Creates an actvity expression of a type
     * @param activityType the type
     */
    public static ofType<T extends WindowsEDSL.Gen.ActivityType>(activityType: T): ActivityExpression<T>
  }
  class TimingConstraint {
    /**
     * The singleton timing constraint represents a precise time point
     * at some offset from either the start or end of a window.
     * @param windowProperty either WindowProperty.START or WindowProperty.END
     */
    public static singleton(windowProperty: WindowProperty): SingletonTimingConstraintNoOperator

    /**
     * The range timing constraint represents a range of acceptable times
     * relative to either the start or end of the window. The range will
     * be between the window "anchor" and the new point defined by the operator
     * and the offset.
     * @param windowProperty either WindowProperty.START or WindowProperty.END
     * @param operator either Operator.PLUS or Operator.MINUS
     * @param operand the duration offset
     */
    public static range(windowProperty: WindowProperty, operator: TimingConstraintOperator, operand: Temporal.Duration): RangeTimingConstraint
  }
  var WindowProperty: typeof AST.WindowProperty
  var Operator: typeof AST.TimingConstraintOperator
  var ActivityTypes: typeof WindowsEDSL.Gen.ActivityType

  type Double = number;
  type Integer = number;
}

/**
 * Represents a continuous closed-open interval (start value is included, end value is not included)
 */
export interface ClosedOpenInterval extends AST.ClosedOpenInterval {}

/**
 * An ActivityTemplate specifies the type of an activity, as well as the arguments it should be given.
 *
 *  Activity templates are generated for each mission model. You can get the full list of activity templates by typing `ActivityTemplates.` (note the period) into the scheduling goal editor, and viewing the auto-complete options.
 *
 * If the activity has parameters, pass them into the constructor in a dictionary as key-value pairs (i.e. `ActivityTemplate.ParamActivity({ param:1 }))`. If the activity has no parameters, do not pass a dictionary (i.e. `ActivityTemplate.ParameterlessActivity()`). */
export interface ActivityTemplate<A extends WindowsEDSL.Gen.ActivityType> extends AST.ActivityTemplate<A> {}

// Make Goal available on the global object
Object.assign(globalThis, { GlobalSchedulingCondition, Goal, ActivityExpression, TimingConstraint: TimingConstraint, WindowProperty: AST.WindowProperty, Operator: AST.TimingConstraintOperator, ActivityTypes: WindowsEDSL.Gen.ActivityType});
