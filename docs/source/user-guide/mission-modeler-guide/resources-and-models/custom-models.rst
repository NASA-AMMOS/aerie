=============
Custom Models
=============

Often, the semantics of the pre-existing models are not exactly what you
need in your mission model. Perhaps you’d like to prevent activities
from changing the rate of an ``Accumulator``, or you’d like to have some
helper methods for interrogating one or more resources. In these cases,
a custom model may be a good solution.

A custom model is a regular Java class, extending the ``Model`` class
generated for your mission model by Merlin (or the base class provided
by the framework, if it’s mission-agnostic). It may implement any helper
methods you’d like, and may contain any sub-models that contribute to
its purpose. The only restriction is that it **must not** contain any
mutable state of its own – all mutable state must be held by one of the
basic models, or one of the internal state-management entities they use,
known as “cells”.

The ``contrib`` package is a rich source of example models. See `the
repository <https://github.com/NASA-AMMOS/aerie/tree/develop/contrib/src/main/java/gov/nasa/jpl/aerie/contrib/models>`__
for more details.
