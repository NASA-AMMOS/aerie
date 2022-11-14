=================
Derived Resources
=================

A derived resource is constructed from an existing resource given a
mapping transformation.

For example, the
`Imager <https://github.com/NASA-AMMOS/aerie/blob/e3048083b78b7d3b6e2c9479e7f85a35b9047b6d/examples/foo-missionmodel/src/main/java/gov/nasa/jpl/aerie/foomissionmodel/models/Imager.java>`__
sample model defines an “imaging in progress” resource with:

.. code:: java

   this.imagingInProgress = this.imagerMode.map($ -> $ != ImagerMode.OFF);

In this example, ``imagingInProgress`` is a full-fledged discrete
resource and will depend only on the imager’s on/off state.

A derived resource may also be constructed from a real resource. For
example, given ``Accumulator``\ s ``instrumentA`` and ``instrumentB``, a
resource that maintains the current sum of both volumes may be
constructed with:

.. code:: java

   var sumResource = instrumentA.volume.resource.plus(instrumentB.volume.resource);
