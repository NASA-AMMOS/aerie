## Examples

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

See [`examples/`](https://github.com/NASA-AMMOS/aerie/tree/develop/examples) for a demonstration of each possible style of configuration definitions:
- [`examples/foo-missionmodel`](https://github.com/NASA-AMMOS/aerie/blob/develop/examples/foo-missionmodel/src/main/java/gov/nasa/jpl/aerie/foomissionmodel/Configuration.java): uses standard `@Parameter` configuration annotations.
- [`examples/banananation`](https://github.com/NASA-AMMOS/aerie/blob/develop/examples/banananation/src/main/java/gov/nasa/jpl/aerie/banananation/Configuration.java): (shown above) uses the `@Template` annotation to define a default `Configuration` object.
- [`examples/config-with-defaults`](https://github.com/NASA-AMMOS/aerie/blob/develop/examples/config-with-defaults/src/main/java/gov/nasa/jpl/aerie/configwithdefaults/Configuration.java): uses `@WithDefaults` to define a default for each parameter.
- [`examples/config-without-defaults`](https://github.com/NASA-AMMOS/aerie/blob/develop/examples/config-without-defaults/src/main/java/gov/nasa/jpl/aerie/configwithoutdefaults/Configuration.java): defined with no default arguments, requires all arguments to be supplied by the planner.

### Use

The mission model may use a configuration to set initial values, for example:

```java
this.sink = new Accumulator(0.0, config.sinkRate);
```
