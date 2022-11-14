================================
Advanced Topic: Activity Mappers
================================

An Activity Mapper is a Java class that implements the
``ActivityMapper`` interface for the ``ActivityType`` being mapped. It
is required that each Activity Type in an mission model have an
associated Activity Mapper, to provide provide several capabilities
surrounding serialization/deserialization of activity instances.

The Merlin annotation processor can automatically be generated for every
activity type, even for those with custom-typed parameters, but if it is
desirable to create a custom activity mapper the interface, it is described
in this section.

.. toctree::
  :includehidden:

  activity-mapper-interface
  example-activity-mapper
