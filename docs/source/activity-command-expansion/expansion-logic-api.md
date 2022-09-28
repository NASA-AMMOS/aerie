# Expansion Logic API

Here is a list of APIs that AERIE provides to help you build your expansion logic.

## Activity's Attributes

You have access to the activity's attributes such as `start time,` `end time`, `duration`, etc

```typescript
C.ECHO(`Activity's Start Time: ${props.activityInstance.startTime}`),     // YYYY-DDDThh:mm:ss
C.ECHO(`Activity's End Time: ${props.activityInstance.endTime}`),         // YYYY-DDDThh:mm:ss
C.ECHO(`Activity's Start Offset: ${props.activityInstance.startOffset}`), // hh:mm:ss
C.ECHO(`Activity's Duration: ${props.activityInstance.Duration}`),        // hh:mm:ss

```

## Computed Attributes

You have access to the activity type's computed attributes. You can use these computed attributes in your expansion logic

```ts
export default function CommandExpansion(props: {
  activityInstance: ActivityType;
}): ExpansionReturn {
  return [
    PeelBanana(),
    C.PREHEAT_OVEN(350),
    C.PREPARE_LOAFF(1, false),
    C.BAKE_BREAD,
  ];

  function PeelBanana(): Command {
    if (props.activityInstance.attributes.computed < 2) {
      return C.PEEL_BANANA("fromStem");
    } else {
      return C.ECHO("Already have enough Banana's peeled...");
    }
  }
}
```

## Time

You can specify a time argument for your commands. The time arguments supported are `absolute`, `relative`, and `epoch`. You will be using the Javascript Temporal Polyfill to represent time values. https://tc39.es/proposal-temporal/docs/


* Absolute Time ([Temporal.Instant](https://tc39.es/proposal-temporal/docs/instant.html)):   YYYY-DDDThh:mm:ss
* Relative Time ([Temporal.Duration](https://tc39.es/proposal-temporal/docs/duration.html)): hh:mm:ss
* Epoch Time ([Temporal.Duration](https://tc39.es/proposal-temporal/docs/duration.html)):    hh:mm:ss


```typescript
// Absolute examples
A`2020-060T03:45:19`.ADD_WATER
A(Temporal.Instant.from("2025-12-24T12:01:59Z")).ADD_WATER
A("2020-060T03:45:19").ADD_WATER

// Relative examples
R`00:15:00`.EAT_BANANA
R(Temporal.Duration.from({ minutes: 15 }).EAT_BANANA
R('00:15:00').EAT_BANANA

// Epoch examples
E`00:15:30`.PREPARE_LOAF(1,true)
E(Temporal.Duration.from({ minutes: 15, seconds: 30 }).PREPARE_LOAF(1,true)
E('00:15:30').PREPARE_LOAF(1,true)

// Command Complete examples
C.BAKE_BREAD
``` 
