# Effect Model
Every activity type has an associated "effect model" that describes how that activity impacts mission resources. An effect model is a method on the activity type class annotated with `@EffectModel`; there can only be one such method, and by convention it is named `run` that accepts the top-level `Mission` model as a parameter. This method is invoked when the activity begins, paused when the activity waits a period of time, and resumes when that period of time passes. The activity's computed duration will be measured from the instant this method is entered, and extends either until the method returns or until all spawned children of the activity have completed -- whichever is longer.

## Exceptions
If an uncaught exception is thrown from an activity's effect model, the simulation will halt. No later activities will be performed, and simulation time will not proceed beyond the instant at which the exception occurred. As such, uncaught exceptions are essentially treated as fatal errors. We advise mission models to employ uncaught exceptions sparingly, as it deprives planners of information about the behavior of the spacecraft after the fault. An uncaught exception will cause the simulation to abort without producing *any* results -- not even up to the point of failure. On the other hand, uncaught exceptions may be useful to identify bugs in the mission model or situations that the mission model is not intended to simulate.

## Actions
An activity's effect model may wait for time to pass, spawn other activities, and affect spacecraft state. Spacecraft state is affected via the `Mission` model parameter, which depends on the details of the modeled mission systems.

Actions related to the passage of simulation time are provided as static methods on the `merlin.framework.ModelActions` class:

- `delay(duration)`: Delay the currently-running activity for the given duration. On resumption, it will observe effects caused by other activities over the intervening timespan.
- `waitUntil(condition)`: Delay the currently-running activity until the provided `Condition` becomes true. On resumption, it will observe effects caused by other activities over the intervening timespan.

Actions related to spawning other activities are provided by the generated `ActivityActions` class, usually found under the `generated` package within your codebase.

- `spawn(activity)`: Spawn a new activity as a child of the currently-running activity at the current point in time. The child will initially see any effects caused by its parent up to this point. The parent will continue execution uninterrupted, and will not initially see any effects caused by its child.
- `call(activity)`: Spawn a new activity as a child of the currently-running activity at the current point in time. The child will initially see any effects caused by its parent up to this point. The parent will halt execution until the child activity has completed.


For example, consider a simple activity for running the on-board heaters:

```java
@ActivityType("RunHeater")
public final class RunHeater {
    private static final int energyConsumptionRate = 1000;

    @Parameter
    public long durationInSeconds = 0;

    @Validation("duration must be positive")
    @Validation.Subject("durationInSeconds")
    public boolean validateDuration() {
        return durationInSeconds > 0;
    }

    @EffectModel
    public void run(final Mission mission) {
        spawn(new PowerOnHeater());

        final double totalEnergyUsed = durationInSeconds * energyConsumptionRate;
        mission.batteryCapacity.use(totalEnergyUsed);

        delay(durationInSeconds, Duration.SECONDS);

        call(new PowerOffHeater());
    }
}
```

This activity first spawns a `PowerOnHeater` activity, which then continues concurrently with the current `RunHeater` activity. Next, the total energy to be used by the heater is subtracted from the remaining battery capacity (see [Resources and Models](../resources-and-models/index)). The energy used depends on the duration parameter of the activity, allowing the activity's effect to be tuned by the planner. Next, the activity waits for the desired heater runtime to elapse, then spawns and waits for a `PowerOffHeater` activity. The activity completes after both children have completed.

## Activity Decomposition
In Merlin mission models, decomposition of an activity is not an independent method, rather it is defined within the effect model by means of invoking child activities. These activities can be invoked using the `call()` method, where the rest of the effect model waits for the child activity to complete; or using the `spawn()` method, where the effect model continues to execute without waiting for the child activity to complete. This method allows any arbitrary serial and parallel arrangement of child activities. This approach replaces duration estimate based wait calls with event based waits. Hence, this allows for not keeping track of estimated durations of activities, while also improving the readability of the activity procedure as a linear sequence of events.

## Computed Attributes
Upon the termination of an Activity, the effect model can optionally log some information that will be included with the simulation results. This information has no effect on the current simulation, but can be used by downstream tools that process the simulation results.

To specify the information that will be logged, the mission modeler must change the return type of the method marked with the @EffectModel annotation.

Let's consider the `RunHeater` example from above:
```java
@ActivityType("RunHeater")
public final class RunHeater {
    private static final int energyConsumptionRate = 1000;

    @Parameter
    public long durationInSeconds;

    @Validation("duration must be positive")
    @Validation.Subject({"durationInSeconds"})
    public boolean validateDuration() {
        return durationInSeconds > 0;
    }

    @EffectModel
    public void run(final Mission mission) {
        spawn(new PowerOnHeater());

        final double totalEnergyUsed = durationInSeconds * energyConsumptionRate;
        mission.batteryCapacity.use(totalEnergyUsed);

        delay(durationInSeconds, Duration.SECONDS);

        call(new PowerOffHeater());
    }
}
```

The effect model return type is"void", which means no useful
information will be logged. Let's define a record type to house our
computed attributes, and update the effect model to return it:

```java
@AutoValueMapper.Record
record ComputedAttributes(
  long durationInSeconds;
  int energyConsumptionRate;
) {}

@EffectModel
public ComputedAttributes run(final Mission mission) {
    spawn(new PowerOnHeater());

    final double totalEnergyUsed = durationInSeconds * energyConsumptionRate;
    mission.batteryCapacity.use(totalEnergyUsed);

    delay(durationInSeconds, Duration.SECONDS);

    call(new PowerOffHeater());

    return new ComputedAttributes(durationInSeconds, energyConsumptionRate);
}
```

The `@AutoValueMapper.Record` annotation exists to make this pattern
more convenient - it only applies to records returned from effect
models.

Computed attributes are not limited to records - they can be any type
that merlin knows how to serialize. See [ValueMappers](../custom-value-mappers/index)
to learn about what types aresupported out of the box, and how to define custom
serializers for your own types. Here is an example where the effect model returns
a list of strings, instead of a record:

```java
@EffectModel
public List<String> run(final Mission mission) {
    final var logs = new ArrayList<String>();

    spawn(new PowerOnHeater());
    logs.add("Spawned PowerOnHeater");

    final double totalEnergyUsed = durationInSeconds * energyConsumptionRate;
    mission.batteryCapacity.use(totalEnergyUsed);
    logs.add("Total energy used: " + totalEnergyUsed);

    logs.add("Delaying for " + durationInSeconds + " seconds");
    delay(durationInSeconds, Duration.SECONDS);

    call(new PowerOffHeater());

    return logs;
}
```
