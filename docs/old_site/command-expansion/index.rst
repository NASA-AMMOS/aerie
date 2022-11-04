===================
Command Expansion
===================

Aerie command expansion allows planners to generate commands from activity information within an activity plan.
A planner creates custom logic that can be assigned to an activity type. This custom logic is used by Aerie
to generate the commands.

Prerequisites
=============

* An AMPCS Command Dictionary
* Mission Model

Steps
=====

#. :doc:`Upload a Command Dictionary and retrieve the command_libary.ts file <upload-and-generate-command-library-file>`
#. :doc:`Select an activity_type to generate the activity_library.ts file <generate-activity-typescript-library>`
#. :doc:`Setup an IDE to start writing custom logic <expansion-authoring-ide-setup>`
#. :doc:`Upload the custom logic <submit-expansion-logic>`
#. :doc:`Create an Expansion Set <create-an-expansion-set>`
#. :doc:`Expand the plan with the Expansion Set <expand-the-plan>`
#. :doc:`Retrieve the generated commands <get-expanded-commands>`

.. toctree::
  :maxdepth: 2
  :hidden:

  upload-and-generate-command-library-file
  generate-activity-typescript-library
  expansion-authoring-ide-setup
  expansion-logic-api
  submit-expansion-logic
  create-an-expansion-set
  expand-the-plan
  get-expanded-commands
  create-sequence
  link-simulated-activity-to-sequence
  retrieve-seqjson-serialization-of-sequence
  valid-sequence-json-format
  generate-seqjson-from-a-standalone-sequence
