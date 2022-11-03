==================
Plan Collaboration
==================

Aerie supports two forms of collaboration:

1. **Real-time collaboration**, where multiple users can work on the same plan at the same time
2. **Branch-based collaboration**, where users can branch existing plans and later merge their changes into other related plans.

Real-Time Collaboration
-----------------------


Branch-Based Collaboration
--------------------------

.. _branching-a-plan:

Branching a Plan
================

Branches can be made from any existing plan.
A branched plan will have the same start and end time as its parent plan, as well as a copy of all the activities in the parent at the moment it was branched.
It will not, however, contain any simulation data, scheduling goals, or constraints that were associated with the parent plan.

.. tabs::

  .. group-tab:: User Interface

    This is how to create a branch using the UI.

  .. group-tab:: API

    This is how to create a branch using the API.

.. toctree::
  :hidden:
  :includehidden:

  merge-request
