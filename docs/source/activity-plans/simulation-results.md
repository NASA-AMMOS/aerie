# Simulation Results

After running a simulation, the results will be saved in the database, and queryable via the GraphQL API.

## Querying for Simulation Results

There are two ways to query for simulation results: via the simulate action, and directly from the database.

### Querying for simulation results via the simulate action

The `simulate` action always immediately returns a status. That status is one of the following:
- pending
- incomplete
- complete
- failed

The first time merlin receives a `simulate` request, it will add the request to its queue, and immediately return an "incomplete" status. The expectation is that the user will re-send the same request repeatedly, with an interval of at least 0.25 seconds, until one of the other two statuses is returned.

After the simulation completes, subsequent requests will receive either a `complete` or a `failed` status. If the simulation fails, the `reason` field will be populated with a message explaining what went wrong.

A message with a `complete` status will also populate the `results` field with information about the results of the simulation.

### Querying for simulation results directly

In the near future, the recommended way to get simulation results will be directly from the database, rather than via merlin. Docs forthcoming as we iron out the details there.

There are several pieces of information included in the simulation results.

## Contents of Simulation Results

### Resource Profiles

For each resource, a profile allows the user to determine the value of that resource at any given time during the simulation.

There are currently two types of resource profiles, which each explain how to interpolate their values. A **discrete profile** is a list of values and times. The value of that resource at some time `t` would be the value of the last profile segment prior to `t`. A **real profile** allows smoother interpolation defined by a linear equation. Thus the value at time `t` is the value at the last profile segment prior to `t` (let's call it `t_prev`) plus the value of the linear equation evaluated at `t - t_prev`.

### Resource Samples

See the [`resourceSamples` GraphQL query](../aerie-api/aerie-graphql-api.md#query-for-all-resource-samples-in-simulated-plan) for details on how to query for resource samples.

### Simulated Activities

These are records of completed activity instances. They include not only the activities that were part of the plan, but also their child activities (see [decomposition](../mission-modeling/activities.md#a-note-about-decomposition)).

The **type** of a simulated activity is a string referring to the `ActivityType` of this activity instance.

The **arguments** is a map of strings to serialized values representing the arguments that were supplied to this activity instance. These arguments can come from the planner, or, in the case of child activities, from the parent activity.

The **start** instant represents the time at which this activity started. This time may have been set by the planner, or, in the case of child activities, it may have been determined by the logic in the parent's effect model.

The **duration** of an activity is the time that elapsed between the start of an activity and the end of that activity. Note that an activity is not considered to be terminated until all of its children have also terminated.

The **parentId** of an activity in the case of a child activity refers to the id of the parent activity. In the case of an activity created by a planner, this field will be `null`.

The **childIds** is a list of ids of the children of this activity. This list will be empty if this activity has not spawned any children.

The **directiveId** will be populated if this activity itself was placed by the planner. It will be empty if it was a child activity.

> Note: `directiveId` and `parentId` are related concepts - if one is present, the other is absent, and vice versa.

The **computedAttributes** of an activity is a piece of information logged by the effect model upon termination of the activity.

The schema for the computed attributes can be found by querying the Activity Type, which is not included in simulation results. If the activity type does not define a computed attributes schema, the default value for computed attributes is `VOID`. 

### Unfinished Activities

Some activities may not terminate within the time limit of the simulation. The types and inputs to the activities will be listed here. Unfinished activities will not have computed attributes, since those are provided upon termination of an activity.

### Events

Every change to the simulation is recorded in an event. Some of these events can be exported as part of simulation results, at the discretion of the mission model.
