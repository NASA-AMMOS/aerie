===========
Constraints
===========

When analyzing a simulation's results, it may be useful to detect windows where certain conditions are met. Constraints are the Aerie tool for fulfilling that role. A constraint is a condition on activities and resources that must hold through an entire simulation. If a constraint does not hold true at any point in a simulation, this is considered a violation.

Managing Constraints
====================

All constraints are associated with either a mission model or a specific plan. If associated with a model, a constraint will be applied to all plans made with that model.
If associated with a plan, it will only applied to the plan, and it will have access to any :doc:`external profiles <../external-datasets/index>` associated with the plan as well.

.. tabs::
  .. group-tab:: UI

     The UI has both a constraints view and page. Clicking *Constraints* in the top right will show a list of all constraints
     that apply to the current plan. Clicking the pencil "edit" button on a constraint or on the *Constraints* option in
     the top-left dropdown will bring up the constraints editor page.

     On the editor page, you can create, update, and delete constraints on all plans and mission models. When you create a constraint,
     you need to associate it with either a mission model or a plan. The editor will be pre-filled with a template for your constraint:

     .. code-block:: typescript

        export default (): Constraint => {

        }

     You then write some constraint that returns a constraint, and click *save*. For details on how to write constraints,
     see the sub-pages of this section and the :doc:`API documentation for the constraints eDSL <../../../constraints-edsl-api/index>`.


  .. group-tab:: API

     Constraints can be uploaded, updated, and deleted directly using the GraphQL API. See `here <https://graphql.org/learn/>`__ for information on the basics of GraphQL. To create a single constraint, send the following mutation:

     .. include:: ../api-examples.rst
      :start-after: begin create constraint
      :end-before: end create constraint

     with arguments of the following format:

     .. include:: ../api-examples.rst
      :start-after: begin create constraint arguments
      :end-before: end create constraint arguments

     This will return the new constraint's ID, which you can use to update or delete it.

     To update a single constraint, send the following mutation:

     .. include:: ../api-examples.rst
      :start-after: begin update constraint
      :end-before: end update constraint

    with arguments:

     .. include:: ../api-examples.rst
      :start-after: begin update constraint arguments
      :end-before: end update constraint arguments

     All fields in the constraint data are optional when updating. This will update only the provided
     arguments on the specified constraint.

     Lastly, you can delete a constraint as follows:

     .. include:: ../api-examples.rst
      :start-after: begin delete constraint
      :end-before: end delete constraint

    with arguments:

     .. include:: ../api-examples.rst
      :start-after: begin delete constraint arguments
      :end-before: end delete constraint arguments




.. toctree::
  :maxdepth: 1
  :hidden:

  writing-constraints
  examples
