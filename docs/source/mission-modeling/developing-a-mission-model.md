# Developing A Mission Model
A mission model defines the behavior of any measurable mission resources, and a set of **activity types**, defining the ways in which a plan may influence mission resources. 

## Mission Modeling Java Libraries

Aerie provides seven libraries that can be imported by a project. They are:
* [contrib](https://github.com/NASA-AMMOS/aerie/packages/1171107)
* [merlin-framework](https://github.com/NASA-AMMOS/aerie/packages/1171109)
* [merlin-framework-processor](https://github.com/NASA-AMMOS/aerie/packages/1171111)
* [merlin-framework-junit](https://github.com/NASA-AMMOS/aerie/packages/1171110)
* [merlin-sdk](https://github.com/NASA-AMMOS/aerie/packages/1171112)
* [merlin-driver](https://github.com/NASA-AMMOS/aerie/packages/1171108)
* [parsing-utilities](https://github.com/NASA-AMMOS/aerie/packages/1171113)

## Package-info File
A mission model must contain, at the very least, a `package-info.java` containing annotations that describe the highest-level features of the mission model. For example:

```java
// examples/banananation/package-info.java
@MissionModel(model = Mission.class)
@WithActivityType(BiteBananaActivity.class)
@WithActivityType(PeelBananaActivity.class)
@WithActivityType(ParameterTestActivity.class)
@WithMappers(BasicValueMappers.class)
package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.banananation.activities.BiteBananaActivity;
import gov.nasa.jpl.aerie.banananation.activities.ParameterTestActivity;
import gov.nasa.jpl.aerie.banananation.activities.PeelBananaActivity;
import gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel.WithActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel.WithMappers;
```

This `package-info.java` identifies the top-level class representing the mission model, and registers activity types that may interact with the mission model. Merlin processes these annotations at compile-time, generating a set of boilerplate classes which take care of interacting with the Aerie platform.

The `@WithMappers` annotation informs the annotation processor of a set of serialization rules for activity 
parameters of various types; the [`BasicValueMappers`](https://github.com/NASA-AMMOS/aerie/blob/develop/contrib/src/main/java/gov/nasa/jpl/aerie/contrib/serialization/rulesets/BasicValueMappers.java) 
ruleset covers most primitive Java types. Mission modelers may also create their own rulesets, specifying 
rules for mapping custom value types. If multiple mapper classes are included via the `@WithMappers` annotations,
and multiple mappers declare a mapping rule to the same data type, the rule found in the earlier declared mapper 
will take precedence. For more information on allowing custom values, see [value mappers](activity-mappers.md#value-mappers).


## Mission Model Class
The top-level mission model is responsible for defining all of the mission resources and their behavior when 
affected by activities. Of course, the top-level model may delegate to smaller, more focused models based 
on the needs of the mission. The top-level model is received by activities, however, so it must make accessible
any resources or methods to be used therein.

```java
// examples/banananation/Mission.java
public class Mission {
  public final AdditiveRegister fruit = AdditiveRegister.create(4.0);
  public final AdditiveRegister peel = AdditiveRegister.create(4.0);
  public final Register<Flag> flag = Register.create(Flag.A);

  public Mission(final Registrar registrar) {
    registrar.discrete("/flag", this.flag, new EnumValueMapper<>(Flag.class));
    registrar.real("/peel", this.peel);
    registrar.real("/fruit", this.fruit);
  }
}
```

Mission resources are declared using [`Registrar#discrete`](https://github.jpl.nasa.gov/pages/Aerie/aerie/latest/javadoc/framework/gov/nasa/jpl/aerie/merlin/framework/Registrar.html#discrete(java.lang.String,gov.nasa.jpl.aerie.merlin.framework.Resource,gov.nasa.jpl.aerie.merlin.protocol.ValueMapper)) or [`Registrar#real`](https://github.jpl.nasa.gov/pages/Aerie/aerie/latest/javadoc/framework/gov/nasa/jpl/aerie/merlin/framework/Registrar.html#real(java.lang.String,gov.nasa.jpl.aerie.merlin.framework.Resource)).

A model may also express autonomous behaviors, where a discrete change occurs in the system outside of an 
activity's effects. A **daemon task** can be used to model these behaviors. Daemons are spawned at the 
beginning of any simulation, and may perform the same effects as an activity. Daemons are prepared using 
the [`spawn`](https://github.jpl.nasa.gov/pages/Aerie/aerie/latest/javadoc/framework/gov/nasa/jpl/aerie/merlin/framework/ModelActions.html#spawn(java.lang.Runnable)) method.

## Activity types
An **activity type** defines a simulated behavior that may be invoked by a planner, separate from the 
autonomous behavior of the mission model itself. Activity types may define **parameters**, which are 
filled with **arguments** by a planner and provided to the activity upon execution. Activity types may 
also define **validations** for the purpose of informing a planner when the parameters they have 
provided may be problematic.

```java
// examples/banananation/activities/PeelBananaActivity.java
@ActivityType("PeelBanana")
public final class PeelBananaActivity {
  private static final double MASHED_BANANA_AMOUNT = 1.0;

  @Parameter
  public String peelDirection = "fromStem";

  @Validation("peel direction must be fromStem or fromTip")
  public boolean validatePeelDirection() {
    return List.of("fromStem", "fromTip").contains(this.peelDirection);
  }

  @EffectModel
  public void run(final Mission mission) {
    if (peelDirection.equals("fromStem")) {
      mission.fruit.subtract(MASHED_BANANA_AMOUNT);
    }
    mission.peel.subtract(1.0);
  }
}
```

Merlin automatically generates parameter serialization boilerplate for every activity type defined in 
the mission model's `package-info.java`. Moreover, the generated `Model` base class provides helper 
methods for spawning each type of activity as children from other activities.

## Uploading a Mission Model
In order to use a mission model to simulate a plan on the Aerie platform, it must be packaged as a 
JAR file with all of its non-Merlin dependencies bundled in. The [template mission model](https://github.com/NASA-AMMOS/mission-model-template) 
provides this capability out of the box, so long as your dependencies are specified 
with Gradle's `implementation` dependency class. The built mission model JAR can be uploaded to 
Aerie through the Aerie web UI.
