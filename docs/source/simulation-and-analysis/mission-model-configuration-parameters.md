# Mission Model Configuration Parameters
The Merlin interface offers a variety of ways to define **parameters** for mission model configurations and activities.
Parameters offer a concise way to export information across the mission-agnostic Merlin interface –
namely a parameter's type to support serialization and a parameter's "required" status to ensure that parameters without mission-model-defined defaults have an argument supplied by the planner.

In this guide **parent class** refers to the Java class that encapsulates parameters.
This class may take the form of either a mission model configuration or activity.

Both configurations and activities make use of the same Java annotations for declaring parameters within a parent class.
The `@Export` annotation interface serves as the common qualifier for exporting information across the mission-agnostic Merlin interface.
The following parameter annotations serve to assist with parameter declaration and validation:
- `@Export.Parameter`
- `@Export.Template`
- `@Export.WithDefaults`
- `@Export.Validation`

The following sections delve into each of these annotations along with examples and suggested use cases for each.

## Declaration

### Without Export Annotations

The first – and perhaps less obvious option – is to not use any parameter annotations.
If a parent class contains no `@Export.Parameter`, `@Export.Template`, or `@Export.WithDefaults` annotation it is assumed that every class member variable is a parameter to export to Merlin.

Defining a parent class becomes as simple as `public record Configuration(Integer a, Integer b, Integer c) { }`.
However, it is not possible to declare a member variable that is not an exported parameter with this approach.

#### Example

```java
@ActivityType("Simple")
public record SimpleActivity(Integer a, Integer b, Integer c) {
    
  @EffectModel
  public void run(final Mission mission) {
    mission.count.set(a);
    delay(1, SECOND);
    mission.count.set(b);
    delay(1, SECOND);
    mission.count.set(c);
  }
}
```

In the above example `a`, `b`, and `c` are all parameters that require planner-provided arguments at planning time.

#### See Also

Aerie's `examples/config-without-defaults` makes use of this succinct style for declaring mission model configuration parameters.

For more information on records in Java 16+, see [Java Record Classes](https://docs.oracle.com/en/java/javase/16/language/records.html). 

#### Recommendation

**Avoid `@Export.Parameter`, `@Export.Template`, or `@Export.WithDefaults` when every member variable for a parent class should be an exported parameter without a default value.**

This approach is great for defining a simple `record` type parent class without defaults.
If defaults are desired then `@Template` or `@WithDefaults` can be used without changing a `record` type class definition.

### `@Export.Parameter`

The `@Parameter` annotation is the most explicit way to define a parameter and its defaults.
Explicitly declaring each parameter within a parent class with or without a default value gives the mission modeler the freedom to decide which member variables are parameters and which parameters are required by the planner.

#### Example

```java
public final class Configuration {

  public Integer a;

  @Export.Parameter
  public Integer b;

  @Export.Parameter
  public Integer c = 3;

  public Configuration(final Integer a, final Integer b, final Integer c) {
    this.a = a;
    this.b = b;
    this.c = c;
  }
}
```

In the above example the parent class is a mission model configuration. Here's a close look at each member variable:
- `a`: a traditional member variable. Not explicitly declared as a parameter and therefore is not considered to be a parameter.
- `b`: explicitly declared as a parameter without a default value. A value will be required by the planner.
- `c`: explicitly declared as a parameter with a default value. A value will not be required by the planner.

#### See Also

Aerie's `examples/foo-missionmodel` uses this style when declaring mission model configuration parameters.

#### Recommendation

**Declare each parameter with a `@Parameter` when a non-`record` type parent class is desired.**

Some mission modelers may prefer the explicitness provided by individual `@Parameter` annotations.
However, this opens the door to subtle mistakes such as an unintentionally absent `@Parameter` annotation or an unintentionally absent default assignment.
Those who prefer a more data-oriented approach may also find this style to not be as ergonomic as using a simple `record` type.

### `@Export.Template`

The `@Template` annotation decouples parameter definitions and default values, allowing `record` types to be used as parent classes.
When the `@Template` annotation is used every parent class member variable is interpreted as a parameter to export to Merlin.
This annotation must be attached to a `public static` constructor method.

#### Example

```java
@ActivityType("ThrowBanana")
public record ThrowBananaActivity(double speed) {

  @Template
  public static ThrowBananaActivity defaults() {
    return new ThrowBananaActivity(1.0);
  }

  @Validation("Speed must be positive")
  public boolean validateBiteSize() {
    return this.speed() > 0;
  }

  @EffectModel
  public void run(final Mission mission) {
    mission.plant.add(-1);
  }
}
```

In the above example `ThrowBananaActivity` is a `record` type with one constructor parameter, `speed`.

#### See Also

Aerie's `examples/banananation` uses this style within `GrowBananaActivity` and `ThrowBananaActivity`.

#### Recommendation

**Use `@Template` when every member variable for a parent class should be an exported parameter with a default value.**

### `@WithDefaults`

Similarly to `@Template`, `@WithDefaults` annotation also decouples parameter definitions and default values, allowing `record` types to be used as parent classes.
When the `@WithDefaults` annotation is used every parent class member variable is interpreted as a parameter to export to Merlin.
Unlike `@Template`, a sparse set of default values may be supplied.

This annotation must be attached to a nested `public static class` within the parent class.
Each member variable of this nested class must have the same name as a parent class's member variable.
Not every parent class member variable is required to have an associated member variable within the nested class.
This allows the mission modeler to selectively choose which parameters must be supplied by the planner.

#### Example

```java
public record Configuration(Integer a, Double b, String c) {

  @WithDefaults
  public static final class Defaults {
    public static Integer a = 42;
    public static String c = "JPL";
  }
}
```

In the above example the parent class is a mission model configuration. Here's a close look at each member variable:
- `a`: a parameter with an associated default value.
- `b`: a parameter without a default value. A value will be required by the planner.
- `c`: a parameter with an associated default value.

#### See Also

Aerie's `examples/config-with-defaults` uses this style within its mission model configuration.
The `examples/banananation` mission model also uses this style within `BakeBananaBreadActivity`.

#### Recommendation

**Use `@WithDefaults` when every member variable for a parent class should be an exported parameter with an optionally provided default value.**

## Validation

A mission model configuration or activity instance can be validated by providing one or more methods annotated by `@Validation`.
The annotation message specifies the message to present to a planner when the validation fails. For example:

```java
@Validation("instrument power must be between 0.0 and 1000.0")
public boolean validateInstrumentPower() {
  return (instrumentPower_W >= 0.0) && (instrumentPower_W <= 1000.0);
}
```

The Merlin annotation processor identifies these methods and arranges for them to be invoked whenever the planner instantiates an instance of the class.
A message will be provided to the planner for each failing validation, so the order of validation methods does not matter.
