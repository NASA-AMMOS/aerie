# Headings

While Sphinx supports many layers of headings, we try to keep the hierarchy limited to 4, excluding the title.
If you have the need for further headers, chances are that you need to re-arrange the content.

Use the following convention for headings:

```{eval-rst}
.. tabs::
  
  .. group-tab:: reStructuredText
  
    .. note:: Watch the length of your text vs the markup. It must be at least as long as the text. It can be longer, but shorter will produce an error. Also, if your markup has a line above and below both lines must be the same length.

    .. list-table::
      :widths: 33 33 33
      :header-rows: 1
      
      * - Heading
        - Markup
        - Description
      * - Page Title
        - .. code-block:: rst

            =====
            Title
            =====
        - Has an equal sign above and below the text. Use for the title of the page. It should be used only once per page
      * - H1
        - .. code-block:: rst

            Heading 1
            ---------
        - Has  a dash below the text. Use for the first level heading.
      * - H2
        - .. code-block:: rst

            Heading 2
            =========
        - Has an equal sign below the text. Use for the second level heading.
      * - H3
        - .. code-block:: rst

            Heading 3
            .........
        - Has dots below the text. Use for a third level heading.
      * - H4
        - .. code-block:: rst

            Heading 4
            ^^^^^^^^^
        - Has carats below the text. Use for a fourth level heading. If you need more levels, re-arrange the text.

  
  .. group-tab:: Markdown
  
    .. list-table::
      :widths: 33 33 33
      :header-rows: 1
      
      * - Heading
        - Markup
        - Description
      * - H1 (Page Title)
        - .. code-block:: md

            # Title
        - Has one pound sign before the text. Use for the title of the page. It should be used only once per page
      * - H2
        - .. code-block:: md

            ## Heading 1
        - Has two pound signs before the text. Use for the first level heading.
      * - H3
        - .. code-block:: md

            ### Heading 2
        - Has three pound signs before the text. Use for the second level heading.
      * - H4
        - .. code-block:: md

            #### Heading 3
        - Has four pound signs before the text. Use for a third level heading.
      * - H5
        - .. code-block:: md

            ##### Heading 4
        - Has five pound signs before the text. Use for a fourth level heading. If you need more levels, re-arrange the text.
```




