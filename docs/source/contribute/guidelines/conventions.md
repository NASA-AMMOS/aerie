---
orphan: true
---
# Brand Types

In brief, a brand is a type-level witness for a single value.
It seems to be folklore within the functional programming community, going back at least as far as 1995.
See the abstract of [this paper](https://plv.mpi-sws.org/rustbelt/ghostcell/paper.pdf) and some of the references to "brand" therein.

A brand is achieved via a method like `Foo<?> create()`, which instantiates a new value of type `Foo<?>`.
The wildcard means that this method has full control over what type Foo is actually instantiated over, so any two invocations of `create()` cannot be inferred to use the same type.
The type is unique to the single value obtained from a single invocation of `create()`.
(These are sometimes also called type-level singletons.)

We can apply the brand to related data, as in `<$> Bar<$> transform(Foo<$> f)`.
We know statically that the Bar we get out is related specifically and solely to the Foo instance we passed in.

The convention in Aerie of using a `$` prefix to denote a brand is for two reasons.
First, it's a rare symbol and serves to distinguish brands from true types.
Second, `$` is somewhat memorably associated in my mind to a hot metal brand of the physical variety.

## `$Schema` Generic Type

The `$Schema` brand is generated for every instance of the mission model.
See `Schema.java` – specifically the `builder()` method – it generates a builder with a fresh brand, whose type parameter is `$Schema`.
It's named so because this type corresponds with the one and only `Schema` instance that this builder will ever construct.
A `Schema` contains information about the kinds of data that will be stored for any simulation against that model instance.
Specifically, information about the cells in which the model keeps its state and to which it will post typed events.

Another way of understanding `$Schema` is to think about it as `$MissionModel` since it's unique to each mission model.
It was named because of how it interacts with the `SimulationTimeline`: as a schema for multiple timeline instances.
At the time this was put together, use-cases for multiple simulations against the same mission model instance (same configuration and all) were anticipated.
That's seeming less and less likely as time goes on, so everything from here on may be able to be simplified down.

## `$Timeline` Generic Type

First, remember that in Java, the type hierarchy (ignoring interfaces) is a tree.
For any type there is a chain of ancestors and a subtree of descendants.

`? super T` lets us speak about all ancestors of `T`, and `? extends T` lets us speak about all descendants of `T`.
We have here a type `$Schema`, manifesting a unique `Schema` at the type level.
We know that we want to have multiple simulations, each with their own event graph (timeline), each supporting the same queries expressed by the `Schema`.
So when we instantiate a `SimulationTimeline` against the `Schema`, we create another brand – conventionally `$Timeline` – and state that it lies below the `$Schema` brand on the tree.
Hence, `$Timeline extends $Schema`:

```java
<$Schema>
SimulationTimeline<? extends $Schema>
create(final Schema<$Schema> schema);
```

## Summary

The reason for the brands is that we don't want objects from one timeline to accidentally get used in the context of another timeline.
A timeline is shaped more like a database of tables than an object graph, and we want to be able to safely assume that an index into one timeline doesn't come from elsewhere.

So a `$Timeline` brand lets us catch at compile-time issues of cross-pollination,
and a `$Schema` "superbrand" lets us treat them in common in some other respects (e.g. using a `Query` compatible with any of them).
In particular, `Query` is associated per-schema, and `Querier` must be able to apply a query on a supported schema to a specific timeline.
