# Activity Directive Metadata

This document describes how to add mission-specific metadata (aka annotations) to activity directives. There are 6 different types of metadata which are summarized in the table below.

| Type          | Schema                                             | Display                   |
| ------------- | -------------------------------------------------- | ------------------------- |
| `string`      | `{ "type": "string" }`                             | Single line text field    |
| `long_string` | `{ "type": "long_string" }`                        | Textarea                  |
| `boolean`     | `{ "type": "boolean" }`                            | Checkbox                  |
| `number`      | `{ "type": "number" }`                             | Single line numeric field |
| `enum`        | `{ "enumerates": [], "type": "enum" }`             | Single select dropdown    |
| `string`      | `{ "enumerates": [], "type": "enum_multiselect" }` | Multi select dropdown     |

## Adding Metadata Schemas

To add mission-specific metadata schemas you need to use the GraphQL API. Here is a mutation that adds an example of each type.

#### Mutation

```
mutation CreateActivityDirectiveMetadataSchemas(
  $schemas: [activity_directive_metadata_schema_insert_input!]!
) {
  insert_activity_directive_metadata_schema(objects: $schemas) {
    affected_rows
    returning {
      created_at
      key
      schema
      updated_at
    }
  }
}
```

#### Query Variable

```json
{
  "schemas": [
    { "key": "STRING_EXAMPLE", "schema": { "type": "string" } },
    { "key": "LONG_STRING_EXAMPLE", "schema": { "type": "long_string" } },
    { "key": "BOOLEAN_EXAMPLE", "schema": { "type": "boolean" } },
    { "key": "NUMBER_EXAMPLE", "schema": { "type": "number" } },
    {
      "key": "ENUM_EXAMPLE",
      "schema": { "enumerates": ["A", "B", "C"], "type": "enum" }
    },
    {
      "key": "ENUM_MULTISELECT_EXAMPLE",
      "schema": {
        "enumerates": ["D", "E", "F", "G"],
        "type": "enum_multiselect"
      }
    }
  ]
}
```
