# Configuring a Mission Model

A **mission model configuration** enables mission modelers to set initial mission model values when running a simulation.
Configurations are tied to a plan, therefore each plan is able to define its own set of configuration parameters.
The `examples/banananation` project contains a `Configuration` data class example to demonstrate how a simple configuration may be created.

## Setup

### Mission Model

The `examples/banananation` project makes use of the `@WithConfiguration` annotation within `package-info.java`:

```java
@MissionModel(model = Mission.class)

@WithConfiguration(Configuration.class)
```

When the `@WithConfiguration` annotation is used, the model – defined within the `@MissionModel` annotation – must accept the configuration as a constructor argument.
See `Mission.java`:

```java
public Mission(final Registrar registrar, final Configuration config) {
  // ...
}
```

### Configuration

A configuration class should be defined with the same parameter annotations as activities.
See [Activity Parameters](activity-parameters.rst) for a thorough explanation of all possible styles of `@Export` parameter declaration and validation.

Similarly to activities, the Merlin annotation processor will take care of all serialization/deserialization of the configuration object.
The Merlin annotation processor will generate a configuration mapper for the configuration defined within `@WithConfiguration()`.

#### Examples

`Configuration` can be a simple data class. For example:

```java
package gov.nasa.jpl.aerie.banananation;

import java.nio.file.Path;

import static gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Template;

public record Configuration(int initialPlantCount, String initialProducer, Path initialDataPath) {

  public static final int DEFAULT_PLANT_COUNT = 200;
  public static final String DEFAULT_PRODUCER = "Chiquita";
  public static final Path DEFAULT_DATA_PATH = Path.of("/etc/os-release");

  public static @Template Configuration defaultConfiguration() {
    return new Configuration(DEFAULT_PLANT_COUNT, DEFAULT_PRODUCER, DEFAULT_DATA_PATH);
  }
}
```

See `examples/` for a demonstration of each possible style of configuration definitions:
- `examples/foo-missionmodel`: uses standard `@Parameter` configuration annotations.
- `examples/banananation`: (shown above) uses the `@Template` annotation to define a default `Configuration` object.
- `examples/config-with-defaults`: uses `@WithDefaults` to define a default for each parameter.
- `examples/config-without-defaults`: defined with no default arguments, requires all arguments to be supplied by the planner.

## Use

The mission model may use a configuration to set initial values, for example:

```java
this.sink = new Accumulator(0.0, config.sinkRate);
```
