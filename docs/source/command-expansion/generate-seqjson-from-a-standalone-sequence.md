# Generate SeqJSON From Standalone Sequence

Some users will like to build a sequence seqJSON file outside of AERIE. Below is an example Sequence File that is compatible with AERIE. All you have to do is upload the contents of this file using the graphQL command. The sequence seqJSON file that are generated are not saved into AERIE. 

```ts
export default () =>
  Sequence.new({
    seqId: 'test_00001',
    metadata: {},
    commands: [
      BAKE_BREAD, 
      A`2020-060T03:45:19`.PREHEAT_OVEN(100)
    ],
  });
```

### GraphQL Query

```
query GetUserSequenceSeqJson($commandDictionaryID: Int!, $edslBody: String!) {
  getUserSequenceSeqJson(commandDictionaryID: $commandDictionaryID, edslBody: $edslBody) {
    id
    metadata
    steps {
      args
      metadata
      stem
      time {
        tag
        type
      }
      type
    }
  }
}
```

With the following `$edslBody`:

```json
{
  "commandDictionaryID": 1,
  "edslBody": "export default () =>\n  Sequence.new({\n    seqId: \"test_00001\",\n    metadata: {},\n    commands: [\n        BAKE_BREAD,\n        A`2020-060T03:45:19`.PREHEAT_OVEN(100),\n    ],\n  });"
}
```

Results:

```json
{
  "id": "test_00001",
  "metadata": {},
  "steps": [
    {
      "args": [],
      "metadata": {},
      "stem": "BAKE_BREAD",
      "time": {
        "type": "COMMAND_COMPLETE"
      },
      "type": "command"
    },
    {
      "args": [100],
      "metadata": {},
      "stem": "PREHEAT_OVEN",
      "time": {
        "tag": "2020-060T03:45:19.000",
        "type": "ABSOLUTE"
      },
      "type": "command"
    }
  ]
}
```
