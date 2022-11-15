======================================
Authoring Scheduling Global Conditions
======================================

.. note::

  For more information on Scheduling Syntax, see the :doc:`Scheduling eDSL Documentation <../../../scheduling-edsl-api/index>`


It is possible to restrict the scheduler from placing activities when certain conditions are not met.

A Global Scheduling Condition is defined as a string of typescript (just like a Scheduling Goal), but
the return type is expected to be of type ``GlobalSchedulingCondition``.

These conditions, when activated, apply for all goals, that is why they are qualified as global.

There are several types of global scheduling conditions available described in the sections below.

Restricting when any activity type can be scheduled
===================================================

This condition takes an expression of type ``Windows`` and prevents the scheduler from inserting any activity outside the
time intervals produced by the expression when evaluated.

The ``Windows`` type is described in the :ref:`Constraints DSL documentation <windows>`.

Example:

.. code-block:: typescript

  export default function myFirstSchedulingCondition(): GlobalSchedulingCondition {
    return GlobalSchedulingCondition.scheduleActivitiesOnlyWhen(
      Real.Resource("/plant").lowerThan(10.0)
      )
  }

In this example, activities of any type can only be placed whenever ``/plant`` is lower than 10.0.

Restricting when some activity types can be scheduled
=====================================================

This condition takes a list of activity types and an expression of type ``Windows``. It prevents the scheduler from
inserting activity of the given activity types outside the time intervals produced by the expression when evaluated.

The ``Windows`` type is described in the :ref:`Constraints DSL documentation <windows>`.

Example:

.. code-block:: typescript

  export default function mySecondSchedulingCondition(): GlobalSchedulingCondition {
    return GlobalSchedulingCondition.scheduleOnlyWhen(
      [ActivityType.BananaNap,ActivityType.BiteBanana],
      Real.Resource("/plant").greaterThan(10.0)
      )
  }

In this example, activities of type ``BananaNap`` and ``BiteBanana`` can be placed only when ``/plant`` is greater than 10.0.

Prevent activities from overlapping - mutual exclusion
======================================================

This condition takes two lists of activity types as arguments. It prevents the scheduler from inserting activities
of the types from the first list to overlap with activities of type from the second list (and vice versa).

.. code-block:: typescript

  export default function myThirdSchedulingCondition(): GlobalSchedulingCondition {
    return GlobalSchedulingCondition.mutex(
      [ActivityType.BananaNap,ActivityType.BiteBanana],
      [ActivityType.ChangeProducer, ActivityType.GrowBanana]
      )
  }

In this example, activities of types ``BananaNap`` and ``BiteBanana`` will not be allowed to overlap with activities of types ``ChangeProduced``
and ``GrowBanana``. But two activities of type ``GrowBanana`` can still overlap. And a ``BananaNap`` can still overlap with a ``BiteBanana``.

By default, two activities from the same type can overlap. To restrict activities of a given type from overlapping with each other,
it is necessary to explicitly write a mutex condition such as the following one.

.. code-block:: typescript

  export default function myFourthSchedulingCondition(): GlobalSchedulingCondition {
    return GlobalSchedulingCondition.mutex(
      [ActivityType.BiteBanana],
      [ActivityType.BiteBanana]
      )
  }

This condition will prevent ``BiteBanana`` from overlapping with each other.

Activating a global scheduling condition
========================================


.. tabs::

  .. group-tab:: User Interface

    Interactions with Global Scheduling Conditions are only possible via the API.

  .. group-tab:: API

    To create a new global scheduling condition

      .. code-block::

        mutation InsertGlobalSchedulingCondition {
          insert_scheduling_condition_one(object:{
            name: "My first scheduling condition"
            model_id: 1
            definition: "export default (): GlobalSchedulingCondition => GlobalSchedulingCondition.mutex([ActivityType.BiteBanana],[ActivityType.BiteBanana])"
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
              definition: "export default (): GlobalSchedulingCondition =>  GlobalSchedulingCondition.scheduleActivitiesOnlyWhen(Real.Resource("/plant").lowerThan(10.0))"
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
