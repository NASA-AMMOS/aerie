Includes
========

An include directive allows you to include the entire contents of one restructured text file directly into another.
This is the easiest way to control content re-use.

When given an absolute path, the directive takes it as relative to the root of the source directory.
It is Aerie practice to place global include files in the *rst_include* directory.

Literal include
---------------

If the file you want to include is **not** |rst|, you can use the Literal Include directive. This adds the page cited as a code-block.

For example:

.. code-block:: none

   .. literalinclude:: ../conf.py
      :lines: 1-10

Gets the ``conf.py`` file from the parent directory and displays the first 10 lines.

.. literalinclude:: ../../../conf.py
      :lines: 1-10
