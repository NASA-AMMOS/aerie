=============
Merging Plans
=============

Create a Merge Request
----------------------

.. tabs::

  .. group-tab:: User Interface

    This is how to create a merge request using the UI.

  .. group-tab:: API

    This is how to create a merge request using the API.

Withdraw a Merge Request
========================

.. tabs::

  .. group-tab:: User Interface

    This is how to withdraw a merge request using the UI.

  .. group-tab:: API

    This is how to withdraw a merge request using the API.

Begin a Merge
-------------
.. note::

  Beginning a merge locks the target plan until the merge is either cancelled, denied, or committed!

.. tabs::

  .. group-tab:: User Interface

    This is how to begin a merge using the UI.

  .. group-tab:: API

    This is how to begin a merge using the API.

    By default, the above mutation will return the list of non-conflicting activities
    and the list of conflicting activities.
    If you would like neither, you can instead perform the following mutation:

    If you want the list of non-conflicting activities of an in-progress merge at a later point, perform the following query:

    If you want the list of conflicting activities of an in-progress merge, perform the following query:

Cancel a Merge
==============

Resolving Conflicts
===================

Deny a Merge
============

Commit a Merge
==============
