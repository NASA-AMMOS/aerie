=============
Merging Plans
=============

Create a Merge Request
----------------------

In order to merge changes between two related plans (most commonly between a branch and its parent),
a merge request must first be created between the **source plan** (the one supplying changes) and the **target plan** (the one to receive changes).

.. tabs::

  .. group-tab:: User Interface

    From the child plan, open the dropdown and select ``Create merge request``. Confirm which plan you wish to be the target plan and press ``Create merge request``.

    .. image:: ../images/plan_collaboration/create_merge_rq.png
      :width: 25%

    .. image:: ../images/plan_collaboration/create_merge_rq_modal.png
      :width: 25%

    .. note:: Currently it is only possible to select the parent branch as the target plan via the User Interface. To merge between other related plans, use the API.

  .. group-tab:: API

    A merge request can be created using the following mutation:

    .. include:: ../api-examples.rst
      :start-after: begin create merge request
      :end-before: end create merge request

Withdraw a Merge Request
------------------------

You can withdraw a merge request so long as it is in the ``pending`` state. A withdrawn merge request cannot be used to begin a merge.

.. tabs::

  .. group-tab:: User Interface

    While on the **source plan**, select the ``Merge requests`` text besides the plan's name. Then, press the ``Withdraw`` button next to the merge request you wish to withdraw.

    .. image:: ../images/plan_collaboration/view_outgoing_merge_rqs.png
      :width: 25%

    .. image:: ../images/plan_collaboration/withdraw_merge_rq.png
      :width: 25%

  .. group-tab:: API

    .. include:: ../api-examples.rst
      :start-after: begin withdraw merge request
      :end-before: end withdraw merge request

Begin a Merge
-------------

You can begin a merge from a pending merge request so long as the plan is not currently locked.

.. note::

  Beginning a merge locks the target plan until the merge is either cancelled, denied, or committed.

.. tabs::

  .. group-tab:: User Interface

    While on the **target plan**, select the ``Merge requests`` text beside the list of branches. Then, press ``Review`` next to the merge request you wish to begin.

    .. image:: ../images/plan_collaboration/view_incoming_merge_rqs.png
      :width: 25%

    .. image:: ../images/plan_collaboration/begin_merge.png
      :width: 25%

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
--------------

You can cancel any ``in-progress`` merge.

.. tabs::

  .. group-tab:: User Interface

    From the merge review screen, press the ``Cancel`` button in the bottom-right corner.

    .. image:: ../images/plan_collaboration/cancel_merge.png
      :width: 25%

  .. group-tab:: API

    .. include:: ../api-examples.rst
      :start-after: begin cancel merge
      :end-before: end cancel merge

Resolving Conflicts
-------------------

Before a merge can be committed, all conflicts must be resolved to either ``source`` or ``target``.

.. tabs::

  .. group-tab:: User Interface

    First, select a conflicting activity from the list. The version of the activity in the source and target plan will appear to the right, with the differing fields highlighted.
    Then, determine which version to keep by pressing either "Keep Source Activity" or "Keep Target Activity".

    .. image:: ../images/plan_collaboration/unresolved_conflict.png
      :width: 25%

    .. image:: ../images/plan_collaboration/resolved_conflict.png
      :width: 25%

  .. group-tab:: API

    .. include:: ../api-examples.rst
      :start-after: begin resolve conflict
      :end-before: end resolve conflict

You can also resolve conflicts in bulk:

.. tabs::

  .. group-tab:: User Interface

    .. image:: ../images/plan_collaboration/resolve_bulk.png
      :width: 25%

  .. group-tab:: API

    .. include:: ../api-examples.rst
      :start-after: begin resolve conflict bulk
      :end-before: end resolve conflict bulk

Deny a Merge
------------

It is possible to deny an in-progress merge, for example, if a request is outdated.
Once a merge has been denied, that request cannot be used to begin a merge.

.. tabs::

  .. group-tab:: User Interface

    From the merge review screen, press the ``Deny Changes`` button in the bottom-right corner.

    .. image:: ../images/plan_collaboration/deny_merge.png
      :width: 25%

  .. group-tab:: API

    .. include:: ../api-examples.rst
      :start-after: begin deny merge
      :end-before: end deny merge

Commit a Merge
--------------

Once all conflicts have been resolved, you can commit a merge.

.. tabs::

  .. group-tab:: User Interface

    From the merge review screen, press the ``Approve Changes`` button in the bottom-right corner.

    .. image:: ../images/plan_collaboration/approve_merge.png
      :width: 25%

  .. group-tab:: API

    .. include:: ../api-examples.rst
      :start-after: begin commit merge
      :end-before: end commit merge
