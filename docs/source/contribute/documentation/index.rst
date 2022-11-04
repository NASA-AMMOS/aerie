===========================
Contributing to the Website
===========================

The Aerie website contains all of the user documentation for installing, maintaining, administering, and developing in Aerie.

How we write
============

Documentation is written primarily for mission model developers, mission planners, and Aerie administrators.
All documentation is saved and tracked on GitHub.
We have created a style guide that breaks down the writing rules.

Prerequisite Software
---------------------
In order to build the docs locally, you will need the following software:

* `Python 3.7 <https://www.python.org/downloads/>`_ or later.
* `Poetry 1.12 <https://python-poetry.org/docs/master/>`_ or later.

The backbone of our documentation is written in a mixture of `reStructuredText <https://www.sphinx-doc.org/en/master/usage/restructuredtext/>`_
and `MarkDown <https://myst-parser.readthedocs.io/en/latest/>`_ and is compiled with Sphinx.
See the :doc:`Aerie Cheat Sheet <./examples/index>` for samples of reStructeredText markup.


.. toctree::
   :hidden:
   :includehidden:
   :maxdepth: 2

   examples/index
   writer-handbook
   docs-pr
   MyST Parser Docs <https://myst-parser.readthedocs.io/en/latest/>
   ReStructuredText Docs <https://www.sphinx-doc.org/en/master/usage/restructuredtext/basics.html>
