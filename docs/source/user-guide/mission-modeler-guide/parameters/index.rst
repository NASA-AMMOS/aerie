==========================
Advanced Topic: Parameters
==========================

The Merlin interface offers a variety of ways to define **parameters**
for mission model configurations and activities. Parameters offer a
concise way to export information across the mission-agnostic Merlin
interface – namely a parameter’s type to support serialization and a
parameter’s “required” status to ensure that parameters without
mission-model-defined defaults have an argument supplied by the planner.

In this guide **parent class** refers to the Java class that
encapsulates parameters. This class may take the form of either a
mission model configuration or activity.

Both configurations and activities make use of the same Java annotations
for declaring parameters within a parent class. The ``@Export``
annotation interface serves as the common qualifier for exporting
information across the mission-agnostic Merlin interface. The following
parameter annotations serve to assist with parameter declaration and
validation:

* ``@Export.Parameter``
* ``@Export.Template`` 
* ``@Export.WithDefaults``
* ``@Export.Validation`` and ``@Export.Validation.Subject``

The following sections delve into each of these annotations along with
examples and suggested use cases for each.


.. toctree::
  :includehidden:
  :maxdepth: 2

  without-export
  export-parameter
  export-template
  export-with-defaults
