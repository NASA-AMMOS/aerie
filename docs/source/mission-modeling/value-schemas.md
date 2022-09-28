# Value Schema

A value _schema_! A value schema is a description of the structure of some value. Using value
schemas, users can tell our system how to work with arbitrarily complex types of values, so 
long as they can be described using the value schema constructs provided by Merlin. Let's 
take a look at how it's done.

## Starting With The Basics
At a fundamental level, a value schema is no more than a combination of elementary value 
schemas. Merlin defines the elementary value schemas, so let's take a look at them:
- `REAL`: A real number 
- `INT`: An integer
- `BOOLEAN`: A boolean value
- `STRING`: A string of characters
- `DURATION`: A duration value
- `PATH`: A file path
- `VARIANT`: A string value constrained to a set of acceptable values.

If you are trying to write a value schema for an integer value, all you have to do is 
use the `INT` value schema, but of course values can quickly take on more complex 
structures, and for that we must examine the remaining value schema constructs.

#### A Note About Variants
The `Variant` value schema is a little unique among the elementary value schemas in 
that it requires input, the set of acceptable values. The way to provide this set of 
values depends on the context in which you are creating a value schema and will be 
addressed in the corresponding section below.

## Building Things Up
In order to combine elementary value schemas, we provide two main constructs:
- `SERIES`: Denotes a list of values of a single type
- `STRUCT`: Denotes a structure of independent values of varying types

The `SERIES` node allows a straightforward declaration of a list of values that fall 
under the same schema, while the `STRUCT` node opens things up, allowing you to create
any combination of different values, each labeled by some string name.

Now that you've seen the basics, let's talk about the two different ways to create 
value schemas -- in code and in JSON/GraphQL (serialized value schemas).

## Value Schemas From Code
Creating a value schema from code is straightforward thanks to the `ValueSchema` class.
Each of the elementary value schemas is accessible as via the `ValueSchema` class. For 
example, a `REAL` is given by `ValueSchema.REAL`. The one exception is that to create 
a `VARIANT` type value schema you'll need to call 
`ValueSchema.ofVariant(Class<? extends Enum> enum)`, providing an enum with to specify 
the acceptable variants.

Like the `VARIANT` element, the `SERIES` and `STRUCT` constructs are created by calling
their corresponding methods `ValueSchema.ofSeries(ValueSchema value)` 
and `ValueSchema.ofStruct(Map<String, ValueSchema> map)`.

## Examples Using ValueSchema
Below are a few examples of how to create a `ValueSchema`. In each, a Java type and its 
corresponding `ValueSchema` are compared.

- `Integer` is described by `ValueSchema.INT`
- `List<Double>` is described by `ValueSchema.ofSeries(ValueSchema.REAL)`
- `Float[]` is described by `ValueSchema.ofSeries(ValueSchema.REAL)`

Note that the second and third examples are entirely different Java types, but are
represented by the same `ValueSchema`. It is also important to take a look at a `Map`
type, as it can be confusing at first how to represent its structure:

`Map<String, Integer>` is described by 
```
ValueSchema.ofStruct(
  Map.of(
    "keys": ValueSchema.ofSeries(ValueSchema.STRING),
    "values": ValueSchema.ofSeries(ValueSchema.INT)
  )
)
```

Here we are taking note of the fact that a `Map` is really just a list of keys and a
list of values. As a final example, consider the custom type below:

```
public class CustomType {
  public int foo;
  public boolean bar;
  public List<String> baz
}
```

A variable of type `CustomType` has structure described by:

```
ValueSchema.ofStruct(
  Map.of(
    "foo": ValueSchema.INT,
    "bar": ValueSchema.BOOLEAN,
    "baz": ValueSchema.ofSeries(ValueSchema.STRING)
  )
)
```

## Serialized Value Schemas
Creating value schemas from JSON/GraphQL is a little less straightforward, since your IDE won't be able to help you, but fear not, you've come to the right place. A value schema is created by declaring an object with a `type` field that tells which type of schema is being created. The values allowed in this field are given below:
- `"real"` corresponds to `REAL`
- `"int"` corresponds to `INT`
- `"boolean"` corresponds to `BOOLEAN`
- `"string"` corresponds to `STRING`
- `"duration"` corresponds to `DURATION`
- `"path"` corresponds to `PATH`
- `"variant"` corresponds to `VARIANT`
- `"series"` corresponds to `SERIES`
- `"struct"` corresponds to `STRUCT`

### Variant
For the `"variant"` type, you'll need to include a second field called `variants` whose value is a list of objects specifying the string-valued `key` and `label` fields of each variant like this:
```
{
  "type": "variant",
  "variants": [
    {
      "key": "ON",
      "label": "ON"
    },
    {
      "key": "OFF",
      "label": "OFF"
    }
  ]
}
```

### Series
For the `"series"` type, a second field called `"items"` must be included as well that provides the value schema for the items in the series. See the below example, a value schema for a list of integers.
```
{
  "type": "series",
  "items": {
    "type": "int"
  }
}
```

### Struct
Lastly, for the `"struct"` type, a second field called `"items"` must be included that provides the actual structure of the struct, mapping string keys to their corresponding value schema. See the below example, a value schema for a struct with a string-valued `label` field, real-valued `position` field, and boolean-valued `on` field:
```
{
  "type": "struct",
  "items": {
    "label": { "type": "string" },
    "position": { "type": "real" },
    "on": { "type": "boolean" }
  }
}
```

## Examples Creating Serialized Value Schemas
Below are more examples of creating serialized value schemas using JSON:

A value schema for an integer:
```JSON
{
  "type": "int"
}
```

A value schema for a list of paths:
```JSON
{
  "type": "series",
  "items": {
    "type": "path"
  }
}
```

A value schema for a list of lists of booleans:
```JSON
{
  "type": "series",
  "items": {
    "type": "series",
    "items": {
      "type": "boolean"
    }
  }
}
```

A value schema for a structure containing a list of integers labeled `lints`, and a boolean labeled `active`:
```JSON
{
  "type": "struct",
  "items": {
    "lints": {
      "type": "series",
      "items": { "type": "int" }
    },
    "active": {
      "type": "boolean"
    }
  }
}
```
