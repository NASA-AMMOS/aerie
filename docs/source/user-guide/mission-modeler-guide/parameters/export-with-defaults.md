## Export With Defaults

Similarly to `@Template`, the `@WithDefaults` annotation also decouples parameter definitions and default values, allowing `record` types to be used as parent classes.
When the `@WithDefaults` annotation is used every parent class member variable is interpreted as a parameter to export to Merlin.
Unlike `@Template`, a sparse set of default values may be supplied.

This annotation must be attached to a nested `public static class` within the parent class.
Each member variable of this nested class must have the same name as a parent class's member variable.
Not every parent class member variable is required to have an associated member variable within the nested class.
This allows the mission modeler to selectively choose which parameters must be supplied by the planner.

### Example

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

### See Also

Aerie's [`examples/config-with-defaults`](https://github.com/NASA-AMMOS/aerie/blob/develop/examples/config-with-defaults/src/main/java/gov/nasa/jpl/aerie/configwithdefaults/Configuration.java) uses this style within its mission model configuration.
The [`examples/banananation`](https://github.com/NASA-AMMOS/aerie/blob/develop/examples/banananation) mission model also uses this style within [`BakeBananaBreadActivity`](https://github.com/NASA-AMMOS/aerie/blob/develop/examples/banananation/src/main/java/gov/nasa/jpl/aerie/banananation/activities/BakeBananaBreadActivity.java).

### Recommendation

**Use `@WithDefaults` when every member variable for a parent class should be an exported parameter with an optionally provided default value.**
