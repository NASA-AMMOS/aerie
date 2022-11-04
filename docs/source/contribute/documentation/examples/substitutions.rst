Substitutions
=============

Substitutions are variables. They are declared in any document and defined in the ``conf.py`` file, ``rst_prolog`` setting.

.. caution:: Do not use substitutions in headings. The reason is the text that replaces the variable may be longer than the line that is over or below the text and this will produce an error.

List of substitutions
---------------------

Our theme can use the following substitutions:

* ``|v|`` for |v|
* ``|x|`` for |x|
* ``|rst|`` for |rst|

Substitutions within code blocks
--------------------------------

To add substitutions within :doc:`Code blocks <code-blocks>`, pass the option ``:substitutions:`` to the ``code-block`` directive.

For example:

.. code-block:: rst

    .. code-block::
      :substitutions:

      |rst|

Renders as:

.. code-block::
  :substitutions:

  |rst|
