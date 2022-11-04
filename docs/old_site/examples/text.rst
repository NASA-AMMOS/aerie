Text
====

Paragraph Styles
----------------

A paragraph style is markup which applies to an entire paragraph.
In restructuredText, there is no need to markup body text.
Any text which is not marked up will be treated as body text.

Character Styles (in-line Markup)
---------------------------------

The following markup is used within a line of text.

* Note that you **cannot** mix inline markup types together.
* Use only one type of markup text at a time.
* Use a space before any markup **and** after the text that follows.

.. list-table::
   :widths: 25 25 25 25
   :header-rows: 1

   * - Name
     - Markup
     - Renders as
     - Description
   * - Bold
     - .. code-block:: rst

          **Bold Text**
     - **Bold Text**
     - Use this markup to create boldface text. Note that there are no spaces next to the asterisks.
   * - Italics
     - .. code-block:: rst

          *Italics*
     - *Italics*
     - Use this markup to create italic text. Note that there are no spaces next to the asterisks.
   * - Code
     - .. code-block:: rst

          ``command line text``
     - ``command line text``
     - Use this markup to create command line text. Note that there are two tick marks before and after the command and no spaces next to the tick marks.
