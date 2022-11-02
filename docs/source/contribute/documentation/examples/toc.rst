TOC
===

These directives create TOCs automatically

TOC
---

The :abbr:`TOC (Table of Contents)` is automatically generated in sphinx when you build the site.

Each index.rst needs to have a toctree directive in order to build the left side nav menu.

.. code-block:: rst

   .. toctree::
      :maxdepth: 2

For more details, see `toctree documentation <https://www.sphinx-doc.org/en/master/usage/restructuredtext/directives.html#directive-toctree>`_.

Mini-TOC
--------

Every topic which has more than one heading in it needs to have a mini-toc.
The Contents directive creates a mini-TOC using the headings you have in the document.
You can set the level of headings to include in the TOC. The recommended depth is 2 for H1 and H2.


.. code-block:: rst

   .. contents::
      :depth: 2
      :local:
