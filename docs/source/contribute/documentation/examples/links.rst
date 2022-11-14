Links and Cross-Referencing
===========================

While Markdown always use the syntax ``[displayed name](target)`` regardless of whether a ``target`` is an internal link, an external link, or an :ref:`anchor <anchor>`,
|rst| differentiates between four types of link.


.. list-table::
   :widths: 25 25 25 25
   :header-rows: 1

   * - Link Type
     - Markup
     - Renders as
     - Description
   * - External Site
     - .. code-block:: rst

          `External Link <https://github.com>`__
     - `External Link <https://github.com>`__
     - Use this markup to create a link to another site. When rendered it has an arrow pointing out icon. It opens the content in a new tab.
   * - .. _here:

       Internal Anchor
     - .. code-block:: rst

          :ref:`Internal Link <here>`
     - :ref:`Internal Link <here>`
     - This is an internal cross reference. It requires an :ref:`anchor <anchor>`. Content opens in the same tab.
   * - Internal Document
     - .. code-block:: rst

          :doc:`Internal Doc <../index>`
     - :doc:`Internal Doc <../index>`
     - Use this markup to link to another page on the site. A full path to a file is required, but the path can be relative. Content opens in the same tab.
   * - Download Link
     - .. code-block:: rst

          :download:`download <index.rst>`

     - :download:`download <index.rst>`
     - This opens a download window. It is used to help users download software or files.

.. _anchor:

Anchors
-------

Anchors are the recommended way to link between pages in the Aerie documentation.
They allow for easy cross-referencing between Markdown and |rst| files.
In addition, Sphinx will automatically determine the path to the anchor.
This avoids any cross-references being unexpectedly incorrect due to differences in how the site generates in ``preview`` and ``multiversion`` mode.
To create an anchor, use the following syntax, leaving a blank line afterwards:

.. tabs::

  .. group-tab:: reStructuredText

    .. code-block:: rst

      .. _unique_anchor_name:
    .. note:: When declaring an anchor, the underscore before ``unique_anchor_name`` is significant.

  .. group-tab:: Markdown

    .. code-block:: md

      (unique_anchor_name)=

Once an anchor has been created, it can be referenced from any other document by using the following syntax:

.. tabs::

  .. group-tab:: reStructuredText

    .. code-block:: rst

      :ref:`Displayed Text <unique_anchor_name>`

  .. group-tab:: Markdown

    .. code-block:: md

      [Displayed Text](unique_anchor_name)

.. note:: If the anchor being referenced originates from a |rst| file, do *not* include the prefixed underscore.
