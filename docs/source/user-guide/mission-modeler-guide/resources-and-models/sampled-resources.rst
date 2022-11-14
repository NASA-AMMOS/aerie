=================
Sampled Resources
=================

A sampled resource allows for a new resource to be constructed from
arbitrarily many existing resources/values and to be sampled once per
second. This differs from a derived resource which provides a continuous
mapping transformation from a single existing resource.

For example, the
`Mission <https://github.com/NASA-AMMOS/aerie/blob/e3048083b78b7d3b6e2c9479e7f85a35b9047b6d/examples/foo-missionmodel/src/main/java/gov/nasa/jpl/aerie/foomissionmodel/Mission.java>`__
sample model defines a “battery state of charge” resource with:

.. code:: java

   this.batterySoC = new SampledResource<>(() -> this.source.volume.get() - this.sink.volume.get());

In this example, ``batterySoC`` will be updated once per second to with
the current difference between the “source” volume and “sink” volume.
