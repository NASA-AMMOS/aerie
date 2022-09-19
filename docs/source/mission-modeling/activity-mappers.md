# Activity Mappers

An Activity Mapper is a Java class that implements the `ActivityMapper`
interface for the `ActivityType` being mapped. It is required that
each Activity Type in an mission model have an associated Activity Mapper, to provide
provide several capabilities surrounding serialization/deserialization of activity instances.

The Merlin annotation processor can automatically [generate activity mappers](https://github.com/NASA-AMMOS/aerie/wiki/Activity-Mappers#generated-activity-mappers) for every activity type, even for those with custom-typed parameters, but if it is desirable to create a custom activity mapper the interface is described below. 

## `ActivityMapper` Interface

The `ActivityMapper` interface is shown below:

```java
public interface ActivityMapper<Instance> {
  String getName();
  Map<String, ValueSchema> getParameters();
  Map<String, SerializedValue> getArguments(Instance activity);

  Instance instantiateDefault();
  Instance instantiate(Map<String, SerializedValue> arguments) throws TaskSpecType.UnconstructableTaskSpecException;

  List<String> getValidationFailures(Instance activity);
}
```

The first thing to notice is that the interface takes a type parameter (here called `Instance`). When implementing the `ActivityMapper` interface, an activity mapper must supply the `ActivityType` being mapped. With that in mind, each of the methods shown must be implemented as such:

- `getName()` returns the name of the activity type being mapped
- `getParameters()` provides the named parameter fields of the activity along with their corresponding `ValueSchema`, that describes their structure
- `getArguments(Instance activity)` provides the actual values for each parameter from a provided activity instance
- `instantiateDefault()` creates a default instance of the activity type without any values provided externally
- `instantiate(Map<String, SerializedValue> arguments)` constructs an instance of the activity type from a the provided arguments, if possible
- `getValidationFailures(Instance activity)` provides a list of reasons a constructed activity is invalid, if any.
Note that validation failures are different from instantiation errors. Validation failures occur when a constructed activity instance's parameters are outside acceptable range.

The `getParameters()` method returns a `Map<String, ValueSchema>`. In this map should be a key for every parameter, with a `ValueSchema` describing the structure of that parameter. See our [Value Schema documentation](https://github.com/NASA-AMMOS/aerie/wiki/Value-Schemas#value-schemas-from-code) for more information on creating value schemas.

## Generated Activity Mappers

In most cases, you will likely want to let Merlin generate activity mappers for you. Thankfully, this is the done automatically when running the Merlin Annotation Processor. When compiling your code with the Merlin annotation processor, the processor will produce an activity mapper for each activity type. This is made possible by the use of the `@WithMappers()` annotations in your [package-info.java](https://github.com/NASA-AMMOS/aerie/wiki/Developing-a-Mission-Model#package-infojava). Each java-file specified by these annotations is parsed to determine what types of values can be mapped. As long as there is a mapper for each activity parameter type used in the model, the annotation processor should have no issues creating activity mappers.

## Value Mappers

Regardless of whether you create custom activity mappers or let Merlin
generate them for you, you will likely find the need to work with a
`ValueMapper` at some point. In fact, generating activity mappers is made
quite simple by considering the fact that an activity instance is wholly
defined by its parameter values.

You may find yourself asking "Just what _is_ a value mapper?" A value mapper
is a small, focused class whose sole responsibility is to tell Merlin how to
handle a specific type of value. Value mappers allow all sorts of capabilities
from custom-typed activity parameters to custom-typed resources.

One of the most convenient things about using value mappers is the fact that
Merlin comes with them already defined for all basic types.
Furthermore, value mappers for combinations of types can easily
be created by passing one `ValueMapper` into another during instantiation.

Although we provide value mappers for basic types, it is
entirely acceptable to create custom value mappers for other types,
such as those imported from external libraries.
This can be done by writing a Java class which implements the
`ValueMapper` interface. Below is a value mapper for an apache `Vector3D` type as an example:

```java
public class Vector3DValueMapper implements ValueMapper<Vector3D> {

  @Override
  public ValueSchema getValueSchema() {
    return ValueSchema.ofSequence(ValueSchema.REAL);
  }

  @Override
  public Result<Vector3D, String> deserializeValue(final SerializedValue serializedValue) {
    return serializedValue
        .asList()
        .map(Result::<List<SerializedValue>, String>success)
        .orElseGet(() -> Result.failure("Expected list, got " + serializedValue.toString()))
        .match(
            serializedElements -> {
              if (serializedElements.size() != 3) return Result.failure("Expected 3 components, got " + serializedElements.size());
              final var components = new double[3];
              final var mapper = new DoubleValueMapper();
              for (int i=0; i<3; i++) {
                final var result = mapper.deserializeValue(serializedElements.get(i));
                if (result.getKind() == Result.Kind.Failure) return result.mapSuccess(_left -> null);

                // SAFETY: `result` must be a Success variant.
                components[i] = result.getSuccessOrThrow();
              }
              return Result.success(new Vector3D(components));
            },
            Result::failure
        );
  }

  @Override
  public SerializedValue serializeValue(final Vector3D value) {
    return SerializedValue.of(
        List.of(
            SerializedValue.of(value.getX()),
            SerializedValue.of(value.getY()),
            SerializedValue.of(value.getZ())
        )
    );
  }
}
```

Notice there are just 3 methods to implement for a `ValueMapper`.
The first is `getValueSchema()`, which should return a `ValueSchema`
describing the structure of the value being mapped (see [here](https://github.com/NASA-AMMOS/aerie/wiki/Value-Schemas) for more info)

The next two methods are inverses of each other: `deserializeValue()` and `serializeValue()`. It is the job of `deserializeValue()` to take a `SerializedValue` and map it, if possible, into the mapper's supported value. Meanwhile, `serializeValue()` takes an instance of the mapper's supported value and turns it into a [`SerializedValue`](https://github.com/NASA-AMMOS/aerie/wiki/Activity-Mappers#what-is-a-serializedvalue).

There are plenty of examples of value mappers over in the [contrib module](https://github.com/NASA-AMMOS/aerie/tree/develop/contrib/src/main/java/gov/nasa/jpl/aerie/contrib/serialization/mappers).

### Registering Value Mappers

As mentioned above, the `@WithMappers()` annotation is used to register value mappers for a mission model.
Value mappers are expected to be defined with static constructor methods within classes listed in `@WithMappers()` annotations.
For example, if `package-info.java` contains:
```java
@WithMappers(BananaValueMappers.class)
```
Then the value mapper may define a custom `Configuration` value mapper with:
```java
public final class BananaValueMappers {
  public static ValueMapper<Configuration> configuration() {
    return new ConfigurationValueMapper();
  }
}
```

Value mappers may be created for types that use parameterized types, but the parameterized types themselves must be either unbounded bounded or `Enum<>`.
For example:
```java
@Parameter
public List<? extends Foo> test;
```
or
```java
@Parameter
public List<? extends Map<? super Foo, ? extends Bar>> test;
```
are not trivially resolved to a single value mapper due to the type constraints at play here.

## What is a SerializedValue

When working with a `ValueMapper` it is inevitable that you will come across the `SerializedValue` type. This is the type we use for serializing all values that need serialization, such as activity parameters and resource values. In crafting a value mapper, you will have to both create a `SerializedValue` and parse one.

Constructing a `SerializedValue` tends to be more straightforward, because there are no questions about the structure of the value you are starting with. For basic types, you need only call `SerializedValue.of(value)`
and the `SerializedValue` class will handle the rest. This can be done for values of the following types: `long`, `double`, `String`, `boolean`. Note that integers and floats can be represented by `long` and `double` respectively. For more complex types, you can also provide a `List<SerializedValue>` or `Map<String, SerializedValue>` to `SerializedValue.of()`. It is clear that these can be used to serialize lists and maps themselves, but arbitrarily complex structures can be serialized in this way. Consider the following examples:

```java
int exInt = 5;
SerializedValue serializedInt = SerializedValue.of(exInt);

List<String> exList = List.of("a", "b", "c")
SerializedValue serializedList = SerializedValue.of(
                                   List.of(
                                     SerializedValue.of(exList.get(0)),
                                     SerializedValue.of(exList.get(1)),
                                     SerializedValue.of(exList.get(2))
                                   )
                                 );

Map<String, Boolean> exMap = Map.of(
  "key1", true,
  "key2", false,
  "key3", true
);
SerializedValue serializedMap = SerializedValue.of(
                                  Map.of(
                                    "key1", SerializedValue.of(exMap.get("key1")),
                                    "key2", SerializedValue.of(exMap.get("key2")),
                                    "key3", SerializedValue.of(exMap.get("key3"))
                                  )
                                );

Vector3D exampleVec = new Vector3D(0,0,0);

SerializedValue serializedVec1 = SerializedValue.of(
                                   List.of(
                                     SerializedValue.of(exampleVec.getX()),
                                     SerializedValue.of(exampleVec.getY()),
                                     SerializedValue.of(exampleVec.getZ())
                                   )
                                 );

SerializedValue serializedVec2 = SerializedValue.of(
                                   Map.of(
                                     "x", SerializedValue.of(exampleVec.getX()),
                                     "y", SerializedValue.of(exampleVec.getY()),
                                     "z", SerializedValue.of(exampleVec.getZ())
                                   )
                                 );
```

The first 3 examples here are straightforward mappings from their java type to their serialized form, however the vector example is more interesting. To highlight this, two forms of `SerializedValue` have been given for it. In the first case, we serialize the `Vector3D` as a list of three values. This will work fine as long as whoever deserializes it knows that the list contains each component in order of x, y and z. In the second example, however, the vector is serialized as a map. Either of these representations may fit better in different scenarios. Generally, the structure of a `SerializedValue` constructed by a `ValueMapper` should match the `ValueSchema` the `ValueMapper` provides.

## Example Activity Mapper

Below is an example of an Activity Type and its Activity mapper for reference:

### Activity Type
```java
@ActivityType("foo")
public final class FooActivity {
  @Parameter
  public int x = 0;

  @Parameter
  public String y = "test";

  @Parameter
  public List<Vector3D> vecs = List.of(new Vector3D(0.0, 0.0, 0.0));

  @Validation("x cannot be exactly 99")
  public boolean validateX() {
    return (x != 99);
  }

  @Validation("y cannot be 'bad'")
  public boolean validateY() {
    return !y.equals("bad");
  }

  @EffectModel
  public void run(final Mission mission) {
    // ...
  }
}
```

## Generated Activity Mapper Example
```java
package gov.nasa.jpl.aerie.foomissionmodel.generated.activities;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.EnumValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.NullableValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers;
import gov.nasa.jpl.aerie.foomissionmodel.Mission;
import gov.nasa.jpl.aerie.foomissionmodel.activities.FooActivity;
import gov.nasa.jpl.aerie.foomissionmodel.generated.ActivityTypes;
import gov.nasa.jpl.aerie.foomissionmodel.mappers.FooValueMappers;
import gov.nasa.jpl.aerie.merlin.framework.ActivityMapper;
import gov.nasa.jpl.aerie.merlin.framework.ModelActions;
import gov.nasa.jpl.aerie.merlin.framework.RootModel;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.types.InvalidArgumentsException;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.UnconstructableArgumentException;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.processing.Generated;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

@Generated("gov.nasa.jpl.aerie.merlin.processor.MissionModelProcessor")
public final class FooActivityMapper implements ActivityMapper<RootModel<ActivityTypes, Mission>, FooActivity, Unit> {
  private final ValueMapper<Integer> mapper_x;

  private final ValueMapper<String> mapper_y;

  private final ValueMapper<List<Vector3D>> mapper_vecs;

  private final ValueMapper<Unit> computedAttributesValueMapper;

  private final Topic<FooActivity> inputTopic = new Topic<>();

  private final Topic<Unit> outputTopic = new Topic<>();

  @SuppressWarnings("unchecked")
  public FooActivityMapper() {
    this.mapper_x =
        BasicValueMappers.$int();
    this.mapper_y =
        new NullableValueMapper<>(
            BasicValueMappers.string());
    this.mapper_vecs =
        new NullableValueMapper<>(
            BasicValueMappers.list(
                FooValueMappers.vector3d(
                    BasicValueMappers.$double())));
    this.computedAttributesValueMapper =
        new EnumValueMapper(Unit.class);
  }

  @Override
  public List<String> getRequiredParameters() {
    return List.of();
  }

  @Override
  public ArrayList<Parameter> getParameters() {
    final var parameters = new ArrayList<Parameter>();
    parameters.add(new Parameter("x", this.mapper_x.getValueSchema()));
    parameters.add(new Parameter("y", this.mapper_y.getValueSchema()));
    parameters.add(new Parameter("vecs", this.mapper_vecs.getValueSchema()));
    return parameters;
  }

  @Override
  public Map<String, SerializedValue> getArguments(final FooActivity activity) {
    final var arguments = new HashMap<String, SerializedValue>();
    arguments.put("x", this.mapper_x.serializeValue(activity.x));
    arguments.put("y", this.mapper_y.serializeValue(activity.y));
    arguments.put("vecs", this.mapper_vecs.serializeValue(activity.vecs));
    return arguments;
  }

  @Override
  public FooActivity instantiate(final Map<String, SerializedValue> arguments) throws
      InvalidArgumentsException {
    final var template = new FooActivity();
    Optional<Integer> x = Optional.ofNullable(template.x);
    Optional<String> y = Optional.ofNullable(template.y);
    Optional<List<Vector3D>> vecs = Optional.ofNullable(template.vecs);

    final var invalidArgsExBuilder = new InvalidArgumentsException.Builder("activity", "foo");

    for (final var entry : arguments.entrySet()) {
      try {
        switch (entry.getKey()) {
          case "x":
            template.x = this.mapper_x.deserializeValue(entry.getValue())
                .getSuccessOrThrow(failure -> new UnconstructableArgumentException("x", failure));
            break;
          case "y":
            template.y = this.mapper_y.deserializeValue(entry.getValue())
                .getSuccessOrThrow(failure -> new UnconstructableArgumentException("y", failure));
            break;
          case "vecs":
            template.vecs = this.mapper_vecs.deserializeValue(entry.getValue())
                .getSuccessOrThrow(failure -> new UnconstructableArgumentException("vecs", failure));
            break;
          default:
            invalidArgsExBuilder.withExtraneousArgument(entry.getKey());
        }
      } catch (final UnconstructableArgumentException e) {
        invalidArgsExBuilder.withUnconstructableArgument(e.parameterName, e.failure);
      }
    }

    x.ifPresentOrElse(
        value -> invalidArgsExBuilder.withValidArgument("x", this.mapper_x.serializeValue(value)),
        () -> invalidArgsExBuilder.withMissingArgument("x", this.mapper_x.getValueSchema()));
    y.ifPresentOrElse(
        value -> invalidArgsExBuilder.withValidArgument("y", this.mapper_y.serializeValue(value)),
        () -> invalidArgsExBuilder.withMissingArgument("y", this.mapper_y.getValueSchema()));
    vecs.ifPresentOrElse(
        value -> invalidArgsExBuilder.withValidArgument("vecs", this.mapper_vecs.serializeValue(value)),
        () -> invalidArgsExBuilder.withMissingArgument("vecs", this.mapper_vecs.getValueSchema()));

    invalidArgsExBuilder.throwIfAny();
    return template;
  }

  @Override
  public List<String> getValidationFailures(final FooActivity activity) {
    final var failures = new ArrayList<String>();
    if (!activity.validateX()) failures.add("x cannot be exactly 99");
    if (!activity.validateY()) failures.add("y cannot be 'bad'");
    return failures;
  }

  @Override
  public ValueSchema getReturnValueSchema() {
    return this.computedAttributesValueMapper.getValueSchema();
  }

  @Override
  public SerializedValue serializeReturnValue(final Unit returnValue) {
    return this.computedAttributesValueMapper.serializeValue(returnValue);
  }

  public Topic<FooActivity> getInputTopic() {
    return this.inputTopic;
  }

  public Topic<Unit> getOutputTopic() {
    return this.outputTopic;
  }

  @Override
  public Task<Unit> createTask(final RootModel<ActivityTypes, Mission> model,
      final FooActivity activity) {
    return ModelActions
        .threaded(() -> {
          try (final var restore = model.registry().contextualizeModel(model)) {
            ModelActions.emit(activity, this.inputTopic);
            activity.run(model.model());
            ModelActions.emit(Unit.UNIT, this.outputTopic);
            return Unit.UNIT;
          }
        })
        .create(model.executor());
  }
}
```
