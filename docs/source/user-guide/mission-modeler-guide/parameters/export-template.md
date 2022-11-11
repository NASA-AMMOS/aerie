## Export Template

The `@Template` annotation decouples parameter definitions and default values, allowing `record` types to be used as parent classes.
When the `@Template` annotation is used every parent class member variable is interpreted as a parameter to export to Merlin.
This annotation must be attached to a `public static` constructor method.

### Example

```java
@ActivityType("ThrowBanana")
public record ThrowBananaActivity(double speed) {

  @Template
  public static ThrowBananaActivity defaults() {
    return new ThrowBananaActivity(1.0);
  }

  @Validation("Speed must be positive")
  @Validation.Subject("speed")
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

### See Also

Aerie's `examples/banananation` uses this style within `GrowBananaActivity` and `ThrowBananaActivity`.

### Recommendation

**Use `@Template` when every member variable for a parent class should be an exported parameter with a default value.**
