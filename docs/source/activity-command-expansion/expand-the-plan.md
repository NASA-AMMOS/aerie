# Expand The Plan

Now for the exciting part! We can finally expand the plan into commands. To do that, we'll need 
to set up a couple of things with your plan. 

1. Add an `activity_instances` to the plan which has expansion logic defined in the `expansion_set`
2. Run a simulation on the plan

### Prerequisite

At this time you will have to use the Hasura Action API in order to expand the plan into commands. 
You will have to retrieve some information needed to expand via the API. The following two values 
are needed:

* `expansionSetID`
* `simulationDatasetId`

Below is the query to retrieve an `expansionSetID`:

```
query GetExpansionSet(
  $commandDictionaryId: Int!
  $missionModelId: Int!
) {
  expansion_set(where: { mission_model_id: { _eq: $missionModelId }, command_dictionary: { id: { _eq: $commandDictionaryId} } }) {
    id
  }
}

# id : 1
```

Below is the query to retrieve the `simulationDatasetId`:

```
query GetSimulationDatasetId(
  $planId: Int!
) {
  simulation(where: { plan_id: { _eq: $planId } }, order_by: { dataset: { id: desc } }, limit: 1) {
    dataset {
      id
    }
  }
}

# id : 5
```

### Expanding

Below is an example of the Hasura Action you can use to expand a plan: 

```
mutation ExpandPlan(
  $expansionSetId: Int!
  $simulationDatasetId: Int!
) {
  expandAllActivities(expansionSetId: $expansionSetId, simulationDatasetId: $simulationDatasetId) {
    id
    expandedActivityInstances {
      commands {
        stem
      }
      errors {
        message
      }
    }
  }
}
```
