## Without Export Annotations

The first – and perhaps less obvious option – is to not use any parameter annotations.
If a parent class contains no `@Export.Parameter`, `@Export.Template`, or `@Export.WithDefaults` annotation it is assumed that every class member variable is a parameter to export to Merlin.

Defining a parent class becomes as simple as `public record Configuration(Integer a, Integer b, Integer c) { }`.
However, it is not possible to declare a member variable that is not an exported parameter with this approach.

### Example

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

### See Also

Aerie's `examples/config-without-defaults` makes use of this succinct style for declaring mission model configuration parameters.

For more information on records in Java 16+, see [Java Record Classes](https://docs.oracle.com/en/java/javase/16/language/records.html). 

### Recommendation

**Avoid `@Export.Parameter`, `@Export.Template`, or `@Export.WithDefaults` when every member variable for a parent class should be an exported parameter without a default value.**

This approach is great for defining a simple `record` type parent class without defaults.
If defaults are desired then `@Template` or `@WithDefaults` can be used without changing a `record` type class definition.
