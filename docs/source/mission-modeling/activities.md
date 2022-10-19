# Activities
An activity is a modeled unit of mission system behavior to be utilized for simulations during planning. 
Activities emit events which can have various effects on modeled resources, and these effects can be 
modulated through input parameters. Activities can represent any part of the mission system behavior 
such as the availability of ground assets, or the execution of commands by the flight software or 
the instruments. In other words, activities in Merlin are entities whose role is to emit stimuli 
(events) to which the mission model reacts. Activities can therefore describe the relation: "when 
this activity occurs, this kind of thing should happen".

An activity type is a prototype for activity instances to be executed in a simulation. Activity 
types are defined by java classes that provide an `EffectModel` method to Merlin, along with a 
set of parameters. Each activity type exists in its own .java file, though activity types can 
be organized into hierarchical packages, for example as `gov.nasa.jpl.europa.clipper.gnc.TCMActivity`

Activity types consist of:
- metadata
- parameters that describe the range in execution and effects of the activity
- effect model that describes how the system will be perturbed when the activity is executed.

## Activity Annotation
In order for Merlin to detect an activity type, its class must be annotated with the `@ActivityType` tag. An activity type
is declared with its name using the following annotation:

```java
@ActivityType("TurnInstrumentOff")
```

By doing so, the Merlin annotation processor can discover all activity types declared in the mission model, and 
validate that activity type names are unique.

## Activity Metadata
Metadata of activities are structured such that the Merlin annotation processor can extract this metadata given 
particular keywords. Currently, the Merlin annotation processor recognizes the following tags: `contact, subsystem, brief_description, 
`and` verbose_description.`

These metadata tags are placed in a JavaDocs style comment block above the Activity Type to which they refer.
For example:

```java
/**
 * @subsystem Data
 * @contact mkumar
 * @brief_description A data management activity that deletes old files
 */
```

These tags are processed, at compile time, by the annotation processor to create documentation for the 
Activity types that are described in the mission model.

## Activity Parameters
Activity parameters provide the ability to modulate the behavior of an activity instance's effect model.
Aside from determinining the effects of the activity, these parameters can be used to determine its duration, 
decomposition into children activities and expansion into commands.

The Merlin annotation processor is used to extract and generate serialization code for parameters of activity types.
The annotation processor also allows authors of a mission model to create mission-specific parameter types, 
ensuring that they will be recognized by the Merlin framework.
See [Activity Parameters](activity-parameters.rst) for a thorough explanation 
of all possible styles of parameter declarations.
For more information on mission-specific parameter types, see [Value Mappers](activity-mappers.md#value-mappers).

## Validations

```{eval-rst}
See :ref:`Activity Parameters <validation>` for a thorough explanation of parameter validation.
```

## Activity Effect Model
Every activity type has an associated "effect model" that describes how that activity impacts mission resources. 
An effect model is a method on the activity type class annotated with `@EffectModel`; there can only be one such
method, and by convention it is named `run` that accepts the top-level `Mission` model as a parameter. This method
is invoked when the activity begins, paused when the activity waits a period of time, and resumes when that period
of time passes. The activity's computed duration will be measured from the instant this method is entered, and 
extends either until the method returns or until all spawned children of the activity have completed 
-- whichever is longer.

**Exceptions:**
If an uncaught exception is thrown from an activity's effect model, the simulation will halt. No later activities 
will be performed, and simulation time will not proceed beyond the instant at which the exception occurred. As such,
uncaught exceptions are essentially treated as fatal errors. We advise mission models to employ uncaught exceptions 
sparingly, as it deprives planners of information about the behavior of the spacecraft after the fault. (At least as
of 2021-06-11, an uncaught exception will cause the simulation to abort without producing *any* results -- not even 
up to the point of failure.) On the other hand, uncaught exceptions may be useful to identify bugs in the mission 
model or situations that the mission model is not intended to simulate.

**Actions:**
An activity's effect model may wait for time to pass, spawn other activities, and affect spacecraft state. 
Spacecraft state is affected via the `Mission` model parameter, which depends on the details of the modeled 
mission systems. (See [Developing a Mission Model](developing-a-mission-model) 
for more on mission modeling.)

Actions related to the passage of simulation time are provided as static methods on 
the `merlin.framework.ModelActions` class:

- `delay(duration)`: Delay the currently-running activity for the given duration. On resumption, it will observe
- effects caused by other activities over the intervening timespan.
- `waitFor(activityId)`: Delay the currently-running activity until the activity with specified ID has completed.
- On resumption, it will observe effects caused by other activities over the intervening timespan.
- `waitUntil(condition)`: Delay the currently-running activity until the provided `Condition` becomes true.
- On resumption, it will observe effects caused by other activities over the intervening timespan.
<!-- - `waitForChildren()`: Delay the currently-running activity until all child activities have completed. 
On resumption, it will observe effects caused by other activities over the intervening timespan. -->

Actions related to spawning other activities are provided by the generated `ActivityActions` class, 
usually found under the `generated` package within your codebase.

- `spawn(activity)`: Spawn a new activity as a child of the currently-running activity at the 
current point in time. The child will initially see any effects caused by its parent up to this point. 
The parent will continue execution uninterrupted, and will not initially see any effects caused by its child.
- `call(activity)`: Spawn a new activity as a child of the currently-running activity at the current 
point in time. The child will initially see any effects caused by its parent up to this point. The 
parent will halt execution until the child activity has completed.


For example, consider a simple activity for running the on-board heaters:

```java
@ActivityType("RunHeater")
public final class RunHeater {
    private static final int energyConsumptionRate = 1000;

    @Parameter
    public long durationInSeconds;

    @Validation("duration must be positive")
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

This activity first spawns a `PowerOnHeater` activity, which then continues concurrently with the current `RunHeater` activity.
Next, the total energy to be used by the heater is subtracted is subtracted from the remaining battery capacity 
(see [Models and Resources](models-and-resources)). The energy used 
depends on the duration parameter of the activity, allowing the activity's effect to be tuned by the planner. 
Next, the activity waits for the desired heater runtime to elapse, then spawns and waits for a `PowerOffHeater` 
activity. The activity completes after both children have completed.

### A Note about Decomposition
In Merlin mission models, decomposition of an activity is not an independent method, rather it is defined within 
the effect model by means of invoking child activities. These activities can be invoked using the `call()` method,
where the rest of the effect model waits for the child activity to complete; or using the `spawn()` method, where 
the effect model continues to execute without waiting for the child activity to complete. This method allows any 
arbitrary serial and parallel arrangement of child activities. This approach replaces duration estimate based wait
calls with event based waits. Hence, this allows for not keeping track of estimated durations of activities, while
also improving the readability of the activity procedure as a linear sequence of events.

## Computed Attributes
Upon the termination of an Activity, the effect model can optionally log some information that will be included 
with the simulation results. This information has no effect on the current simulation, but can be used by 
downstream tools that process the simulation results.

To specify the information that will be logged, the mission modeler must change the return type of the method 
marked with the @EffectModel annotation.

Let's consider the `RunHeater` example from above:
```java
@ActivityType("RunHeater")
public final class RunHeater {
    private static final int energyConsumptionRate = 1000;

    @Parameter
    public long durationInSeconds;

    @Validation("duration must be positive")
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
The effect model return type is "void", which means no useful information will be logged. Let's change 
that to `String` and add a log message:
```java
@EffectModel
public String run(final Mission mission) {
    spawn(new PowerOnHeater());

    final double totalEnergyUsed = durationInSeconds * energyConsumptionRate;
    mission.batteryCapacity.use(totalEnergyUsed);

    delay(durationInSeconds, Duration.SECONDS);

    call(new PowerOffHeater());

    return "This is a log message!"
}
```
Computed attributes are not limited to strings - they can be any type that merlin knows how to serialize. 
See [ValueMappers](activity-mappers.md#value-mappers) to learn about what types are supported out of the 
box, and how to define custom serializers for your own types.

Let's show how we can define computed attributes as a Map of Strings to Longs:
```java
@EffectModel
public Map<String, Long> run(final Mission mission) {
    spawn(new PowerOnHeater());

    final double totalEnergyUsed = durationInSeconds * energyConsumptionRate;
    mission.batteryCapacity.use(totalEnergyUsed);

    delay(durationInSeconds, Duration.SECONDS);

    call(new PowerOffHeater());

    return Map.of("duration_in_seconds", durationInSeconds,
                  "energy_consumption_rate", (Long) energyConsumptionRate);
}
```

(duration-types)=
## Duration Types

The scheduler (see [Scheduling Guide](../activity-plans/scheduling-guide) places 
activities in a plan to try achieving scheduling goals. 

As the effect model of an activity is not accessible to the scheduler, its duration can only be computed
by simulation. Satisfying temporal constraints associated with the scheduling of this activity ("activity
A must end before activity B ") may lead to multiple simulations and thus more computation time. 

However, it is possible to provide information about how the duration of an activity is determined to
help the scheduler.

 The duration of an activity is said to be: 
 -  **controllable** if it is ***only*** determined by one of the activity parameter.
 -  **uncontrollable** otherwise. 

By default, the duration of an activity is uncontrollable. To specify that the duration of an activity is
controllable, the `@ControllableDuration` annotation can be added to the effect model method such as in 
the example below:

```java
@ActivityType("RunHeater")
public final class RunHeater {
    private static final int energyConsumptionRate = 1000;

    @Parameter
    public long durationInSeconds;

    @Validation("duration must be positive")
    public boolean validateDuration() {
        return durationInSeconds > 0;
    }

    @EffectModel
    @ControllableDuration(parameterName = "durationInSeconds")
    public void run(final Mission mission) {
        spawn(new PowerOnHeater());

        final double totalEnergyUsed = durationInSeconds * energyConsumptionRate;
        mission.batteryCapacity.use(totalEnergyUsed);

        delay(durationInSeconds, Duration.SECONDS);
    }
}
```

Note that compared to the `RunHeater` activity present in the previous paragraphs, 
the `call(new PowerOffHeater())` at the end of the effect model has been removed because (1) 
it would have made the duration of the activity depend on the duration of the `PowerOffHeater` 
activity, (2) we assume `PowerOffHeater` has an uncontrollable duration.
Spawning another activity does not affect the duration of this activity as they run in parallel.

However, let's say `PowerOffHeater` also has a controllable duration that is passed through a 
parameter, we could write the following activity:

```java
@ActivityType("RunHeater")
public final class RunHeater {
    private static final int energyConsumptionRate = 1000;

    @Parameter
    public long durationInSeconds;

    @Validation("duration must be positive")
    public boolean validateDuration() {
        return durationInSeconds > 0;
    }

    @EffectModel
    @ControllableDuration(parameterName = "durationInSeconds")
    public void run(final Mission mission) {
        spawn(new PowerOnHeater());

        final double totalEnergyUsed = durationInSeconds * energyConsumptionRate;
        mission.batteryCapacity.use(totalEnergyUsed);
		
	durationPowerOff = 100L;
	durationPowerOn = durationInSeconds - durationPowerOff;
	
	delay(durationPowerOn, Duration.SECONDS);
        call(new PowerOffHeater(durationPowerOff))
    }
}
```

Even though the duration of `RunHeater` depends on the (fixed) duration of `PowerOffHeater` , we 
know its duration will be equal to `durationInSeconds`, making the activity effectively controllable. 

The annotation `@ControllableDuration(parameterName = "durationInSeconds")` has no other effect 
than to tell the scheduler that the duration of this activity can be controlled via 
the `durationInSeconds` parameter.  It acts like a contract between the mission model and the 
scheduler, ensuring that the duration of the activity will be equal to the duration specified 
by `durationInSeconds`. 

After being given this information, the scheduler considers it can control the duration of the
activity which will thus require less simulations, significantly improving scheduling performance
for this activity type. 
