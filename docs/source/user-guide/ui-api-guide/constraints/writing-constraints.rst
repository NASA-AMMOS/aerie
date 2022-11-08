===================
Writing Constraints
===================

The goal of most constraints is to produce a ``Windows`` object through
operations on profiles and activity instances. This is a conceptual
guide to what they represent. More specific documentation is generated
for the `Constraints API
documentation <../../../../constraints-edsl-api>`_.

Important Concepts
==================

Profiles
________

Profiles represent functions over time to a specific type, and usually
come from mission model resources. For example, a “mission phase”
resource might be simulated by the mission model and exposed to the
constraints with intervals of time labelled as ``cruise``, ``edl``, and
``surface``; a “battery charge” resource could be represented as a real
number between 0 and 1 that varies with activities; etc. These resources
can be referenced by calling ``Discrete.Resource("mission phase")`` or
``Real.Resource("battery charge")`` in the constraint, allowing you to
transform them and do comparisons.

Real profiles are for integers and floating point numbers, and provide
methods for some basic math operations like addition and derivatives.
Discrete profiles are for everything else, like strings or objects.

Profiles can have *gaps*, or intervals where the value is unknown. This
comes up most often when dealing with external datasets. In most cases
it is best to apply a default value to a profile’s gaps ASAP using the
``profile.assignGaps(defaultValue)`` method.

Windows
_______

Windows are like a boolean profile, augmented with some extra
functionality. For many constraints, the final result is a ``Windows``
object, which tells Aerie what times are violations of the constraint.
``true`` means the state is nominal and ``false`` means the state is a
violation. This means that you should describe the conditions you *want*
to happen, not the conditions you *don’t* want to happen.

There are a few ways to calculate a ``Windows`` object from profiles,
and many operations that can be done on them; including the traditional
boolean ``and``, ``or``, and ``not``. These are all in the `API
documentation <../../../../constraints-edsl-api>`__. Like all profiles,
Windows can have gaps too. However, some operations (such as converting
to ``Spans`` or splitting segments) are not possible on gaps, so you’ll
be required to apply a default value using
``windows.assignGaps(boolean)``.

You can directly return your ``Windows`` object from the constraint function,
and it will be automatically converted in to a ``Constraint`` object.

Spans
_____

``Spans`` are designed to work around a limitation of ``Windows``: if
two ``true`` segments of a ``Windows`` object are transformed so that
they touch, they are combined or “coalesced” into a single segment, and
the knowledge that they were originally separate is lost. In situations
where this is not acceptable (such as when working with activities that
can overlap), ``Spans`` can be used. ``Spans`` are not a type of profile
at all; instead they’re just a collection of intervals on the timeline.
They share some operations available for ``Windows``, but not all are
valid (such as ``not``).

Activity Instances
__________________

Activity instances are accessible through two functions:
``Constraint.ForEachActivity`` and ``Spans.ForEachActivity``. These
functions provide you with a reference to each activity of a given type,
and allow you to access the activity’s location in time as a ``Windows``
or ``Spans`` object, and exposes its parameters as profiles.

Other constraint types
______________________

Not all constraints are based solely off of a ``Windows`` object. The main exceptions are constraints that deal with
individual activity instances, because these will use the
`Constraint.ForEachActivity(...) <../../../../constraints-edsl-api/classes/Constraint/#foreachactivity>`_ function instead of
returning a basic ``Windows``. This will evaluate your ``Windows`` expression once for each instance of the given activity
type, and associate any violations in those expressions with the activity instance it was evaluated on, which can be helpful
in the UI to figure out which activity caused the violation.

Mental Model for Evaluation
===========================

A constraint doesn’t directly query simulation data, or directly return
violations. Instead, your constraint code defines an expression to be
interpreted by the Merlin server. The exact implementation details don’t
matter for constraint authors, but for this reason you cannot directly
inspect a profile’s values or a plan’s activities. This is also why
there are no plans to support querying external profiles directly from a
web request or filesystem access *inside* the constraint code. For that,
see the `external dataset documentation <../../external-datasets>`__.
