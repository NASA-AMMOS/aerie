## Declaring a Mission Model Configuration

To use a mission model configuration the `@WithConfiguration` annotation must be
used within the mission model's `package-info.java` to register the configuration with Merlin.

For example, `examples/banananation`'s [`package-info.java`](https://github.com/NASA-AMMOS/aerie/blob/develop/examples/banananation/src/main/java/gov/nasa/jpl/aerie/banananation/package-info.java)
makes use of this annotation:
```java
@MissionModel(model = Mission.class)

@WithConfiguration(Configuration.class)
```

In this example `Configuration` is the class class containing all mission model configuration data.
When the `@WithConfiguration` annotation is used, the model – defined within the `@MissionModel` annotation – must accept the configuration as the last constructor parameter.
See `Mission.java`:

```java
public Mission(final Registrar registrar, final Configuration config) {
  // ...
}
```

If the second argument is an `Instant` denoting plan start time the configuration parameter must still be provided as the last argument.
For example:

```java
public Mission(final Registrar registrar, final Instant planStart, final Configuration config) {
  // ...
}
```
