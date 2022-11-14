====================
Value Schema in Code
====================

Creating a value schema from code is straightforward thanks to the
``ValueSchema`` class. Each of the elementary value schemas is
accessible as via the ``ValueSchema`` class. For example, a ``REAL`` is
given by ``ValueSchema.REAL``. The one exception is that to create a
``VARIANT`` type value schema you’ll need to call
``ValueSchema.ofVariant(Class<? extends Enum> enum)``, providing an enum
with to specify the acceptable variants.

Like the ``VARIANT`` element, the ``SERIES`` and ``STRUCT`` constructs
are created by calling their corresponding methods
``ValueSchema.ofSeries(ValueSchema value)`` and
``ValueSchema.ofStruct(Map<String, ValueSchema> map)``.

Examples Using ValueSchema
--------------------------

Below are a few examples of how to create a ``ValueSchema``. In each, a
Java type and its corresponding ``ValueSchema`` are compared.

-  ``Integer`` is described by ``ValueSchema.INT``
-  ``List<Double>`` is described by
   ``ValueSchema.ofSeries(ValueSchema.REAL)``
-  ``Float[]`` is described by
   ``ValueSchema.ofSeries(ValueSchema.REAL)``

Note that the second and third examples are entirely different Java
types, but are represented by the same ``ValueSchema``. It is also
important to take a look at a ``Map`` type, as it can be confusing at
first how to represent its structure:

``Map<String, Integer>`` is described by

::

   ValueSchema.ofStruct(
     Map.of(
       "keys": ValueSchema.ofSeries(ValueSchema.STRING),
       "values": ValueSchema.ofSeries(ValueSchema.INT)
     )
   )

Here we are taking note of the fact that a ``Map`` is really just a list
of keys and a list of values. As a final example, consider the custom
type below:

::

   public class CustomType {
     public int foo;
     public boolean bar;
     public List<String> baz
   }

A variable of type ``CustomType`` has structure described by:

::

   ValueSchema.ofStruct(
     Map.of(
       "foo": ValueSchema.INT,
       "bar": ValueSchema.BOOLEAN,
       "baz": ValueSchema.ofSeries(ValueSchema.STRING)
     )
   )
