=====================
Mission Modeler Guide
=====================

In Merlin, a mission model serves activity planning needs in two
ways. First, it describes how various mission resources behave
autonomously over time. Second, it defines how activities perturb
these resources at discrete time points, causing them to change their
behavior. This information enables Aerie to provide scheduling,
constraint validation, and resource plotting capabilities on top of a
mission model.


Creating a Mission Model
------------------------
To see how to create a mission model, see our :doc:`Quickstart Guide <../quickstart/create-mission-model>`.

.. _package-info-file:

The Package-info File
---------------------
A mission model must contain, at the very least, a
``package-info.java`` containing annotations that describe the
highest-level features of the mission model. For example:

.. code:: java

   // examples/banananation/package-info.java
   @MissionModel(model = Mission.class)
   @WithActivityType(BiteBananaActivity.class)
   @WithActivityType(PeelBananaActivity.class)
   @WithActivityType(ParameterTestActivity.class)
   @WithMappers(BasicValueMappers.class)
   package gov.nasa.jpl.aerie.banananation;

   import gov.nasa.jpl.aerie.banananation.activities.BiteBananaActivity;
   import gov.nasa.jpl.aerie.banananation.activities.ParameterTestActivity;
   import gov.nasa.jpl.aerie.banananation.activities.PeelBananaActivity;
   import gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers;
   import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel;
   import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel.WithActivityType;
   import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel.WithMappers;

This ``package-info.java`` identifies the top-level class representing
the mission model, and registers activity types that may interact with
the mission model. Merlin processes these annotations at compile-time,
generating a set of boilerplate classes which take care of interacting
with the Aerie platform.

The ``@WithMappers`` annotation informs the annotation processor of a
set of serialization rules for activity parameters of various types;
the `BasicValueMappers
<https://github.com/NASA-AMMOS/aerie/blob/develop/contrib/src/main/java/gov/nasa/jpl/aerie/contrib/serialization/rulesets/BasicValueMappers.java>`__
ruleset covers most primitive Java types. Mission modelers may also
create their own rulesets, specifying rules for mapping custom value
types. If multiple mapper classes are included via the
``@WithMappers`` annotations, and multiple mappers declare a mapping
rule to the same data type, the rule found in the earlier declared
mapper will take precedence. For more information on allowing custom
values, see :doc:`value mappers <./custom-value-mappers/index>`.

The Mission Model Class
-----------------------
The top-level mission model is responsible for defining all of the
mission resources and their behavior when affected by activities. Of
course, the top-level model may delegate to smaller, more focused
models based on the needs of the mission. The top-level model is
received by activities, however, so it must make accessible any
resources or methods to be used therein.

.. code:: java

   // examples/banananation/Mission.java
   public class Mission {
     public final AdditiveRegister fruit = AdditiveRegister.create(4.0);
     public final AdditiveRegister peel = AdditiveRegister.create(4.0);
     public final Register<Flag> flag = Register.create(Flag.A);

     public Mission(final Registrar registrar) {
       registrar.discrete("/flag", this.flag, new EnumValueMapper<>(Flag.class));
       registrar.real("/peel", this.peel);
       registrar.real("/fruit", this.fruit);
     }
   }

Mission resources are declared using ``Registrar#discrete`` or
``Registrar#real``.

A model may also express autonomous behaviors, where a discrete change
occurs in the system outside of an activity’s effects. A **daemon
task** can be used to model these behaviors. Daemons are spawned at
the beginning of any simulation, and may perform the same effects as
an activity. Daemons are prepared using the ``spawn`` method.

Building a Mission Model
------------------------


.. toctree::
  :includehidden:
  :maxdepth: 1

  activity-types/index
  resources-and-models/index
  configuration/index
  parameters/index
  custom-value-types/index
  custom-value-mappers/index
  activity-mappers/index
  foundations-of-simulation-and-modeling
