====================
Value Schema Basics
====================

A value schema is a description of the structure of
some value. Using value schemas, users can tell our system how to work
with arbitrarily complex types of values, so long as they can be
described using the value schema constructs provided by Merlin. Let’s
take a look at how it’s done.

Starting With The Basics
------------------------

At a fundamental level, a value schema is no more than a combination of
elementary value schemas. Merlin defines the elementary value schemas,
so let’s take a look at them:

- ``REAL``: A real number
- ``INT``: An integer
- ``BOOLEAN``: A boolean value
- ``STRING``: A string of characters
- ``DURATION``: A duration value
- ``PATH``: A file path
- ``VARIANT``: A string value constrained to a set of acceptable values.

If you are trying to write a value schema for an integer value, all you
have to do is use the ``INT`` value schema, but of course values can
quickly take on more complex structures, and for that we must examine
the remaining value schema constructs.

A Note About Variants
~~~~~~~~~~~~~~~~~~~~~

The ``Variant`` value schema is a little unique among the elementary
value schemas in that it requires input, the set of acceptable values.
The way to provide this set of values depends on the context in which
you are creating a value schema and will be addressed in the
corresponding section below.

Building Things Up
------------------

In order to combine elementary value schemas, we provide two main
constructs: - ``SERIES``: Denotes a list of values of a single type -
``STRUCT``: Denotes a structure of independent values of varying types

The ``SERIES`` node allows a straightforward declaration of a list of
values that fall under the same schema, while the ``STRUCT`` node opens
things up, allowing you to create any combination of different values,
each labeled by some string name.

Now that you’ve seen the basics, let’s talk about the two different ways
to create value schemas – in code and in JSON/GraphQL (serialized value
schemas).
