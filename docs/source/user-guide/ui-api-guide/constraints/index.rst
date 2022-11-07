===========
Constraints
===========

When analyzing a simulation's results, it may be useful to detect windows where certain conditions are met. Constraints are the Aerie tool for fulfilling that role. A constraint is a condition on activities and resources that must hold through an entire simulation. If a constraint does not hold true at any point in a simulation, this is considered a violation.

Managing Constraints
====================

All constraints are associated with either a mission model or a specific plan. If associated with a model, a constraint will be applied to all plans made with that model. If associated with a plan, it will only applied to the plan, and it will have access to any `external profiles <../external-datasets>`_ associated with the plan as well.

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
     see the sub-pages of this section and the `API documentation for the constraints eDSL <../constraints-edsl-api>`_.


  .. group-tab:: API

     Constraints can be uploaded, updated, and deleted directly using the GraphQL API. See `this section <../graphql-api>`_ for information on the basics of GraphQL. To create a single constraint, send the following mutation:

     .. code-block::

        mutation CreateConstraint($constraint: constraint_insert_input!) {
          createConstraint: insert_constraint_one(object: $constraint) {
            id
          }
        }

     with arguments of the following format:

     .. code-block::

        {
          "constraint": {
            "model_id": number, // required if plan_id is absent
            "plan_id": number, // required if model_id is absent
            "name": string,
            "summary": string, // optional
            "description": string, // optional
            "definition": string
          }
        }

     This will return the new constraint's ID, which you can use to update or delete it.

     To update a single constraint, send the following mutation:

     .. code-block::

        mutation UpdateConstraint($id: Int!, $constraint: constraint_set_input!) {
          updateConstraint: update_constraint_by_pk(
            pk_columns: { id: $id }, _set: $constraint
          ) {
            id
          }
        }

     .. code-block::

        {
          "id": number,
          "constraint": {...} // same input as when creating
        }

     All fields in the constraint data are optional when updating. This will update only the provided
     arguments on the specified constraint.

     Lastly, you can delete a constraint as follows:

     .. code-block::

        mutation DeleteConstraint($id: Int!) {
          deleteConstraint: delete_constraint_by_pk(id: $id) {
            id
          }
        }

     The arguments only need to contain the id: ``{ "id": number }``.




.. toctree::
  :maxdepth: 1

  writing-constraints
  examples
