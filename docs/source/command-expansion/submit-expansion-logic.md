# Submit Expansion Logic

## Upload

Once you've defined your expansion logic you need to upload it back to the commanding server and link it to the `activity_type` you wrote it for. Below you can use the GraphGL to upload the expansion logic. Use the GraphQL to upload the expansion logic file for the activity_type

```
mutation UploadExpansionLogic(
  $activityTypeName: String!
  $expansionLogic: String!
) {
  addCommandExpansionTypeScript(
    activityTypeName: $activityTypeName
    expansionLogic: $expansionLogic
  ) {
    id
  }
}
```

## Retrieve


If you would like to retrieve an expansion logic for modification use this GraphQL API below:

```
query GetExpansionLogic(
  $expansionRuleId: Int!
) {
  expansion_rule(where: {id: {_eq: $expansionRuleId}}) {
    expansion_logic
    activity_type
    id
  }
}
```

Retrieve a specific activity_type's various expansion logics:

```
query GetExpansionLogic(
  $activityType: String!
) {
  expansion_rule(where: {activity_type: {_eq: $activityType}}) {
    expansion_logic
    activity_type
    id
  }
}
```
