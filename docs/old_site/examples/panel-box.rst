Panel box
=========

A custom directive to creates boxes on subordinate ``index.rst`` files.

.. warning:: Do not use the panel-box on the root ``index.rst``.

Syntax
------

.. code-block:: rst

   .. panel-box::
      <options>

      <text>

Options
-------

The ``topic-box`` directive supports the following options:

.. list-table::
  :widths: 20 20 10 20 30
  :header-rows: 1

  * - Option
    - Type
    - Required
    - Example Value
    - Description
  * - ``title``
    - string
    - |v|
    - Lorem ipsum
    - Hero box title.
  * - ``class``
    - string
    -
    -
    - Custom CSS class.

Usage
-----

For example, using:

.. code-block:: rst

   .. panel-box::
      :title: Admin

      Test

Results in:

.. panel-box::
    :title: Admin

    Test
