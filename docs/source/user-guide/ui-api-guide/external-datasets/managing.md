# Managing External Datasets

This document describes how to add external datasets to Aerie.

External datasets contain resource profiles that are quite similar to the profiles produced by a Merlin simulation. A resource profile describes the behavior of a resource over time. Each profile has a name, a type, a start time, and a list of profile segments that conform to that type. Each profile segment defines the value of the resource over a duration of time (referred to as its "dynamics"). The first segment starts at the start time of the profile, and every subsequent segment starts at the end of the previous segment.

External datasets come from the user rather than Merlin's simulation capabilities, and can be uploaded at any time before or after simulation. They can be viewed in the Aerie UI along with a plan.

Ultimately the purpose of external datasets is up to the user. You might be looking to include some geometry information to assist in building a plan, or you may simply be adding power and thermal modeling results to be viewable with existing simulation results. Just be aware that at this time external profiles are not accessible to the mission model during simulation, and are simply for viewing purposes in the UI.

## Add External Dataset Mutation

To upload an external dataset you need to add it to a specific plan. The dataset will persist as long as the plan exists, or until it is explicitly deleted. An external dataset can be added via the GraphQL mutation `addExternalDataset`:

```{eval-rst}
  .. include:: ../api-examples.rst
    :start-after: begin add external dataset
    :end-before: end add external dataset
```

The `addExternalDataset` GraphQL mutation takes three query variables as specified below:

| Parameter       | Type    | Description                                                                       |
| --------------- | ------- | --------------------------------------------------------------------------------- |
| `$planId`       | Integer | The ID of the plan to associate the external dataset with                         |
| `$datasetStart` | String  | The DOY UTC timestamp the dataset starts from<br/>UTC Format: `yyyy-dddThh:mm:ss` |
| `$profileSet`   | Object  | The set of precomputed profiles that make up the external dataset                 |

The profile set to be uploaded should have one entry for each profile, indexed by a unique name mapping to an object specifying the details of the profile.

Each profile should have a `type` field, which specifies whether the profile is real-valued or discrete-valued. It must also contain a `schema` field, which specifies the schema of the values it takes on.

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

| Field                 | Type                      | Description                                                                               |
| --------------------- | ------------------------- | ----------------------------------------------------------------------------------------- |
| `duration`            | Integer                   | The duration (in microseconds) the segment's dynamics hold before the next segment begins |
| `dynamics` (optional) | Dependent on profile type | The behavior of the profile over the lifetime of this segment                             |

A discrete profile's dynamics should match the format specified by the `schema` field, while a real profile's dynamics should always contain an initial value and a rate of change. See our example external dataset query variables [below](#example-query-variables-for-the-addexternaldataset-mutation) to see both profile specification types. If the `dynamics` field of a segment isn't specified, the segment is called a "gap", and represents intervals when the value is unknown.

## Delete External Dataset Mutation

There may be a time when you find an external dataset you've been using is no longer relevant and must be removed. You can use the following mutation:

```{eval-rst}
  .. include:: ../api-examples.rst
    :start-after: begin delete external dataset
    :end-before: end delete external dataset
```

You can use the following query variable specifying the external dataset `id` you wish to delete:

```json
{
  "id": 1
}
```

## Example Query Variables for the AddExternalDataset Mutation

Below shows example [query variables](https://graphql.org/learn/queries/#variables) you can use with the [addExternalDataset mutation](#add-external-dataset-mutation).

### Create an External Dataset with Real and Discrete Profiles

This example shows an external dataset being uploaded to the plan with ID `2` starting at `2018-331T04:00:00`. Two precomputed profiles are included in the external dataset.

First a real profile called `batteryEnergy` starts at a value of 50 and decreases at a rate of -0.5 units per second over 30 seconds. At that point, the value is 35 and the rate is changed to -0.1 units per second for 30 more seconds.

The second profile is a discrete profile called `awake` and contains a schema that tells us its values are boolean. The segments tell us that for the first 30 seconds the profile's dynamics are the value `true` and for the next 30 seconds the value `false`.

```json
{
  "planId": 2,
  "datasetStart": "2018-331T04:00:00",
  "profileSet": {
    "batteryEnergy": {
      "type": "real",
      "schema": {
        "type": "struct",
        "items": {
          "rate": { "type": "real" },
          "initial": { "type": "real" }
        }
      },
      "segments": [
        { "duration": 30000000, "dynamics": { "initial": 50, "rate": -0.5 } },
        { "duration": 30000000, "dynamics": { "initial": 35, "rate": -0.1 } }
      ]
    },
    "awake": {
      "type": "discrete",
      "schema": { "type": "boolean" },
      "segments": [
        { "duration": 30000000, "dynamics": true },
        { "duration": 30000000, "dynamics": false }
      ]
    }
  }
}
```

### Create an External Dataset with a Profile Gap

This example adds a single precomputed profile called `orientation`. This discrete profile's schema tells us that its values are structs with real-valued `x`, `y` and `z` fields. For the first hour the profile takes a value of `x=0`, `y=0`, `z=1`. Then the profile has a gap for an hour. For the third hour thereafter the profile is valued at `x=1`, `y=1`, `z=0`.

```json
{
  "planId": 7,
  "datasetStart": "2038-192T14:00:00",
  "profileSet": {
    "orientation": {
      "type": "discrete",
      "schema": {
        "type": "struct",
        "items": {
          "x": { "type": "real" },
          "y": { "type": "real" },
          "z": { "type": "real" }
        }
      },
      "segments": [
        { "duration": 3600000000, "dynamics": { "x": 0, "y": 0, "z": 1 } },
        { "duration": 3600000000 },
        { "duration": 3600000000, "dynamics": { "x": 1, "y": 1, "z": 0 } }
      ]
    }
  }
}
```

### Create an External Dataset from a CSV

This example shows how to convert a [CSV](https://en.wikipedia.org/wiki/Comma-separated_values) into an external dataset with 3 profiles. The CSV has the following form:

| Time (s)    | TotalPower       | BatteryStateOfCharge | Temperature       |
| ----------- | ---------------- | -------------------- | ----------------- |
| 164937600.0 | 0.0              | 143.15               | 0.0               |
| 164937700.0 | 384.999999940483 | 1.4                  | -12.0964867663028 |
| 164937800.0 | 384.999999399855 | 137.45               | -12.0974993557598 |
| 164937900.0 | 385.000010807604 | 134.85               | -12.0985125609155 |
| 164938000.0 | 381.80000002749  | 132.4                | -12.0995253838464 |

Here `Time` is expressed in seconds and you can see in this example there are 100 second increments between each row.  
`TotalPower`, `BatteryStateOfCharge`, and `Temperature` are the data that we import as profiles.

Here is the example query variable showing the CSV converted into external dataset profiles. The external dataset is added to a plan with ID `1` and starts at `2024-001T00:00:00` UTC.

```json
{
  "planId": 1,
  "datasetStart": "2024-001T00:00:00",
  "profileSet": {
    "TotalPower": {
      "type": "real",
      "schema": {
        "type": "struct",
        "items": {
          "rate": { "type": "real" },
          "initial": { "type": "real" }
        }
      },
      "segments": [
        { "duration": 100000000, "dynamics": { "initial": 0.0, "rate": 0.0 } },
        { "duration": 100000000, "dynamics": { "initial": 384.999999940483, "rate": 0.0 } },
        { "duration": 100000000, "dynamics": { "initial": 384.999999399855, "rate": 0.0 } },
        { "duration": 100000000, "dynamics": { "initial": 385.000010807604, "rate": 0.0 } },
        { "duration": 100000000, "dynamics": { "initial": 381.80000002749, "rate": 0.0 } }
      ]
    },
    "BatteryStateOfCharge": {
      "type": "discrete",
      "schema": { "type": "real" },
      "segments": [
        { "duration": 100000000, "dynamics": 143.15 },
        { "duration": 100000000, "dynamics": 1.4 },
        { "duration": 100000000, "dynamics": 137.45 },
        { "duration": 100000000, "dynamics": 134.85 },
        { "duration": 100000000, "dynamics": 132.4 }
      ]
    },
    "Temperature": {
      "type": "discrete",
      "schema": { "type": "real" },
      "segments": [
        { "duration": 100000000, "dynamics": 0.0 },
        { "duration": 100000000, "dynamics": -12.0964867663028 },
        { "duration": 100000000, "dynamics": -12.0974993557598 },
        { "duration": 100000000, "dynamics": -12.0985125609155 },
        { "duration": 100000000, "dynamics": -12.0995253838464 }
      ]
    }
  }
}
```

Notice for `TotalPower` we use a `real` profile just for example completeness. Since we are only dealing with explicit data points in the CSV and not the rate at which the data points change, the dynamics rate for each `real` profile segment is `0.0`. Thus we could have equivalently encoded `TotalPower` as a `discrete` profile as we do for `BatteryStateOfCharge` and `Temperature`.

Also notice the `duration` is simply calculated as how many microseconds pass between each data point in the CSV.
