## Export Parameter

The `@Parameter` annotation is the most explicit way to define a parameter and its defaults.
Explicitly declaring each parameter within a parent class with or without a default value gives the mission modeler the freedom to decide which member variables are parameters and which parameters are required by the planner.

### Example

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

### See Also

Aerie's `examples/foo-missionmodel` uses this style when declaring mission model configuration parameters.

### Recommendation

**Declare each parameter with a `@Parameter` when a non-`record` type parent class is desired.**

Some mission modelers may prefer the explicitness provided by individual `@Parameter` annotations.
However, this opens the door to subtle mistakes such as an unintentionally absent `@Parameter` annotation or an unintentionally absent default assignment.
Those who prefer a more data-oriented approach may also find this style to not be as ergonomic as using a simple `record` type.
