===========================
Contributing to the Website
===========================

The Aerie website contains all of the user documentation for installing, maintaining, administering, and developing in Aerie.

For information on how to submit changes to the Aerie docs, including how to test your documentation changes, see :doc:`docs-pr`.

How we write
============

Documentation is written primarily for mission model developers, mission planners, and Aerie administrators.
All documentation is saved and tracked on GitHub.
We have created a :doc:`style guide <writer-handbook>` that breaks down the writing rules.

Prerequisite Software
=====================

In order to build the docs locally, you will need the following software:

* `Python 3.7 <https://www.python.org/downloads/>`_ or later.
* `Poetry 1.12 <https://python-poetry.org/docs/master/>`_ or later.

The backbone of our documentation is written in a mixture of `reStructuredText <https://www.sphinx-doc.org/en/master/usage/restructuredtext/>`_
and `MarkDown <https://myst-parser.readthedocs.io/en/latest/>`_ and is compiled with Sphinx.

For samples of |rst| and |md| markup, see the :doc:`Examples <./examples/index>`.

Previewing changes live
========================

To preview your changes while you are working, run ``make preview`` from the command line in the ``docs`` directory.
If you have previously run ``make preview``, it is recommended to run ``make clean`` first. Navigate to http://127.0.0.1:5500/.
The site will automatically update as you work.

Once you are finished writing documentation, follow the instructions :ref:`here <validating_docs>` to ensure your documentation will
work properly on the published site.


.. toctree::
  :hidden:
  :includehidden:
  :maxdepth: 2

  docs-pr
  writer-handbook
  examples/index
  MyST Parser Docs <https://myst-parser.readthedocs.io/en/latest/>
  ReStructuredText Docs <https://www.sphinx-doc.org/en/master/usage/restructuredtext/basics.html>
