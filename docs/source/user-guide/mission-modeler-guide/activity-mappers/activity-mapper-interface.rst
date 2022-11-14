========================
ActivityMapper Interface
========================

The ``ActivityMapper`` interface is shown below:

.. code:: java

   public interface ActivityMapper<Instance> {
     String getName();
     Map<String, ValueSchema> getParameters();
     Map<String, SerializedValue> getArguments(Instance activity);

     Instance instantiateDefault();
     Instance instantiate(Map<String, SerializedValue> arguments) throws TaskSpecType.UnconstructableTaskSpecException;

     List<String> getValidationFailures(Instance activity);
   }

The first thing to notice is that the interface takes a type parameter
(here called ``Instance``). When implementing the ``ActivityMapper``
interface, an activity mapper must supply the ``ActivityType`` being
mapped. With that in mind, each of the methods shown must be implemented
as such:

-  ``getName()`` returns the name of the activity type being mapped
-  ``getParameters()`` provides the named parameter fields of the
   activity along with their corresponding
-  ``ValueSchema``, that describes their structure
-  ``getArguments(Instance activity)`` provides the actual values for
   each parameter from a provided
-  activity instance
-  ``instantiateDefault()`` creates a default instance of the activity
   type without any values provided
-  externally
-  ``instantiate(Map<String, SerializedValue> arguments)`` constructs an
   instance of the activity type
-  from a the provided arguments, if possible
-  ``getValidationFailures(Instance activity)`` provides a list of
   reasons a constructed activity is
-  invalid, if any. Note that validation failures are different from
   instantiation errors. Validation failures occur when
-  a constructed activity instance’s parameters are outside acceptable
   range.

The ``getParameters()`` method returns a ``Map<String, ValueSchema>``.
In this map should be a key for every parameter, with a ``ValueSchema``
describing the structure of that parameter. See our `Value Schema
documentation <value-schemas.md#value-schemas-from-code>`__ for more
information on creating value schemas.

Generated Activity Mappers
--------------------------

In most cases, you will likely want to let Merlin generate activity
mappers for you. Thankfully, this is the done automatically when running
the Merlin Annotation Processor. When compiling your code with the
Merlin annotation processor, the processor will produce an activity
mapper for each activity type. This is made possible by the use of the
``@WithMappers()`` annotations in your
`package-info.java <developing-a-mission-model.md#package-info-file>`__.
Each java-file specified by these annotations is parsed to determine
what types of values can be mapped. As long as there is a mapper for
each activity parameter type used in the model, the annotation processor
should have no issues creating activity mappers.

Value Mappers
-------------

Regardless of whether you create custom activity mappers or let Merlin
generate them for you, you will likely find the need to work with a
``ValueMapper`` at some point. In fact, generating activity mappers is
made quite simple by considering the fact that an activity instance is
wholly defined by its parameter values.

You may find yourself asking “Just what *is* a value mapper?” A value
mapper is a small, focused class whose sole responsibility is to tell
Merlin how to handle a specific type of value. Value mappers allow all
sorts of capabilities from custom-typed activity parameters to
custom-typed resources.

One of the most convenient things about using value mappers is the fact
that Merlin comes with them already defined for all basic types.
Furthermore, value mappers for combinations of types can easily be
created by passing one ``ValueMapper`` into another during
instantiation.

Although we provide value mappers for basic types, it is entirely
acceptable to create custom value mappers for other types, such as those
imported from external libraries. This can be done by writing a Java
class which implements the ``ValueMapper`` interface. Below is a value
mapper for an apache ``Vector3D`` type as an example:

.. code:: java

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

Notice there are just 3 methods to implement for a ``ValueMapper``. The
first is ``getValueSchema()``, which should return a ``ValueSchema``
describing the structure of the value being mapped (see `value
schemas <value-schemas>`__ for more info)

The next two methods are inverses of each other: ``deserializeValue()``
and ``serializeValue()``. It is the job of ``deserializeValue()`` to
take a ``SerializedValue`` and map it, if possible, into the mapper’s
supported value. Meanwhile, ``serializeValue()`` takes an instance of
the mapper’s supported value and turns it into a
```SerializedValue`` <#what-is-a-serializedvalue>`__.

There are plenty of examples of value mappers over in the `contrib
module <https://github.com/NASA-AMMOS/aerie/tree/develop/contrib/src/main/java/gov/nasa/jpl/aerie/contrib/serialization/mappers>`__.

Registering Value Mappers
~~~~~~~~~~~~~~~~~~~~~~~~~

As mentioned above, the ``@WithMappers()`` annotation is used to
register value mappers for a mission model. Value mappers are expected
to be defined with static constructor methods within classes listed in
``@WithMappers()`` annotations. For example, if ``package-info.java``
contains:

.. code:: java

   @WithMappers(BananaValueMappers.class)

Then the value mapper may define a custom ``Configuration`` value mapper
with:

.. code:: java

   public final class BananaValueMappers {
     public static ValueMapper<Configuration> configuration() {
       return new ConfigurationValueMapper();
     }
   }

Value mappers may be created for types that use parameterized types, but
the parameterized types themselves must be either unbounded bounded or
``Enum<>``. For example:

.. code:: java

   @Parameter
   public List<? extends Foo> test;

or

.. code:: java

   @Parameter
   public List<? extends Map<? super Foo, ? extends Bar>> test;

are not trivially resolved to a single value mapper due to the type
constraints at play here.

What is a SerializedValue
-------------------------

When working with a ``ValueMapper`` it is inevitable that you will come
across the ``SerializedValue`` type. This is the type we use for
serializing all values that need serialization, such as activity
parameters and resource values. In crafting a value mapper, you will
have to both create a ``SerializedValue`` and parse one.

Constructing a ``SerializedValue`` tends to be more straightforward,
because there are no questions about the structure of the value you are
starting with. For basic types, you need only call
``SerializedValue.of(value)`` and the ``SerializedValue`` class will
handle the rest. This can be done for values of the following types:
``long``, ``double``, ``String``, ``boolean``. Note that integers and
floats can be represented by ``long`` and ``double`` respectively. For
more complex types, you can also provide a ``List<SerializedValue>`` or
``Map<String, SerializedValue>`` to ``SerializedValue.of()``. It is
clear that these can be used to serialize lists and maps themselves, but
arbitrarily complex structures can be serialized in this way. Consider
the following examples:

.. code:: java

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

The first 3 examples here are straightforward mappings from their java
type to their serialized form, however the vector example is more
interesting. To highlight this, two forms of ``SerializedValue`` have
been given for it. In the first case, we serialize the ``Vector3D`` as a
list of three values. This will work fine as long as whoever
deserializes it knows that the list contains each component in order of
x, y and z. In the second example, however, the vector is serialized as
a map. Either of these representations may fit better in different
scenarios. Generally, the structure of a ``SerializedValue`` constructed
by a ``ValueMapper`` should match the ``ValueSchema`` the
``ValueMapper`` provides.