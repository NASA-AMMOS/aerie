# Overview

When analyzing a simulation's results, it may be useful to detect windows where certain conditions are met. Constraints are the Aerie tool for fulfilling that role. A constraint is a condition on activities and resources that must hold through an entire simulation. If a constraint does not hold true at any point in a simulation, this is considered a violation. The results yielded by a simulation run will include a list of violation windows for every constraint that has been violated.

## Creating a Constraint in Aerie

Constraints can be created via the Aerie GraphQL API.

## Constraint Examples

To define a constraint, you will need to build it up by constructing and transforming objects in the [constraint API](#constraints-api). To help get you started, here are a few examples:

#### Constraint Example 1

Let's start off with a basic constraint that a resource, let's call it `BatteryTemperature`, doesn't exceed some threshold, say 340. We do so by using `Real.Resource(...)` to get the `BatteryTemperature` resource, and `Real.Value(...)` to get a real number we can compare a real resource profile to.

```typescript
export default (): Constraint =>
  Real.Resource("BatteryTemperature").lessThanOrEqual(Real.Value(340));
```

The `Real.Resource(...)` function creates an object that refers to the `BatteryTemperature` real resource profile. The `.lessThanOrEqual(...)` method then expects either another real profile or a number literal as argument. In the above example, we passed it a real profile which has the value `340` for all time. We could instead omit `Real.Value(...)`, and `.lessThanOrEqual(...)` will automatically wrap the `340` literal in `Real.Value(...)` for us:

```typescript
// This is identical
export default (): Constraint =>
  Real.Resource("BatteryTemperature").lessThanOrEqual(340);
```

The result of `.lessThanOrEqual(...)` is a `Windows` object, representing the time windows when the condition is true.

#### Constraint Example 2

Now we examine a more complex constraint. Let's imagine a solar panel that rotates the panels to a certain angle. Suppose the panels can rotate as fast as 5 degrees per second, but are not allowed to go more than 3 degrees per second unless the spacecraft is operating in IDLE mode. For this we will use a real resource, PanelAngle, and a discrete resource, `OpMode`.

Note that this breaks down to two conditions, either of which must be true the entire simulation. This constraint should be satisfied as as either:

1. The `OpMode` is `"IDLE"`
2. The rate of the `PanelAngle` is no more than 3 degrees per second

```typescript
export default (): Constraint =>
  Windows.Any(
    Discrete.Resource("OpMode").equal("IDLE"),
    Real.Resource("PanelAngle").rate().lessThan(3)
  );
```

The API keeps track of the type schemas of all your Discrete and Real value profiles. Real profiles are easy; they are always numbers. The structures of Discrete profiles are defined by the simulation developer. For example, the `OpMode` resource might be defined as an enum of either `"IDLE"` or `"ACTIVE"`. If you tried to use a different value, like `Discrete.Resource("OpMode").equal("BOOGIE")`, it would throw a compile-time type error.

Much like the previous example, the `.equal("IDLE")` method could instead be `.equal(Discrete.Value("IDLE"))`. The `equal` method and all such comparison operators operate on Discrete and Real profiles of the same type. If you provide a literal instead of a profile, it will be automatically wrapped in `Discrete.Value(...)` if it is the correct type.

#### Constraint Example 3

The first example of an activity constraint we present says that whenever an instance of `ActivityTypeA` occurs, the value of `ResourceX` must be less than 10.0.

```typescript
export default (): Constraint =>
  Constraint.ForEachActivity(ActivityType.ActivityTypeA, (instance) =>
    Real.Resource("ResourceX").lessThan(10).if(instance.window())
  );
```

For those unfamiliar with Typescript, the `instance => ...` syntax defines an anonymous function which the `Constraint.ForEachActivity(...)` function calls. `instance` is of the type `ActivityInstance<A extends ActivityType>`, and can be used to access the instance's window, start time, end time, and parameters.

The `.if(instance.window())` is very important. Without it, the constraint would apply at all times, not just during `ActivityTypeA`. In general, `expressionWindows.if(conditionWindows)` means that `expressionWindows` is only checked during the windows of `conditionWindows`. It is equivalent to `Windows.Any(conditionWindows.invert(), expressionWindows)`, meaning that the above example could also be written:

```typescript
export default (): Constraint =>
  Constraint.ForEachActivity(ActivityType.ActivityTypeA, (instance) =>
    Windows.Any(
      instance.window().invert(),
      Real.Resource("ResourceX").lessThan(10)
    )
  );
```

#### Constraint Example 4

As a final example, we present a complex constraint containing two activity types, and several nested expressions. Most constraints should be much simpler than this, but this demonstrates just how capable the constraints API is.

The constraint below basically says this: For each pair of activities of type TypeA and TypeB, during the intersection of the two activities either parameter `b` of the TypeB instance must be false, or Resource ResC must be no greater than half of parameter `a` of the TypeA instance. Quite a mouthful, but here it is:

```typescript
export default (): Constraint =>
  Constraint.ForEachActivity(ActivityType.TypeA, (instanceA) =>
    Constraint.ForEachActivity(ActivityType.TypeB, (instanceB) =>
      Windows.Any(
        instanceB.parameters.b.equal(false),
        Real.Resource("ResC").lessThanOrEqual(instanceA.parameters.a.times(0.5))
      )
        .if(instanceA.window())
        .if(instanceB.window())
    )
  );
```

## Violation Examples

Constraint violations contain two sets of information describing where constraints are violated. First, a list of associated activity instance IDs representing the activity instances in violation (this will be an empty list for constraints that don't involve activities). Second, the list of violation windows themselves tells when during the simulation violations occur.

Constraint violations are reported per activity instance, so it is entirely possible for multiple violations to be produced by a single constraint. This unambiguous representation clearly indicates activity instances that violate a constraint despite the constraint being defined at the type-level.

Below are several examples of constraint violations:

A single activity instance with ID "2" is in violation from time 5 to 7:

```json
{
  "activityInstanceIds": ["2"],
  "windows": [[5, 7]]
}
```

A constraint is violated from 2 to 4 and also from 5 to 8. No activities are involved in this violation.

```json
{
  "activityInstanceIds": [],
  "windows": [
    [2, 4],
    [5, 8]
  ]
}
```

## Constraints API

Below we list all classes, constructors, and methods in the <a href="../../constraints-edsl-api/{{current_version}}/index.html" target="_blank">Constraint EDSL API</a>, along with their arguments and return types. But before we do that, keep in mind that the entire API is a facade and does _none_ of the constraint evaluation itself. So for example, when we say that `.lessThan(...)` returns a "set of windows", this is merely a helpful lie.

In reality, it returns a node of an Abstract Syntax Tree (AST) which represents the `LessThan` operation, which is serialized and sent to Merlin's Java backend to be evaluated there. Before the constraints API was released, constraint authors had to write a highly verbose JSON AST representing these operations. The API builds that same AST. Due to this architecture, it is not possible to use the constraint to actually _observe_ the "set of windows" represented by `.lessThan(...)`, or the "discrete profile" represented by `Discrete.Resource(...)`, or any other value or object "returned" by the API.

---

### `Constraint`

The `Constraint` class is what the constraint function returns, and can represent all possible constraints. In some use cases, the constraint author will directly create a `Constraint`, such as with the constructor functions below. In many other use cases, the author will return a `Windows` object instead, which will be automatically converted to a `Constraint` as if they had called [the `windows.violations()` method](#method-violations).

#### constructor `ForEachActivity`

Evaluates a constraint on each instance of an activity type, and evaluates to the aggregated list of violations

- **Arguments:**
  - `activityType`: variant of `ActivityType`
  - `expression`: A function of signature `(instance: ActivityInstance<A extends ActivityType>) => Constraint`
- **Returns:** `Constraint`

#### constructor `ForbiddenActivityOverlap`

Evaluates to a list of violations, one for each overlap between the given activity types.

- **Arguments:**
  - `activityType1`: variant of `ActivityType`
  - `activityType2`: variant of `ActivityType`
- **Returns:** `Constraint`

---

### `ActivityType`

An enum of activity type names. The variants are generated automatically from the mission model.

---

### `ActivityInstance<A extends ActivityType>`

Represents an instance of an activity type. It is only intended to be created by the `ForEachActivity` constructor and accessed by the function

#### method `window`

Gets the window from start to end of the activity instance

- **Returns:** `Windows`

#### method `start`

Gets the instantaneous window at the start of the activity instance

- **Returns:** `Windows`

#### method `end`

Gets the instantaneous window at the end of the activity instance

- **Returns:** `Windows`

#### getter method `parameters`

Gets an object containing each of the activity parameters' referenced profile as a field

- **Returns:** ActivityParameters<A extends ActivityType>

---

(windows)=

### `Windows`

Represents a set of time intervals (a.k.a. windows).

#### constructor `All`

Computes the intersection of the input windows

- **Arguments:**
  - `...windows`: any number of `Windows` objects
- **Returns:** `Windows`

#### constructor `Any`

Computes the union of the input windows

- **Arguments:**
  - `...windows`: any number of `Windows` objects
- **Returns:** `Windows`

#### method `if`

Transforms a windows object, filtering `this`'s windows by only checking them during `condition`'s windows. `expressionWindows.if(conditionWindows)` is equivalent to `Windows.Any(conditionWindows.invert(), expressionWindows)`.

- **Arguments:**
  - `condition`: `Windows`
- **Returns:** `Windows`

#### method `invert`

Transforms a `windows` object, inverting the windows. This is the complement of the object with the plan bounds.

- **Returns:** `Windows`

#### method `shorterThan`

Transforms a `windows` object, returning the windows whose duration is shorter than `duration`.

- **Arguments:**
  - `duration`: `long`
- **Returns:** `Windows`

#### method `longerThan`

Transforms a `windows` object, returning the windows whose duration is longer than `duration`.

- **Arguments:**
  - `duration`: `long`
- **Returns:** `Windows`

#### method `shiftBy`

Transforms a `windows` object, shifting the start and end of each window by a fixed positive or negative duration, respectively `fromStart` and `fromEnd`. Window [1,2] shifted by [-1,1] will become [0,3]. The same [1,2] shifted by [1,-2] will result in the `empty` window and disappear from the set of windows.

- **Arguments:**
  - `fromStart`: `long`
  - `fromEnd`: `long`
- **Returns:** `Windows`

#### method `violations`

Converts a `Windows` object into a list of violations, one for each interval that does _not_ have a window. This method is always optional, and only exists if you want to be explicit about converting a `Windows` object to a `Constraint`. If you don't call this method, it will be done for you.

- **Returns:** `Constraint`

---

### `Real`

A real number profile. This essentially represents a function from time to real numbers. You cannot sample or query the function from within the constraints API, but you can transform it and convert it into `Windows` when certain conditions are met.

#### constructor `Resource`

References the profile defined by a Real Resource. The string resource name is constrained to be a valid real resource name; hence the argument is `RealResourceName` instead of `string`. `RealResourceName` merely defines a small subset of all strings to be valid arguments.

- **Arguments:**
  - `name`: `RealResourceName` name of the resource to get the profile of
- **Returns:** `Real`

#### constructor `Value`

Creates a constant profile which is equal to the argument for all time.
This function is optional when used on the right side of an operator. For example, `Real.Resource("res").equal(Real.Value(3))` can be written as `Real.Resource("res").equal(3)`; but `Real.Value(3).equal(Real.Resource("res"))` _cannot_ be written `3.equal(Real.Resource("res"))`.

- **Arguments:**
  - `value`: Real number to build a profile from
- **Returns:** `Real`

#### method `plus`

Adds this real profile to another

- **Arguments:**
  - `other`: Either a `Real` or a `number`
- **Returns:** `Real`

#### method `plus`

Multiplies this real profile by a constant

- **Arguments:**
  - `multiplier`: `number`
- **Returns:** `Real`

#### method `plus`

Computes the derivative of the profile

- **Returns:** `Real`

#### method `lessThan`

Computes the windows when this is less than another real profile.

- **Arguments:**
  - `other`: Either a `Real` or a `number`
- **Returns:** Set of windows

#### method `lessThanOrEqual`

Computes the windows when this is less than or equal to another real profile.

- **Arguments:**
  - `other`: Either a `Real` or a `number`
- **Returns:** Set of windows

#### method `greaterThan`

Computes the windows when this is greater than another real profile.

- **Arguments:**
  - `other`: Either a `Real` or a `number`
- **Returns:** Set of windows

#### method `greaterThanOrEqual`

Computes the windows when this is greater than or equal to another real profile.

- **Arguments:**
  - `other`: Either a `Real` or a `number`
- **Returns:** Set of windows

#### method `changes`

Computes the instantaneous windows when the profile changes

- **Returns:** `Windows`

#### method `equal`

Computes the windows when this profile is equal to another

- **Arguments:**
  - `other`: `Real`
- **Returns:** `Windows`

#### method `notEqual`

Computes the windows when this profile is not equal to another

- **Arguments:**
  - `other`: `Real`
- **Returns:** `Windows`

---

### `Discrete<Schema>`

A discrete value profile, where `Schema` is the type of the value. This essentially represents a function from time to _any_ codomain (set of possible outputs), and the codomain is the `Schema` type. `Schema` is usually decided by the resource or activity parameter that you want to access, and the `Discrete` class imposes a type requirement that any comparisons must be between Discrete objects of the same `Schema` type.

You cannot sample or query the function from within the constraints API, but you can transform it and convert it into `Windows` when certain conditions are met.

#### constructor `Resource`

References the profile defined by a Discrete Resource. The string resource name is constrained to be a valid real resource name; hence the argument type is `R extends ResourceName` instead of `string`. `ResourceName` merely defines a small subset of all strings to be valid arguments. `DiscreteResourceSchema<R>` matches resource name to the discrete `Schema` (profile codomain).

- **Arguments:**
  - `name`: `R extends ResourceName`; name of the resource to get the profile of
- **Returns:** `Discrete<DiscreteResourceSchema<R>>`

#### constructor `Value`

Creates a constant profile which is equal to the argument for all time.
This function is optional when used on the right side of an operator. For example, `Discrete.Resource("res").equal(Discrete.Value("value"))` can be written as `Discrete.Resource("res").equal("value")`; but `Discrete.Value("value").equal(Discrete.Resource("res"))` _cannot_ be written `"value".equal(Discrete.Resource("res"))`.

- **Arguments:**
  - `value`: any type, which determines `Schema`; Any value that can be [serialized](../mission-modeling/activity-mappers.md#what-is-a-serializedvalue).
- **Returns:** `Discrete<Schema>`

#### method `transition`

Returns the set of instantaneous windows when the profile transitions from one specific state to another.

- **Arguments:**
  - `from`: instance of `Schema` representing the state the profile must transition from
  - `to`: instance of `Schema` representing the state the profile must transition from
- **Returns:** `Windows`

#### method `changes`

Computes the instantaneous windows when the profile changes

- **Returns:** `Windows`

#### method `equal`

Computes the windows when this profile is equal to another

- **Arguments:**
  - `other`: `Discrete<Schema>`
- **Returns:** `Windows`

#### method `notEqual`

Computes the windows when this profile is not equal to another

- **Arguments:**
  - `other`: `Discrete<Schema>`
- **Returns:** `Windows`
