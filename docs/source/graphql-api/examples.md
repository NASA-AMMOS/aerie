# Examples

The following queries are examples of what Aerie refers to as "canonical queries" because they map to commonly understood use cases and data structures within mission subsystems. When writing a GraphQL query, refer to the schema for all valid fields that one can specify in a particular query.

## Query for All Plans

```
query {
  plan {
    duration
    id
    model_id
    name
    start_time
  }
}
```

## Query a Single Plan

```
query {
  plan_by_pk(id: 1) {
    duration
    id
    model_id
    name
    start_time
  }
}
```

## Query for All Activity Instances (directives) for a Plan

You can either query `plan_by_pk` for all activity instances for a single plan, or query `plan` for all activity instances from _all_ plans.

The following query returns all activity instances for a single plan:

```
query {
  plan_by_pk(id: 1) {
    activity_directives {
      arguments
      id
      type
    }
  }
}
```

## Query the Mission Model of a Plan

```
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
```

## Query for All Activity Types within a Mission Model of a Plan

Returns a list of activity types. For each activity type, the name, parameter schema, which parameters are required (must be defined).

```
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
```

## Run Simulation

The following query starts a simulation, or returns information if the simulation has already started. The `simulationDatasetId` can be used to query for simulation results.

```
query {
  simulate(planId: 1) {
    reason
    simulationDatasetId
    status
  }
}
```

## Query for All Simulated Activities in a Simulated Plan

```
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
```

## Query for All Resource Profiles in Simulated Plan

Profiles are simulated resources. The following query gets profiles for a given plan's latest simulation dataset (i.e. the latest resource simulation results):

```
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
```

## Query for All Simulated Activities and Resource Profiles in Simulated Plan

The following query just combines the previous two queries to get all activities and profiles in a simulated plan:

```
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
```

## Query for All Resource Samples in Simulated Plan

```
query {
  resourceSamples(planId: 1) {
    resourceSamples
  }
}
```

## Query for All Constraint Violations in Simulated Plan

```
query {
  constraintViolations(planId: 1) {
    constraintViolations
  }
}
```

## Query for All Resource Types in a Mission Model

```
query {
  resourceTypes(missionModelId: 1) {
    name
    schema
  }
}
```

## Create Plan

```
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
```

## Create Simulation

Each plan must have a least one associated simulation to execute a simulation. To create a simulation for a plan you can use the following mutation:

```
mutation {
  insert_simulation_one(
    object: { arguments: {}, plan_id: 1, simulation_template_id: null }
  ) {
    id
  }
}
```

## Create Activity Instances (Directives)

```
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
```

## Query for Activity Effective Arguments

This query returns a set of effective arguments given a set of required (and overridden) arguments.

```
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
```

Resulting in:

```json
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
```

When a required argument is not provided, the returned JSON will indicate which argument is missing. With `examples/banananation`'s `BakeBananaBread`, where only the `temperature` parameter has a default value:

```
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
```

Results in:

```json
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
```

## Query for Mission Model Configuration Effective Arguments

The `getModelEffectiveArguments` returns the same structure as `getActivityEffectiveArguments`; a set of effective arguments given a set of required (and overridden) arguments. For example, `examples/config-without-defaults`'s has all required arguments:

```
query {
  getModelEffectiveArguments(missionModelId: 1, modelArguments: {}) {
    arguments
    errors
    success
  }
}
```

Results in:

```json
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
```
