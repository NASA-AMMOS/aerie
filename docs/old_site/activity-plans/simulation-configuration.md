# Simulation Configuration

You can create, update and use a simulation configuration via the UI and/or API.

## Using the UI

TODO

## Using the GraphQL API

Using the GraphQL API it is possible to define simulations from as many simulation templates as desired. Please refer to the [Aerie GraphQL API documentation](../graphql-api/index) to get up-and-running with a GraphQL client capable of communicating with the Aerie API.

### Create a Plan

Create a plan `test-plan`. This will trigger Merlin to create a new simulation that can be queried for from the GraphQL API:

```
query  {
  simulation {
    id
  }
}
```

Which indicates that the one and only simulation `id` is:

```json
{
  "data": {
    "simulation": [
      {
        "id": 1
      }
    ]
  }
}
```

This ID will be used to identify the newly created simulation.

### Create a Simulation Template

A simulation template must be associated with a mission model. In this example the only existing mission model ID is `1`.

```
mutation  {
  insert_simulation_template(objects: {
    model_id: 1,
    description: "first template",
    arguments: { initialPlantCount: 42, initialProducer: "template"}
  }) {
    returning {
      id
    }
  }
}
```

Resulting in:

```json
{
  "data": {
    "insert_simulation_template": {
      "returning": [
        {
          "id": 1
        }
      ]
    }
  }
}
```

This ID will be used to identify the newly created simulation template.

Note that the arguments supplied here are not the full set of arguments required for simulation:

```
query  {
  mission_model_parameters_by_pk(model_id: 1) {
    parameters
  }
}
```

Results in:

```json
{
  "data": {
    "mission_model_parameters_by_pk": {
      "parameters": {
        "initialDataPath": {
          "order": 1,
          "schema": {
            "type": "path"
          }
        },
        "initialProducer": {
          "order": 2,
          "schema": {
            "type": "string"
          }
        },
        "initialPlantCount": {
          "order": 0,
          "schema": {
            "type": "int"
          }
        }
      }
    }
  }
}
```

As can be seen above, the template does not define a `initialDataPath` argument.
In this case this must be provided by the simulation's argument set prior to simulation.

### Associate Simulation with Simulation Template

Attach the simulation template to the current simulation and update the simulation's arguments.

```
mutation {
  update_simulation(
    _set: {
      simulation_template_id: 1,
      arguments: {
        initialPlantCount: 200,
        initialDataPath: "/etc/os-release"
      }
    },
    where: {id: {_eq: 1}}) {
    returning {
      id
    }
  }
}
```

The simulation with ID `1` is now associated with the template with ID `1`.
In addition to being associated with a template, the simulation has defined a `initialPlantCount` argument override and a `initialDataPath` assignment.

### Run the Simulation

```
query {
  simulate(planId: 1) {
    results
  }
}
```

The full results on this query are omitted for brevity but a sampling should look like:

```json
"/plant": [
    {
        "x": 0,
        "y": 200
    },
    {
        "x": 31536000000000,
        "y": 200
    }
],
"/producer": [
    {
        "x": 0,
        "y": "template"
    },
    {
        "x": 31536000000000,
        "y": "template"
    }
]
```

Since an `initialProducer` argument was not provided in the simulation argument set the template's `initialProducer` value comes through here.
