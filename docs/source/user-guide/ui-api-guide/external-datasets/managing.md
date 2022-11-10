# Managing External Datasets

External profiles are quite similar to the profiles produced by a Merlin simulation. They describe the behavior of some dynamic characteristic over time and can be viewed in the Aerie UI along with a plan. External profiles come from the user, rather than Merlin's simulation capabilities, and can be uploaded at any time, before or after simulation.

Ultimately, the purpose of external profiles is up to the user. You might be looking to include some geometry information to assist in building a plan, or you may simply be adding power and thermal modeling results to be viewable with existing simulation results. Just be aware that at this time external profiles are not accessible to the mission model during simulation, and are simply for viewing purposes in the UI.

## How To Upload A Dataset
To upload external profiles, you will need to add them to a specific plan. A collection of profiles that all start at the same time are called an "External Dataset". The dataset must be associated with a plan, and will persist as long as the plan exists, or until it is deleted by a user. An external dataset can be added via the GraphQL mutation `addExternalDataset`.

### GraphQL Mutation: `addExternalDataset`
The `addExternalDataset` GraphQL mutation takes three parameters as specified below:

| Parameter      | Type    | Description                                                                      |
|----------------|---------|----------------------------------------------------------------------------------|
| `planId`       | Integer | The ID of the plan to associate the external dataset with.                       |
| `datasetStart` | String  | The UTC timestamp the dataset is based from.<br/>UTC Format: `yyyy-dddThh:mm:ss` |
| `profileSet`   | Object  | The set of precomputed profiles that make up the external dataset.               |

The profile set to be uploaded should have one entry for each profile, indexed by a unique name mapping to an object specifying the details of the profile. 
Each profile should have a `type` field, which specifies whether the profile is real- or discrete-valued. 
It must also contain a `schema` field, which specifies the schema of the values it takes on. 
For discrete profiles, these are not limited to basic types, but can take on any complex structure made up using our `ValueSchema` construct (for more information, see our [ValueSchema documentation](../../mission-modeler-guide/custom-value-types/index.rst)). 
Currently, real profiles only support linear equations with the following schema:


```json
{
  "type": "struct",
  "items": {
    "rate": {
      "type": "real"
    },
    "initial": {
      "type": "real"
    }
  }
}
```

Finally, each profile requires a list of segments that describe the actual behavior of the profile. The `segments` field is a list of segment objects, where each segment should contain the following two fields:

| Field                 | Type                      | Description                                                                                |
|-----------------------|---------------------------|--------------------------------------------------------------------------------------------|
| `duration`            | Integer                   | The duration (in microseconds) the segment's dynamics hold before the next segment begins. |
| `dynamics` (optional) | Dependent on profile type | The behavior of the profile over the lifetime of this segment.                             |

A discrete profile's dynamics should match the format specified by the `schema` field, while a real profile's dynamics should always contain an initial value and a rate of change. See our example external dataset mutations [below](#example-mutations) to see both profile specification types. If the `dynamics` field of a segment isn't specified, the segment is called a "gap", and represents intervals when the value is unknown.

## Deleting External Datasets
There may be a time when you find an external dataset you've been using is no longer relevant, and must be removed. This is easily done by providing the ID of the dataset you wish to delete to the `delete_dataset_by_pk` mutation. For example, the following mutation will delete the dataset with id 5:

```{eval-rst}
  .. include:: ../api-examples.rst
    :start-after: begin delete external dataset
    :end-before: end delete external dataset
```

## Example Mutations

### Example 1

This example shows an external dataset being uploaded to the plan with ID `2` starting at `2018-331T04:00:00`. Two precomputed profiles are included in the external dataset. First a real profile called `batteryEnergy` starts at a value of 50 and decreases at a rate of -0.5 units per second over 30 seconds. At that point, the value is 35 and the rate is changed to -0.1 units per second for 30 more seconds. The second profile, a discrete profile called `awake` contains a schema that tells us its values are boolean. The segments tell us that for the first 30 seconds the profile's dynamics are the value `true` and for the next 30 seconds the value `false`. At the end we see the ID of the external dataset that is created is queried as the result of this mutation.

``` 
mutation {
  addExternalDataset(
    planId: 2
    datasetStart: "2018-331T04:00:00"
    profileSet: {
      batteryEnergy: {
        type: "real"
        schema: { type: "struct", items: {
          rate: { type: "real" },
          initial: { type: "real" }
        }}
        segments: [
          { duration: 30000000, dynamics: { initial: 50, rate: -0.5 } }
          { duration: 30000000, dynamics: { initial: 35, rate: -0.1 } }
        ]
      }
      awake: {
        type: "discrete"
        schema: { type: "boolean" }
        segments: [
          { duration: 30000000, dynamics: true }
          { duration: 30000000, dynamics: false }
        ]
      }
    }
  ) {
    datasetId
  }
}
```

### Example 2

This mutation adds to plan with id `7` an external dataset starting at `2038-192T14:00:00` with a single precomputed profile, `orientation`. This discrete profile's schema tells us that its values are structs with real-valued `x`, `y` and `z` fields. For the first hour, the profile takes a value of `x=0`, `y=0`, `z=1`. Then, the profile has a gap for an hour. For the third hour thereafter, the profile is valued at `x=1`, `y=1`, `z=0`.

```
mutation {
  addExternalDataset(
    planId: 7
    datasetStart: "2038-192T14:00:00"
    profileSet: {
      orientation: {
        type: "discrete"
        schema: {
          type: "struct"
          items: {
            x: { type: "real" }
            y: { type: "real" }
            z: { type: "real" }
          }
        }
        segments: [
          { duration: 3600000000, dynamics: { x: 0, y: 0, z: 1 } }
          { duration: 3600000000 }
          { duration: 3600000000, dynamics: { x: 1, y: 1, z: 0 } }
        ]
      }
    }
  ) {
    datasetId
  }
}
```
