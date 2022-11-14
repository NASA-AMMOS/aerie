Links
=====

There are a few links you can use with different purposes.

.. list-table::
   :widths: 25 25 25 25
   :header-rows: 1

   * - Link type
     - Markup
     - Renders as
     - Description
   * - External Link
     - .. code-block:: rst

          `External Link <https://github.com/NASA-AMMOS/aerie>`_
     - `External Link <https://github.com/NASA-AMMOS/aerie>`_
     - Use this markup to create a link to another site or project. When rendered it has an arrow pointing out icon. It opens the content in a new tab.
   * - Internal Cross-reference
     - .. code-block:: rst

          :ref:`Internal Link <here>`
     - :ref:`Internal Link <here>`
     - This is an internal cross reference. It requires a bookmark. Content opens in the same tab.
   * - Internal Cross-reference Bookmark
     - .. code-block:: rst

          .. _here:
     - .. _here:
     - This is an internal cross reference bookmark. It requires an internal cross-reference anchor (above). It does not render, but serves as a point to link to.
   * - Internal Doc Reference
     - .. code-block:: rst

          :doc:`Internal Doc <../index>`
     - :doc:`Internal Doc <../index>`
     - This is an internal doc cross reference. it looks for a file. A full path is required.
   * - Download Link
     - .. code-block:: rst

          :download:`download <index.rst>`

     - :download:`download <index.rst>`
     - This opens a download window. It is used to help users download software or files.
