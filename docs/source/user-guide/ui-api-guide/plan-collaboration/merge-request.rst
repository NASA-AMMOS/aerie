=============
Merging Plans
=============

Create a Merge Request
----------------------

In order to merge changes between two related plans (most commonly between a branch and its parent),
a merge request must first be created between the **source plan** (the one supplying changes) and the **target plan** (the one to receive changes).

.. tabs::

  .. group-tab:: User Interface

    This is how to create a merge request using the UI.

  .. group-tab:: API

    A merge request can be created using the following mutation:

    .. include:: ../api-examples.rst
      :start-after: begin create merge request
      :end-before: end create merge request

Withdraw a Merge Request
========================

You can withdraw a merge request so long as it is in the ``pending`` state. A withdrawn merge request cannot be used to begin a merge.

.. tabs::

  .. group-tab:: User Interface

    This is how to withdraw a merge request using the UI.

  .. group-tab:: API

    .. include:: ../api-examples.rst
      :start-after: begin withdraw merge request
      :end-before: end withdraw merge request

Begin a Merge
-------------
.. note::

  Beginning a merge locks the target plan until the merge is either cancelled, denied, or committed.

.. tabs::

  .. group-tab:: User Interface

    This is how to begin a merge using the UI.

  .. group-tab:: API

    Once you have a pending merge request between two plans, you can use the following mutation to begin the merge:

    .. include:: ../api-examples.rst
       :start-after: begin begin merge
       :end-before: end begin merge

    By default, the above mutation will return the list of non-conflicting activities
    and the list of conflicting activities.
    If you would like neither, you can instead perform the following mutation:

    .. include:: ../api-examples.rst
      :start-after: begin begin merge no data
      :end-before: end begin merge no data

    If you want the list of non-conflicting activities of an in-progress merge at a later point, perform the following query:

    .. include:: ../api-examples.rst
      :start-after: begin non-conflicting activities
      :end-before: end non-conflicting activities

    If you want the list of conflicting activities of an in-progress merge, perform the following query:

    .. include:: ../api-examples.rst
      :start-after: begin conflicting activities
      :end-before: end conflicting activities

Cancel a Merge
==============

You can cancel any ``in-progress`` merge.

.. tabs::

  .. group-tab:: User Interface

    This is how to do so via the UI.

  .. group-tab:: API

    .. include:: ../api-examples.rst
      :start-after: begin cancel merge
      :end-before: end cancel merge

Resolving Conflicts
===================

Before a merge can be committed, all conflicts must be resolved to either ``source`` or ``target``.

.. tabs::

  .. group-tab:: User Interface

  .. group-tab:: API

    .. include:: ../api-examples.rst
      :start-after: begin resolve conflict
      :end-before: end resolve conflict

You can also resolve conflicts in bulk:

.. tabs::

  .. group-tab:: User Interface

  .. group-tab:: API

    .. include:: ../api-examples.rst
      :start-after: begin resolve conflict bulk
      :end-before: end resolve conflict bulk

Deny a Merge
============

It is possible to deny an in-progress merge, for example, if a request is outdated.
Once a merge has been denied, that request cannot be used to begin a merge.

.. tabs::

  .. group-tab:: User Interface

  .. group-tab:: API

    .. include:: ../api-examples.rst
      :start-after: begin deny merge
      :end-before: end deny merge

Commit a Merge
==============

Once all conflicts have been resolved, you can commit a merge.

.. tabs::

  .. group-tab:: User Interface

  .. group-tab:: API

    .. include:: ../api-examples.rst
      :start-after: begin commit merge
      :end-before: end commit merge
