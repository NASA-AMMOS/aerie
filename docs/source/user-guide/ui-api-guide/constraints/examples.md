# Examples

To define a constraint, you will need to build it up by constructing and transforming objects in the [constraint API](../../../constraints-edsl-api/index). To help get you started, here are a few examples:

## Accessing Resource Profiles

Let's start off with a basic constraint that a resource, let's call it `BatteryTemperature`, doesn't exceed some threshold, say 340. We do so by using `Real.Resource(...)` to get the `BatteryTemperature` resource, and `Real.Value(...)` to get a real number we can compare a real resource profile to.

```typescript
export default (): Constraint =>
  Real.Resource("BatteryTemperature") // this references a real profile
      .lessThanOrEqual(Real.Value(340)); // this transforms it into Windows
```

The `Real.Resource(...)` function creates an object that refers to the `BatteryTemperature` real resource profile. The `.lessThanOrEqual(...)` method then expects either another real profile or a number literal as argument. In the above example, we passed it a real profile which has the value `340` for all time. We could instead omit `Real.Value(...)`, and `.lessThanOrEqual(...)` will automatically wrap the `340` literal in `Real.Value(...)` for us:

```typescript
// This is identical
export default (): Constraint =>
  Real.Resource("BatteryTemperature").lessThanOrEqual(340);
```

The result of `.lessThanOrEqual(...)` is a `Windows` object, representing the time windows when the condition is true.

## Manipulating Windows

Now we examine a more complex constraint. Let's imagine a solar panel that rotates the panels to a certain angle. Suppose the panels are able to rotate as fast as 5 degrees per second, but are not allowed to go more than 3 degrees per second unless the spacecraft is operating in IDLE mode. For this we will use a real resource, PanelAngle, and a discrete resource, `OpMode`.

Note that this breaks down to two conditions, either of which must be true the entire simulation. This constraint should be satisfied as as either:

1. The `OpMode` is `"IDLE"`
2. The rate of the `PanelAngle` is no more than 3 degrees per second

```typescript
export default (): Constraint =>
    Windows.Or( // This "or"s together any number of Windows objects
        Discrete.Resource("OpMode").equal("IDLE"),
        Real.Resource("PanelAngle").rate().lessThan(3)
    );
```

The API keeps track of the type schemas of all your Discrete and Real value profiles. Real profiles are easy; they are always numbers. The structures of Discrete profiles are defined by the simulation developer. For example, the `OpMode` resource might be defined as an enum of either `"IDLE"` or `"ACTIVE"`. If you tried to use a different value, like `Discrete.Resource("OpMode").equal("BOOGIE")`, it would throw a compile-time type error.

Much like the previous example, the `.equal("IDLE")` method could instead be `.equal(Discrete.Value("IDLE"))`. The `equal` method and all such comparison operators operate on Discrete and Real profiles of the same type. If you provide a literal instead of a profile, it will be automatically wrapped in `Discrete.Value(...)` if it is the correct type.

We also provide a helper function `if`, which is used when a condition only needs to apply at certain times. `checkTheseWindows.if(onlyTheseWindowsAreTrue)` translates to `Windows.Or(checkTheseWindows, onlyTheseWindowsAreTrue.not())`. So the above example could be rewritten as:

```typescript
export default (): Constraint =>
    Real.Resource("PanelAngle").rate.lessThan(3)
        .if(Discrete.Resource("OpMode").notEqual("IDLE"));
```

## Accessing Activities

This example of an activity constraint says that whenever an instance of `ActivityTypeA` occurs, the value of `ResourceX` must be less than 10.0. This will be evaluated on every instance of an `ActivityTypeA` activity.

```typescript
export default (): Constraint =>
    Constraint.ForEachActivity(ActivityType.ActivityTypeA, (instance) =>
        Real.Resource("ResourceX").lessThan(10).if(instance.window())
    );
```

For those unfamiliar with Typescript, the `instance => ...` syntax defines an anonymous function which the `Constraint.ForEachActivity(...)` function calls. `instance` is of the type `ActivityInstance<A extends ActivityType>`, and can be used to access the instance's window, start time, end time, and parameters. Unfortunately, in order for `ForEachActivity` to behave correctly in more complex cases, it needs to re-evaluate the condition on the whole plan for each instance separately. This means that we need to manually trim the violation down to the extent of the activity with `.if(instance.window())`.

## Violation Examples

Constraint violations contain two sets of information describing where constraints are violated. First, a list of associated activity instance IDs representing the activity instances in violation (this will be an empty list for constraints that don't involve activities). Second, the list of violation windows themselves tells when during the simulation violations occur.

Constraint violations are reported per activity instance, so it is entirely possible for multiple violations to be produced by a single constraint. This unambiguous representation clearly indicates activity instances that violate a constraint despite the constraint being defined at the type-level.

Below are several examples of constraint violations:

A single activity instance with ID "2" is in violation from the start of the plan for one hour. Durations are in microseconds.

```json
{
  "activityInstanceIds": ["2"],
  "windows": [
    [0, 3600000000]
  ]
}
```

A constraint is violated for the first and fourth hours of the plan. No activities are involved in this violation.

```json
{
  "activityInstanceIds": [],
  "windows": [
    [0, 3600000000],
    [14400000000, 18000000000]
  ]
}
```
