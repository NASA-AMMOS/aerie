============
API Examples
============

The following queries are examples of what Aerie refers to as "canonical queries" because they map to commonly understood use cases and data structures within mission subsystems.
When writing a GraphQL query, refer to the schema for all valid fields that one can specify in a particular query.

Query for All Plans
-------------------

.. begin query all plans
.. code-block::

  query {
    plan {
      duration
      id
      model_id
      name
      start_time
    }
  }
.. end query all plans

Query a Single Plan
-------------------

.. begin query single plan
.. code-block::

  query {
    plan_by_pk(id: 1) {
      duration
      id
      model_id
      name
      start_time
    }
  }
.. end query single plan

Query for All Activity Instances (directives) for a Plan
--------------------------------------------------------

You can either query ``plan_by_pk`` for all activity instances for a single plan, or query ``plan`` for all activity instances from *all* plans.

The following query returns all activity instances for a single plan:

.. begin query all activity instances of plan
.. code-block::

  query {
    plan_by_pk(id: 1) {
      activity_directives {
        arguments
        id
        type
      }
    }
  }
.. end query all activity instances of plan

Query the Mission Model of a Plan
---------------------------------

.. begin query mission model of plan
.. code-block::

  query {
    plan_by_pk(id: 1) {
      mission_model {
        id
        mission
        name
        owner
        version
      }
    }
  }
.. end query mission model of plan

Query for All Activity Types within a Mission Model of a Plan
-------------------------------------------------------------

Returns a list of activity types. For each activity type, the name, parameter schema, which parameters are required (must be defined).

.. begin query all activity types within mission model of plan
.. code-block::

  query {
    plan_by_pk(id: 1) {
      mission_model {
        activity_types {
          computed_attributes_value_schema
          name
          parameters
          required_parameters
        }
      }
    }
  }
.. end query all activity types within mission model of plan

Run Simulation
--------------

The following query starts a simulation, or returns information if the simulation has already started. The ``simulationDatasetId`` can be used to query for simulation results.

.. begin run simulation
.. code-block::

  query {
    simulate(planId: 1) {
      reason
      simulationDatasetId
      status
    }
  }
.. end run simulation

Query for All Simulated Activities in a Simulated Plan
------------------------------------------------------

.. begin query all simulated activities in simulated plan
.. code-block::

  query {
    plan_by_pk(id: 1) {
      simulations(limit: 1) {
        simulation_datasets(order_by: { id: desc }, limit: 1) {
          simulated_activities {
            activity_type_name
            attributes
            duration
            id
            parent_id
            simulation_dataset_id
            start_offset
          }
        }
      }
      start_time
    }
  }
.. end query all simulated activities in simulated plan

Query for All Resource Profiles in Simulated Plan
-------------------------------------------------

Profiles are simulated resources. The following query gets profiles for a given plan's latest simulation dataset (i.e. the latest resource simulation results):

.. begin query all resource profiles in simulated plan
.. code-block::

  query {
    plan_by_pk(id: 1) {
      duration
      simulations(limit: 1) {
        simulation_datasets(order_by: { id: desc }, limit: 1) {
          dataset {
            profiles {
              name
              profile_segments {
                dynamics
                start_offset
              }
              type
            }
          }
        }
      }
      start_time
    }
  }
.. end query all resource profiles in simulated plan

Query for All Simulated Activities and Resource Profiles in Simulated Plan
--------------------------------------------------------------------------

The following query just combines the previous two queries to get all activities and profiles in a simulated plan:

.. begin query all simulated activities and resource profiles in simulated plan
.. code-block::

  query {
    plan_by_pk(id: 1) {
      duration
      simulations(limit: 1) {
        simulation_datasets(order_by: { id: desc }, limit: 1) {
          simulated_activities {
            activity_type_name
            attributes
            duration
            id
            parent_id
            simulation_dataset_id
            start_offset
          }
          dataset {
            profiles {
              name
              profile_segments {
                dynamics
                start_offset
              }
              type
            }
          }
        }
      }
      start_time
    }
  }
.. end query all simulated activities and resource profiles in simulated plan

Query for All Resource Samples in Simulated Plan
------------------------------------------------

.. begin query all resource samples in simulated plan
.. code-block::

  query {
    resourceSamples(planId: 1) {
      resourceSamples
    }
  }
.. end query all resource samples in simulated plan

Query for All Constraint Violations in Simulated Plan
-----------------------------------------------------

.. begin query all constraint violations in simulated plan
.. code-block::

  query {
    constraintViolations(planId: 1) {
      constraintViolations
    }
  }
.. end query all constraint violations in simulated plan

Query for All Resource Types in a Mission Model
-----------------------------------------------

.. begin query all resource types in mission model
.. code-block::

  query {
    resourceTypes(missionModelId: 1) {
      name
      schema
    }
  }
.. end query all resource types in mission model

Create Plan
-----------

.. begin create plan
.. code-block::

  mutation {
    insert_plan_one(
      object: {
        duration: "432000 seconds 0 milliseconds"
        model_id: 1
        name: "My First Plan"
        start_time: "2020-001T00:00:00"
      }
    ) {
      id
      revision
    }
  }
.. end create plan

Create Simulation
-----------------

Each plan must have at least one associated simulation to execute a simulation. To create a simulation for a plan you can use the following mutation:

.. begin create simulation
.. code-block::

  mutation {
    insert_simulation_one(
      object: { arguments: {}, plan_id: 1, simulation_template_id: null }
    ) {
      id
    }
  }
.. end create simulation

Create Activity Instances (Directives)
--------------------------------------

.. begin create activity instances
.. code-block::

  mutation {
    insert_activity_directive(
      objects: [
        {
          arguments: { peelDirection: "fromTip" }
          plan_id: 1
          start_offset: "1749:01:35.575"
          type: "PeelBanana"
        }
        {
          arguments: { peelDirection: "fromTip" }
          plan_id: 1
          start_offset: "1750:01:35.575"
          type: "PeelBanana"
        }
      ]
    ) {
      returning {
        id
        start_offset
      }
    }
  }
.. end create activity instances

Query for Activity Effective Arguments
--------------------------------------

This query returns a set of effective arguments given a set of required (and overridden) arguments.

.. begin query activity effective arguments
.. code-block::

  query {
    getActivityEffectiveArguments(
      missionModelId: 1
      activityTypeName: "BakeBananaBread"
      activityArguments: { tbSugar: 1, glutenFree: false }
    ) {
      arguments
      errors
      success
    }
  }
.. end query activity effective arguments

Resulting in:

.. begin results activity effective arguments
.. code-block:: json

  {
    "data": {
      "getActivityEffectiveArguments": {
        "arguments": {
          "temperature": 350,
          "tbSugar": 1,
          "glutenFree": false
        },
        "success": true
      }
    }
  }
.. end results activity effective arguments

When a required argument is not provided, the returned JSON will indicate which argument is missing.
With ``examples/banananation``'s ``BakeBananaBread``, where only the ``temperature`` parameter has a default value:

.. begin query activity effective arguments missing arguments
.. code-block::

  query {
    getActivityEffectiveArguments(
      missionModelId: 1
      activityTypeName: "BakeBananaBread"
      activityArguments: {}
    ) {
      arguments
      errors
      success
    }
  }
.. end query activity effective arguments missing arguments

Results in:

.. begin results activity effective arguments missing arguments
.. code-block:: json

  {
    "data": {
      "getActivityEffectiveArguments": {
        "arguments": {
          "temperature": 350
        },
        "errors": {
          "tbSugar": {
            "schema": {
              "type": "int"
            },
            "message": "Required argument for activity \"BakeBananaBread\" not provided: \"tbSugar\" of type ValueSchema.INT"
          },
          "glutenFree": {
            "schema": {
              "type": "boolean"
            },
            "message": "Required argument for activity \"BakeBananaBread\" not provided: \"glutenFree\" of type ValueSchema.BOOLEAN"
          }
        },
        "success": false
      }
    }
  }
.. end results activity effective arguments missing arguments

Query for Mission Model Configuration Effective Arguments
---------------------------------------------------------

The ``getModelEffectiveArguments`` returns the same structure as ``getActivityEffectiveArguments``;
a set of effective arguments given a set of required (and overridden) arguments.
For example, ``examples/config-without-defaults``'s has all required arguments:

.. begin query mission model configuration effective arguments
.. code-block::

  query {
    getModelEffectiveArguments(missionModelId: 1, modelArguments: {}) {
      arguments
      errors
      success
    }
  }
.. end query mission model configuration effective arguments

Results in:

.. begin results mission model configuration effective arguments
.. code-block:: json

  {
    "data": {
      "getModelEffectiveArguments": {
        "arguments": {},
        "errors": {
          "a": {
            "schema": {
              "type": "int"
            },
            "message": "Required argument for configuration \"Configuration\" not provided: \"a\" of type ValueSchema.INT"
          },
          "b": {
            "schema": {
              "type": "real"
            },
            "message": "Required argument for configuration \"Configuration\" not provided: \"b\" of type ValueSchema.REAL"
          },
          "c": {
            "schema": {
              "type": "string"
            },
            "message": "Required argument for configuration \"Configuration\" not provided: \"c\" of type ValueSchema.STRING"
          }
        },
        "success": false
      }
    }
  }
.. end results mission model configuration effective arguments

Create Constraint
-----------------

To create a single constraint, use the following mutation:

.. begin create constraint
.. code-block::

  mutation CreateConstraint($constraint: constraint_insert_input!) {
    createConstraint: insert_constraint_one(object: $constraint) {
      id
    }
  }
.. end create constraint

with arguments of the following format:

.. begin create constraint arguments
.. code-block::

  {
    "constraint": {
      "model_id": number, // required if plan_id is absent
      "plan_id": number, // required if model_id is absent
      "name": string,
      "summary": string, // optional
      "description": string, // optional
      "definition": string
    }
  }
.. end create constraint arguments

Update a Constraint
-------------------

.. begin update constraint
.. code-block::

  mutation UpdateConstraint($id: Int!, $constraint: constraint_set_input!) {
    updateConstraint: update_constraint_by_pk(
      pk_columns: { id: $id }, _set: $constraint
    ) {
      id
    }
  }
.. end update constraint

with arguments:

.. begin update constraint arguments
.. code-block::

  {
    "id": number,
    "constraint": {...} // same input as when creating
  }
.. end update constraint arguments

Delete a Constraint
-------------------

.. begin delete constraint
.. code-block::

  mutation DeleteConstraint($id: Int!) {
    deleteConstraint: delete_constraint_by_pk(id: $id) {
      id
    }
  }
.. end delete constraint

with arguments:

.. begin delete constraint arguments
.. code-block::

  {
    "id": number
  }
.. end delete constraint arguments

Add External Dataset
--------------------------

.. begin add external dataset
.. code-block::

  mutation AddExternalDataset($planId: Int!, $datasetStart: String!, $profileSet: ProfileSet!) {
    addExternalDataset(planId: $planId, datasetStart: $datasetStart, profileSet: $profileSet) {
      datasetId
    }
  }
.. end add external dataset


Delete External Dataset
--------------------------

.. begin delete external dataset
.. code-block::

  mutation DeleteExternalDataset($id: Int!) {
    delete_dataset_by_pk(id: $id) {
      id
    }
  }
.. end delete external dataset
