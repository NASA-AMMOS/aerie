# Activity Parameters

Activity parameters provide the ability to modulate the behavior of an activity instance's effect model.
Aside from determinining the effects of the activity, these parameters can be used to determine its duration, 
decomposition into children activities and expansion into commands.

The Merlin annotation processor is used to extract and generate serialization code for parameters of activity types.
The annotation processor also allows authors of a mission model to create mission-specific parameter types, 
ensuring that they will be recognized by the Merlin framework.
See [Advanced Topic: Parameters](../parameters/index) for a thorough explanation 
of all possible styles of parameter declarations.
For more information on mission-specific parameter types, see [Value Mappers](../custom-value-mappers/index).

## Parameter Validations

A mission model configuration or activity instance can be validated by providing one or more methods annotated by `@Validation` and `@Validation.Subject`
The `@Validation` annotation message specifies the message to present to a planner when the validation fails.
The `@Validation.Subject` annotation specifies which parameter(s) the validation is associated with.
These associated parameters are reported from Merlin when querying for activity argument validations to couple validations with parameters.

For example:

```java
@Validation("instrument power must be between 0.0 and 1000.0")
@Validation.Subject("instrumentPower_W")
public boolean validateInstrumentPower() {
  return (instrumentPower_W >= 0.0) && (instrumentPower_W <= 1000.0);
}
```

To associate a validation with multiple parameters `{"...", "...", ...}` syntax may be used within the `@Validation.Subject` annotation.
For example:

```java
@Validation("instrument powers must be between 0.0 and 1000.0")
@Validation.Subject({"instrumentPower_W", "instrumentPower_Y"})
public boolean validateInstrumentPowers() {
  return (instrumentPower_W >= 0.0) && (instrumentPower_W <= 1000.0) &&
      (instrumentPower_Y >= 0.0) && (instrumentPower_Y <= 1000.0) &&;
}
```

The Merlin annotation processor identifies these methods and arranges for them to be invoked whenever the planner instantiates an instance of the class.
A message will be provided to the planner for each failing validation, so the order of validation methods does not matter.

## Duration Types

The scheduler (see [Scheduling Guide](../../ui-api-guide/scheduling/index) places
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
