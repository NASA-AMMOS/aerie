# Get Expanded Commands

When the plan has been expanded you will be able to retrieve the generated commands from the expansion run. Each `activity_instance` will have commands and any errors generated. If there are no commands and errors listed then an activity_instance didn't have any expansion logic defined. Below is an example of how to retrieve all the expanded commands from an expansion run or a specific command from activity_instance. 

```
query GetAllExpandedCommandsForExpansionRun(
  $expansionRunId: Int!
) {
  activity_instance_commands(where: {expansion_run: {id: {_eq: $expansionRunId}}}) {
    activity_instance_id
    commands
    errors
  }
}
```

```
query GetCommandsForExpansionRunAndActivityInstance(
  $expansionRunId: Int!
  $activityInstanceId: Int!
) {
  activity_instance_commands(where: {activity_instance_id: {_eq: $activityInstanceId}, expansion_run: {id: {_eq: $expansionRunId}}}) {
    commands
    errors
  }
}
```
