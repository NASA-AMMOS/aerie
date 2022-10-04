# Generate Activity TypeScript Library

In order to write an author expansion logic, you will need to know which `activity_types` to use. These `activity_types` are defined in the mission model.

Upon the selection of an `activity_type`, the command expansion service will generate a typescript library file containing all parameters that can be used when developing your logic. Below is the GraphQL request used to retrieve the library.

1. Run the graphQL query below

```
query GetActivityTypescript($activityTypeName: String!, $missionModelId: Int!) {
  getActivityTypeScript(activityTypeName: $activityTypeName, missionModelId: $missionModelId) {
    typescriptFiles {
      content
      filePath
    }
    reason
    status
  }
}
```

2. Save to a file <activity_type>_activity-types.ts ex. BakeBananaBread_activity-types.ts

### Return Values

* content - The typescript library for your activity_type
* filepath - the filename of your library
* reason - If any errors were thrown when generating this library file, the error message would be stored here. If there are no errors, you will get a `null`
* status - server-status ex. 404, 500, 200
