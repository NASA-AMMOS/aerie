==========
Scheduling
==========


This guide explains how to use the scheduling service with the latest version of Aerie.

The scheduling service allows you to add activities to a plan based on goals that you define.
These goals are defined in `TypeScript <https://www.typescriptlang.org/>`__, using a library provided by Aerie.

.. note::
  This guide assumes some very basic familiarity with programming (terminology like functions,
  arguments, return values, types, and objects, will not be explained), but it does not assume
  the reader has seen TypeScript or JavaScript before.

Prerequisites
-------------

* You will need to have uploaded a Mission Model
* You will need to have created a plan from that mission model


Sections
--------

* To create/modify/delete scheduling goals, please see :doc:`here <scheduling-create-delete>`.

* To run the scheduler once you have created goals, please see :doc:`here <scheduling-specification>`.

* For more details on how to author scheduling goals, please see :doc:`here <scheduling-goals>`.

* For more details on how to write scheduling conditions, which are ways of restricting where the scheduler places activities globally, please see :doc:`here <scheduling-conditions>`.


Topics
------


.. toctree::
  :includehidden:

  scheduling-goals
  scheduling-specification
  scheduling-conditions
  scheduling-create-delete
  examples
