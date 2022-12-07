==================
Plan Collaboration
==================

Aerie supports two forms of collaboration:

1. **Real-time collaboration**, where multiple users can work on the same plan at the same time
2. **Branch-based collaboration**, where users can branch existing plans and later merge their changes into other related plans.

Real-Time Collaboration
-----------------------

Real-time collaboration occurs automatically whenever multiple users have the same plan open.

Branch-Based Collaboration
--------------------------

Branches can be made from any existing plan.
A branched plan will have the same start and end time as its parent plan, as well as a copy of all the activities in the parent at the moment it was branched.
It will not, however, contain any simulation data, scheduling goals, or constraints that were associated with the parent plan.

For information on merging branches, see :doc:`merge-request`.

.. _branching-a-plan:

Branching a Plan
================

.. tabs::

  .. group-tab:: User Interface

    When on a plan, select the drop down from beside the plan's name and select "Create branch".

    .. image:: ../images/plan_collaboration/create_branch_dropdown.png
      :width: 25%

    That will open a modal window where you can enter the new branch's name. Selecting "Create Branch" will open the new branch.

    .. image:: ../images/plan_collaboration/create_branch_modal.png
      :width: 25%

    To return to the parent plan, either click the parent plan's name or select "Open parent plan" from the dropdown.

    .. image:: ../images/plan_collaboration/open_parent_plan.png
      :width: 25%

    To return to a branch from the parent plan, click the ``1 branch`` text from beside the name of the plan.
    Then select the plan from the list of branches.

    .. image:: ../images/plan_collaboration/open_branch_text.png
      :width: 25%

    .. image:: ../images/plan_collaboration/open_branch_modal.png
      :width: 25%

  .. group-tab:: API

    The following mutation can be used to create a branch:

    .. include:: ../api-examples.rst
      :start-after: begin branch plan
      :end-before: end branch plan

    In order to perform scheduling tasks on a branch, it is necessary to first create a scheduling specification.

    .. include:: ../api-examples.rst
      :start-after: begin create scheduling specification
      :end-before: end create scheduling specification input

.. toctree::
  :hidden:
  :includehidden:

  merge-request
