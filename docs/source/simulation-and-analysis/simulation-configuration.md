# Simulation Configuration

The Aerie web application provides a graphical user interface to set and update simulation configuration arguments.

## Using Web-App GUI

Navigate to the plans page. As seen in **Figure 1**, the plan creation form includes a "Simulation Configuration" section that accepts a user-provided file. Specifically, the user-provided file is expected to be a serialized (JSON) set of mission model arguments.

![](images/sim-config-plans.png)
*Figure 1: Plan creation and selection view.*

Conceptually, these uploaded simulation configuration arguments are stored within a plan-specific simulation **template**.
Within the GUI each plan may be associated with just one simulation template;
individual simulations may set missing arguments or override existing arguments using the template's arguments as a base set of arguments.
**Figure 2** shows the simulation configuration view where arguments for a specific simulation may be set/overridden.

![](images/sim-config-args.png)
*Figure 2: Simulation configuration view.*

Formalized loosely, the sets of arguments at play here are:
- **T**, the template-defined arguments provided at plan creation time.
- **S**, the simulation-defined arguments provided prior to simulation invocation.
- **M**, the merged set of arguments derived from the union between **T** and **S**. Any duplicates will be resolved using arguments from **S**. This set is ultimately used in a simulation.

## Using GraphQL API

Using the GraphQL API it is possible to define simulations from as many simulation templates as desired.
Please refer to the [Aerie GraphQL API SIS](https://github.com/NASA-AMMOS/aerie/wiki/Aerie-GraphQL-API-Software-Interface-Specification)
to get up-and-running with a GraphQL client capable of communicating with the Aerie API.

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
