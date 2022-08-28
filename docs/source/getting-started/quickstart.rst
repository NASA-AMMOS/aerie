============
Quickstart
============

This guide will show you how to create your first documentation page, list it in the table of contents, and preview the site locally.

Prerequisites
-------------

You must have a project cloned locally with the :doc:`documentation toolchain <installation>`. Additionally, you will need to have installed:

- `Python 3.7 <https://www.python.org/downloads/>`_ or later.
- `Poetry 1.12 <https://python-poetry.org/docs/master/>`_ or later.

Step 1: Create a new doc
------------------------

Under the ``docs`` folder (``docs/source`` in some projects), create a new `.rst` file.

For example, we named our file ``my-new-page.rst`` and added the following content:

.. code-block:: restructuredText

    =============
    My first page
    =============

    Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas id risus laoreet libero bibendum pharetra non ut sem. Curabitur in nulla diam.
    Donec scelerisque neque lectus, et fringilla eros vestibulum vel. Suspendisse vitae dolor volutpat, lobortis libero a, commodo mi.
    Aenean pretium neque sit amet erat vulputate laoreet. Mauris dapibus vel dui sit amet bibendum.

.. tip:: If you are not familiar with restructuredText syntax, refer to :doc:`Examples <../examples/index>`.

Step 2: Add the page to the toc tree
-------------------------------------

When creating new pages, you must add the topic to the `toc tree <https://www.sphinx-doc.org/en/master/markup/toctree.html>`_. If you do not you will have an error when compiling.

To add the page to the toc tree:

#. Look in the folder you have created the page and find the ``index.rst`` file which is inside the same directory.
#. Edit the ``toctree`` directive to include the name of the new topic without its extension. For example:

    .. code-block:: restructuredText

        .. toctree::
           :maxdepth: 2

           my-new-page

#. Save the file.

Step 3: Preview the docs locally
--------------------------------

Included in every existing documentation project is a make file.
This file contains scripts that you can run to create a testing environment, compile the docs, and produce a local sandbox (website) to test the rendering of the HTML documentation.

To preview the docs locally:

#. From the command line, run ``make preview`` within the ``docs`` folder.

#. Open ``http://127.0.0.1:5500/`` to preview the generated site with you changes.

From the Make file (located in most projects in the /docs/makefile directory), there are more scripts you can run. For more information, see :doc:`Commands <../commands>`.

Next steps
----------

Do you want to submit your changes? See our :doc:`Docs contributor’s handbook <../contribute/contribute-docs>`.
