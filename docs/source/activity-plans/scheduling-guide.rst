================
Scheduling Guide
================

.. warning::
  Automated scheduling eDSL is under heavy development. This documentation will change regularly.

Introduction
============

This guide explains how to use the scheduling service with the latest version of Aerie.

The scheduling service allows you to add activities to a plan based on goals that you define.
These goals are defined in `TypeScript <https://www.typescriptlang.org/>`__, using a library provided by Aerie.

.. note::
  This guide assumes some very basic familiarity with programming (terminology like functions,
  arguments, return values, types, and objects, will not be explained), but it does not assume
  the reader has seen TypeScript or JavaScript before.

Prerequisites
-------------

* You will need to have uploaded a Mission Model
* You will need to have created a plan from that mission model


Authoring a new goal
====================

In the Aerie UI, open the scheduling pane, and click `New Goal`. This will open a text editor, with the following
default text:

.. code-block:: typescript

  export default (): Goal => {
    // Your code here
  }

This is a TypeScript function that takes no arguments and returns a Goal.

To unpack all of the parts:

- ``export default`` signals to Aerie that this is the function that defines the Goal.
- ``() => {}`` in TypeScript is called an `arrow function <https://www.tutorialsteacher.com/typescript/arrow-function>`_.
- The parenthesis ``()`` represent the parameters that the function takes. Scheduling goals cannot take any parameters, so these parenthesis must be empty.
- The curly braces ``{}`` represent the definition of the goal. The return statement for the function must go inside the braces.
- The ``: Goal`` part signifies that this function returns a Goal. TypeScript will check that the function does indeed return a Goal - if it does not, it will underline your code in red.

The code provided when you click ``New Goal`` is incomplete - the function does not yet return a Goal, so you should
see the word ``Goal`` underlined in red:

.. image:: https://user-images.githubusercontent.com/1189602/161592529-5abff638-bec7-4c19-a0c0-639e1bf35d98.png

Mousing over the word Goal, you should see something akin to the following message:

``A function whose declared type is neither 'void' nor 'any' must return a value.``

This message means that the function has promised to return a value, but it currently lacks a return statement.
Between the curly braces, add the following code: ``return Goal.ActivityRecurrenceGoal()``:

.. code-block:: typescript

  export default (): Goal => {
    return Goal.ActivityRecurrenceGoal() // <---- this is the new code
  }

Now, the editor should tell you that ``ActivityRecurrenceGoal()`` takes one argument.

.. image:: https://user-images.githubusercontent.com/1189602/161593612-b23560ea-c3b9-44c8-bac4-620e98d76356.png

The argument that we're missing is the "options" object. Objects in typescript are defined using curly braces ``{}``
with key-value pairs, like so:

.. code-block:: typescript

  {
    key: value
  }

If we pass an empty object ``{}`` to ``ActivityRecurrenceGoal``, we will get a new error message that will tell us what
keys our object will need:

.. code-block:: typescript

  export default (): Goal => {
    return Goal.ActivityRecurrenceGoal({}) // <---- the empty object is written as {}
  }

::

  Argument of type '{}' is not assignable to parameter of type '{ activityTemplate: ActivityTemplate;
  interval: number; }'.
  Type '{}' is missing the following properties from type '{ activityTemplate: ActivityTemplate; interval: number;
  }': activityTemplate, interval


This error message tells us that our object is missing two keys: ``activityTemplate``, and ``interval``. If we look up
the definition of ``ActivityRecurrenceGoal`` in the scheduling documentation, we will see that it does indeed need an
activity template and an interval. Let's add those:

.. code-block:: typescript

  export default (): Goal => {
      return Goal.ActivityRecurrenceGoal({
          activityTemplate: null,
          interval: Temporal.Duration.from({ hours: 24 })
      })
  }

Now, we just need to finish specifying the **activityTemplate**. Start by typing ``ActivityTemplates.`` (note the period)
, and select an activity type. Provide your activity an object with the arguments that that activity takes. Once
the editor is no longer underlining your code, save your goal, and hit ``Schedule & Analyze`` and observe that your
new activities were added to the plan.

Specifying the order of goals
=============================

The Aerie scheduler accepts a list of goals, and tries to satisfy them one by one by adding activities to your plan.
We refer to this list of goals as a **scheduling specification**. Currently, Aerie creates one scheduling
specification per plan. A goal's **priority** is simply a number reflecting that goal's position in the scheduling
specification. The first goal will always have priority ``0``, and the n-th goal will always have priority ``n - 1``.
Within a scheduling specification, a scheduling goal can be toggled enabled or disabled. A disabled scheduling goal
will be excluded from scheduling execution. The priority ordering of the scheduling goals remains unchanged whether
a goal is enabled or disabled.

Scheduling DSL Documentation
============================
Here you will find the full set of features in the scheduling DSL.


.. warning::
  Activities with :ref:`uncontrollable durations <duration-types>` have
  been found to behave somewhat unpredictably, in terms of when they are placed. This has to do with how temporal
  constraints interact with the unpredictability of the durations.  Finding when an activity will start while subject
  to temporal constraint involves search.

ActivityTemplate
================

An ``ActivityTemplate`` specifies the type of an activity, as well as the arguments it should be given. Activity
templates are generated for each mission model. You can get the full list of activity templates by typing
``ActivityTemplates.`` (note the period) into the scheduling goal editor, and viewing the auto-complete options.

If the activity has parameters, pass them into the constructor in a dictionary as key-value pairs
(i.e. ``ActivityTemplate.ParamActivity({ param:1 }))``. If the activity has no parameters, do not pass a
dictionary (i.e. ``ActivityTemplate.ParameterlessActivity())``.

Goal Types
==========

Activity Recurrence Goal
------------------------
The Activity Recurrence Goal (sometimes referred to as a "frequency goal") specifies that a certain activity
should occur repeatedly throughout the plan, at some given interval.

**Inputs**

- **activityTemplate**: the description of the activity whose recurrence we're interested in.
- **interval**: a ``Duration`` of time specifying how often this activity must occur

**Behavior**

This interval is treated as an upper bound - so if the activity occurs more frequently, that is not considered
a failure.

The scheduler will find places in the plan where the given activity has not occurred within the given interval,
and it will place an instance of that activity there.

.. note::
  The interval is measured between the *start times* of two activity instances. Neither the duration, nor
  the end time of the activity are examined by this goal.

**Example**

.. code-block:: typescript

  export default function myGoal() {
    return Goal.ActivityRecurrenceGoal({
      activityTemplate: ActivityTemplates.GrowBanana({
        quantity: 1,
        growingDuration: Temporal.Duration.from({ hours: 1 })
      }),
      interval: Temporal.Duration.from({ hours: 2 })
    })
  }

The goal above will place a ``GrowBanana`` activity in every 2-hour period of time that does not already contain one
with the exact same parameters.

Coexistence Goal
----------------
The Coexistence Goal specifies that a certain activity should occur once **for each** occurrence of some condition.

**Inputs**

* **forEach**: a set of time windows (``Windows``, see [documentation](../constraints) on how to produce such an expression) or a set of activities (``ActivityExpression``)
* **activityTemplate**: the description of the activity to insert after each activity identified by ``forEach``
* **startsAt**: optionally specify a specific time when the activity should start relative to the window
* **startsWithin**: optionally specify a range when the activity should start relative to the window
* **endsAt**: optionally specify a specific time when the activity should end relative to the window
* **endsWithin**: optionally specify a range when the activity should end relative to the window

.. note::
  Either the start or end of the activity must be constrained. This means that at least **1** of the 4
  properties ``startsAt``, ``startsWithin``, ``endsAt``, ``endsWithin`` must be given.

**Behavior**

The scheduler will find places in the plan where the ``forEach`` condition is true, and if not, it will insert a new
instance using the given ``activityTemplate`` and temporal constraints.

**Examples**

.. code-block:: typescript

  export default () => Goal.CoexistenceGoal({
    forEach: ActivityExpression.ofType(ActivityTypes.GrowBanana),
    activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
    startsAt: TimingConstraint.singleton(WindowProperty.END).plus(Temporal.Duration.from({ minutes: 5 }))
  })

Behavior: for each activity A of type ``GrowBanana`` present in the plan when the goal is evaluated, place an activity
of type ``PeelBanana`` starting exactly at the end of A + 5 minutes.

.. code-block:: typescript

  export default () => Goal.CoexistenceGoal({
    forEach: ActivityExpression.ofType(ActivityTypes.GrowBanana),
    activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
    startsWithin: TimingConstraint.range(WindowProperty.END, Operator.PLUS, Temporal.Duration.from({ minutes: 5 })),
    endsWithin: TimingConstraint.range(WindowProperty.END, Operator.PLUS, Temporal.Duration.from({ minutes: 6 }))
  })

Behavior: for each activity A of type ``GrowBanana`` present in the plan when the goal is evaluated, place an activity
of type ``PeelBanana`` starting in the interval [end of A, end of A + 5 minutes] and ending in the interval [end of A,
end of A + 6 minutes].

.. code-block:: typescript

  export default () => Goal.CoexistenceGoal({
    forEach: Real.Resource("/fruit").equal(4.0),
    activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
    endsAt: TimingConstraint.singleton(WindowProperty.END).plus(Temporal.Duration.from({ minutes: 5 }))
  })

Behavior: for each continuous period of time during which the ``/fruit`` resource is equal to 4, place an activity of
type ``PeelBanana`` ending exactly at the end of A + 6 minutes. Note that the scheduler will allow a default timing
error of 500 milliseconds for temporal constraints. This parameter will be configurable in an upcoming release.

.. warning::
  If the end is unconstrained while the activity has an uncontrollable duration, the scheduler may fail
  to place the activity. To work around this, add an ``endsWithin`` constraint that encompasses your expectation for
  the duration of the activity - this will help the scheduler narrow the search space.

Cardinality Goal
----------------
The Cardinality Goal specifies that a certain activity should occur in the plan either a certain number of times,
or for a certain total duration.

**Inputs**

- **activityTemplate**: the description of the activity whose recurrence we're interested in.
- **specification**: an object with either an ``occurrence`` field, a ``duration`` field, or both (see examples below).

**Behavior**

The duration and occurrence are treated as lower bounds - so if the activity occurs more times, or for a longer
duration, that is not considered a failure, and the scheduler will not add any more activities.

The scheduler will identify whether it not the plan has enough occurrences or total duration of the given activity
template. If not, it will add activities until satisfaction.

**Examples**

Setting a lower bound on the total duration:

.. code-block:: typescript

  export default function myGoal() {
      return Goal.CardinalityGoal({
          activityTemplate: ActivityTemplates.GrowBanana({
              quantity: 1,
              growingDuration: Temporal.Duration.from({ seconds: 1 }),
          }),
          specification: { duration: Temporal.Duration.from({ seconds: 10 }) }
      })
  }

Setting a lower bound on the number of occurrences:

.. code-block:: typescript

  export default function myGoal() {
      return Goal.CardinalityGoal({
          activityTemplate: ActivityTemplates.GrowBanana({
              quantity: 1,
              growingDuration: Temporal.Duration.from({ seconds: 1 }),
          }),
          specification: { occurrence: 10 }
      })
  }

Combining the two:

.. code-block:: typescript

  export default function myGoal() {
      return Goal.CardinalityGoal({
          activityTemplate: ActivityTemplates.GrowBanana({
              quantity: 1,
              growingDuration: Temporal.Duration.from({ seconds: 1 }),
          }),
          specification: { occurrence: 10, duration: Temporal.Duration.from({ seconds: 10 }) }
      })
  }

.. note::
  In order to avoid placing multiple activities at the same start time, the Cardinality goal introduces an
  assumed mutual exclusion constraint - namely that new activities will not be allowed to overlap with existing
  activities.

OR goal - Disjunction of goals
------------------------------

The OR Goal aggregates several goals together and specifies that at least one of them must be satisfied.

**Inputs**

- **goals**: a list of goals (here below referenced as the subgoals)

**Behavior**

The scheduler will try to satisfy each subgoal in the list until one is satisfied. If a subgoal is only partially
satisfied, the scheduler will not backtrack and will let the inserted activities in the plan.

**Examples**

.. code-block:: typescript

  export default function myGoal() {
      return Goal.CardinalityGoal({
               activityTemplate: ActivityTemplates.GrowBanana({
                 quantity: 1,
                 growingDuration: Temporal.Duration.from({ hours: 1 }),
             }),
            specification: { occurrence : 10 }
            }).or(
             Goal.ActivityRecurrenceGoal({
              activityTemplate: ActivityTemplates.GrowBanana({
              quantity: 1,
              growingDuration: Temporal.Duration.from({ hours: 1 }),
            }),
            interval: Temporal.Duration.from({ hours: 2 })
          }))
  }

If the plan has a 24-hour planning horizon, the OR goal above will try placing activities of the ``GrowBanana`` type.
The first subgoal will try placing 10 1-hour occurrences. If it fails to do so, because the planning horizon is maybe
too short, it will then try to schedule 1 activity every 2 hours for the duration of the planning horizon.

It may fail to achieve both subgoals but as the scheduler does not backtrack for now, activities inserted by any of
the subgoals are kept in the plan.

AND goal - Conjunction of goals
-------------------------------

The AND Goal aggregates several goals together and specifies that at least one of them must be satisfied.

**Inputs**

- **goals**: an ordered list of goals (here below referenced as the subgoals)

**Behavior**

The scheduler will try to satisfy each subgoal in the list. If a subgoal is only partially satisfied, the scheduler
will not backtrack and will let the inserted activities in the plan. If all the subgoals are satisfied, the AND goal
will appear satisfied. If one or several subgoals have not been satisfied, the AND goal will appear unsatisfied.

**Examples**

.. code-block:: typescript

  export default function myGoal() {
      return Goal.CoexistenceGoal({
        forEach: Real.Resource("/fruit").equal(4.0),
        activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
        endsAt: TimingConstraint.singleton(WindowProperty.END).plus(Temporal.Duration.from({ minutes: 5 }))
      }).and(
        Goal.CardinalityGoal({
              activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
              specification: { occurrence : 10 }
            }))
  }

The AND goal above has two subgoals. The coexistence goal will place activities of type ``PeelBanana`` everytime the
``/fruit`` resource is equal to 4. The second goal will place 10 occurrences of the same kind of activities ``PeelBanana``.
The first subgoal will be evaluated first and will place a certain number of ``PeelBanana`` activities in the plan. When
the second goal will be evaluated, it will count already present ``PeelBanana`` activities and insert the missing number.
Imagine the first goals leads to inserting 2 activities. The second goal will then have to place 8 activities to be
satisfied.

Restricting when a goal is applied
==================================

By default, a goal applies on the whole planning horizon. The Aerie scheduler provides support for restricting *when*
a goal applies with the ``.applyWhen()`` method in the ``Goal`` class. This node allows users to provide a set of windows
(``Windows``, see :ref:`documentation <windows>`) which could be a time
or a resource-based window.

The ``.applyWhen()`` method, takes one argument: the windows (in the form of an expression) that the goal should apply
over. What follows is an example that applies a daily recurrence goal only when a given resource is greater than 2.
If the resource is less than two, then the goal is no longer applied.

.. code-block:: typescript

  export default function myGoal() {
      return Goal.ActivityRecurrenceGoal({
              activityTemplate: ActivityTemplates.GrowBanana({
              quantity: 1,
              growingDuration: Temporal.Duration.from({ hours: 1 }), //1 hour in microseconds
            }),
            interval: Temporal.Duration.from({ hours: 2 }) // 2 hours in microseconds
          }).applyWhen(Real.Resource("/fruit").greaterThan(2))
  }

.. note::
  If you are trying to schedule an activity, or a recurrence within a window but that window cuts off either the
  activity or the recurrence interval (depending on the goal type), it will not be scheduled. For example, if you
  had a recurrence interval of 3 seconds, scheduling a 2 second activity each recurrence, and had the following window,
  you'd get the following:

.. code-block::

  RECURRENCE INTERVAL: [++-++-++-]
  GOAL WINDOW:         [+++++----]
  RESULT:              [++-------]

That, is, the second activity won't be scheduled as the goal window cuts off its recurrence interval. Scheduling is
*local*, not global. This means for every window that is matched (as it is possible to have disjoint
windows, imagine a resource that fluctuates upward and downward but only applying that goal when the resource is over
a certain value), the goal is applied individually. So, for that same recurrence interval setup as before, we could
have:

.. code-block::

  RECURRENCE INTERVAL: [++-++-++-++-]
  GOAL WINDOW:         [+++++--+++--]
  RESULT:              [++-----++---] //(the second one is applied independently of the first!)

When mapping out a temporal window to apply a goal over, keep in mind that the ending boundary of the goal is
*exclusive*, i.e. if I want to apply a goal in the window of 10-12 seconds, it will apply only on seconds 10 and 11.
This is in line with the `fencepost problem <https://icarus.cs.weber.edu/~dab/cs1410/textbook/3.Control/fencepost.html>`__.

Global Scheduling Conditions
============================
It is possible to restrict the scheduler from placing activities when certain conditions are not met.

A Global Scheduling Condition is defined as a string of typescript (just like a Scheduling Goal), but
the return type is expected to be of type ``Windows``.

.. code-block:: typescript

  export default function myFirstSchedulingCondition(): Windows {
    return Real.Resource("/plant").greaterThan(10.0)
  }

The ``Windows`` type is described in the :ref:`Constraints DSL documentation <windows>`.

Interactions with Global Scheduling Conditions are only possible via the API.

To create a new Global Scheduling Conditions

.. code-block::

  mutation InsertGlobalSchedulingCondition {
    insert_scheduling_condition_one(object:{
      name: "My first scheduling condition"
      model_id: 1
      definition: "export default (): Windows => Real.Resource('/fruit').greaterThan(5.0)"
    }) {
      id
    }
  }

This mutation returns an ``id``, which can be used to associate it with a scheduling specification.
(You'll need to look up the ``id`` of the scheduling specification you're interested in).

.. code-block::

  mutation AssociateConditionToSpecification {
      insert_scheduling_specification_conditions_one(object:{
      condition_id: 2
      specification_id: 1
      enabled: true
    }) {
      __typename
    }
  }

From now on, running the scheduler using that specification will also run that scheduling condition. Just like goals,
scheduling conditions can be updated, deleted, and disabled via the API.

Example: updating the definition

.. code-block::

  mutation UpdateGlobalSchedulingConditionDefinition {
    update_scheduling_condition_by_pk(
      pk_columns: {id:2},
      _set: {
        definition: "export default (): Windows => Real.Resource('/fruit').greaterThanOrEqual(1.0)"
      }
    ) {
      __typename
    }
  }

Example: disabling a condition in a specification

.. code-block::

  mutation DisableSchedulingCondition{
    update_scheduling_specification_conditions_by_pk(
      pk_columns: {
        condition_id:2,
        specification_id:1
      },
      _set:{
        enabled:false
      }
    )
  }

Example: removing a condition from a specification

.. code-block::

  mutation RemoveSchedulingCondition{
    delete_scheduling_specification_conditions_by_pk(
      condition_id:2,
      specification_id:1
    ){
      __typename
    }
  }
