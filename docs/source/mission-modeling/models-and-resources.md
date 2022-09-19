# Models and Resources

## Mission Resources

In Merlin, a resource is any measurable quantity whose behavior is to be tracked over the course of a simulation.
Resources are general-purpose, and can model quantities such as finite resources, geometric attributes, ground and flight events, and more.
Merlin provides basic models for three types of quantity: a discrete quantity that can be set (`Register`), a continuous quantity that can be added to (`Counter`), and a continuous quantity that grows autonomously over time (`Accumulator`).

A common example of a `Register` would be a spacecraft or instrument mode, while common `Accumulator`s might be battery capacity or data volume.

Defining a resource is as simple as constructing a model of the appropriate type.
The model will automatically register its resources for use from the Aerie UI.
Alternatively, a resource may be **derived** or **sampled** from an existing resource.

### Derived Resources

A derived resource is constructed from an existing resource given a mapping transformation.

For example, the [Imager](https://github.com/NASA-AMMOS/aerie/blob/e3048083b78b7d3b6e2c9479e7f85a35b9047b6d/examples/foo-missionmodel/src/main/java/gov/nasa/jpl/aerie/foomissionmodel/models/Imager.java) sample model defines an "imaging in progress" resource with:
```java
this.imagingInProgress = this.imagerMode.map($ -> $ != ImagerMode.OFF);
```
In this example, `imagingInProgress` is a full-fledged discrete resource and will depend only on the imager's on/off state.

A derived resource may also be constructed from a real resource. For example, given `Accumulator`s `instrumentA` and `instrumentB`, a resource that maintains the current sum of both volumes may be constructed with:
```java
var sumResource = instrumentA.volume.resource.plus(instrumentB.volume.resource);
```

### Sampled Resources

A sampled resource allows for a new resource to be constructed from arbitrarily many existing resources/values and to be sampled once per second.
This differs from a derived resource which provides a continuous mapping transformation from a single existing resource.

For example, the [Mission](https://github.com/NASA-AMMOS/aerie/blob/e3048083b78b7d3b6e2c9479e7f85a35b9047b6d/examples/foo-missionmodel/src/main/java/gov/nasa/jpl/aerie/foomissionmodel/Mission.java) sample model defines a "battery state of charge" resource with:
```java
this.batterySoC = new SampledResource<>(() -> this.source.volume.get() - this.sink.volume.get());
```
In this example, `batterySoC` will be updated once per second to with the current difference between the "source" volume and "sink" volume.


## Custom models

Often, the semantics of the pre-existing models are not exactly what you need in your adaptation.
Perhaps you'd like to prevent activities from changing the rate of an `Accumulator`, or you'd like to have some helper methods for interrogating one or more resources.
In these cases, a custom model may be a good solution.

A custom model is a regular Java class, extending the `Model` class generated for your adaptation by Merlin (or the base class provided by the framework, if it's mission-agnostic).
It may implement any helper methods you'd like, and may contain any sub-models that contribute to its purpose.
The only restriction is that it **must not** contain any mutable state of its own -- all mutable state must be held by one of the basic models, or one of the internal state-management entities they use, known as "cells".

The `contrib` package is a rich source of example models.
See [the repository](https://github.com/NASA-AMMOS/aerie/tree/develop/contrib/src/main/java/gov/nasa/jpl/aerie/contrib/models) for more details.
