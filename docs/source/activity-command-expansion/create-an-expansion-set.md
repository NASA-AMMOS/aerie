# Create An Expansion Set

Think of an "expansion_set" as a way of grouping expansion logic to different versions of mission models 
or command dictionaries.

For example, say you have a mission model and command dictionary version 1. You have written expansion 
logic that uses commands from the command dictionary. Later the command dictionary or mission model is 
updated to version 2.  If your original expansion logic was created to only be compatible with the previous
version of the command dictionary or mission model, you need to go through and rewrite/update your 
existing logic for all the activity_types.

An alternative is that you can use the "expansion_set". This will allow you to copy over a set of expansion
logic into a new "expansion_set" without manually changing anything in your code. Some expansion logic 
might not be valid anymore but the user can decide which expansion logic to copy over to the new set 
containing the updated command dictionary or mission model.

A future feature will inform users of which expansion rules are compatible/incompatible for the selected
mission model and command dictionary,
making the migration of expansion_sets easier.

The below GraphQL creates and expansion set for a given command dictionary and mission model which 
includes the provided expansions.

```
mutation CreateExpansionSet(
  $commandDictionaryId: Int!
  $missionModelId: Int!
  $expansionIds: [Int!]!
) {
  createExpansionSet(
    commandDictionaryId: $commandDictionaryId
    missionModelId: $missionModelId
    expansionIds: $expansionIds
  ) {
    id
  }
}
```

**Ex. 1**

That was a lot to take in. Let's create our own expansion set and tie everything together. Below is an 
example GraphGL to create an "expansion_set". With the 4 expansion logic defined we want to group them 
with command dictionary and mission model version 1.

```
{
  "commandDictionaryId": 1,
  "missionModelId": 1,
  "expansionIds": [1,2,4,9]
}
```

**Ex.2**

Now let us say the mission model has changed and out of the 4 activity_types defined in the mission 
model we are dropping one of them. We will only copy over the expansion logic that is valid in this
new set.

```
{
  "commandDictionaryId": 1,
  "missionModelId": 2,
  "expansionIds": [1,2,9]
}
```

## List Expansion Sets

Below is a way to list the created expansion sets in GraphGL:

```
query GetAllExpansionSets {
  expansion_set {
    id
    command_dictionary {
      version
    }
    mission_model {
      id
    }
    expansion_rules {
      activity_type
      id
    }
  }
}
```

Get all `expansion_set`'s for a particular `mission_model` or `command dictionary`:

```
query GetSpecificExpansionSet(
  $commandDictionaryId: Int!
  $missionModelId: Int!
) {
  expansion_set(where: {command_dictionary: {id: {_eq: $commandDictionaryId}}, mission_model_id: {_eq: $missionModelId}}) {
    id
    expansion_rules {
      activity_type
      id
    }
  }
}
```
