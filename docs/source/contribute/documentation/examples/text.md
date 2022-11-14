# Text

## Paragraph Styles

A paragraph style is markup which applies to an entire paragraph.
In both {{md}} and {{rst}}, there is no need to markup body text.
Any text which is not marked up will be treated as body text.

## Character Styles (in-line Markup)

The following markup is used within a line of text.

* Note that you **cannot** mix inline markup types together.
* Use only one type of markup text at a time.
* Use a space before any markup **and** after the text that follows.

``````{tabs}

`````{group-tab} {{rst}}

````{list-table}
---
widths: 25 25 25 25
header-rows: 1
---

   * - Name
     - Markup
     - Renders as
     - Description
   * - Bold
     - ```{code-block} rst

       **Bold Text**
       ```
     - ```{eval-rst} 
       **Bold Text**
       ```
     - Use this markup to create boldface text. Note that there are no spaces next to the asterisks.
   * - Italics
     - ```{code-block} rst

       *Italics*
       ```
     - ```{eval-rst}
       *Italics*
       ```
     - Use this markup to create italic text. Note that there are no spaces next to the asterisks.
   * - Code
     - ```{code-block} rst

       ``command line text``
       ```
     - ```{eval-rst}
       ``command line text``
       ```
     - Use this markup to create command line text. Note that there are two tick marks before and after the command and no spaces next to the tick marks.
````
`````


`````{group-tab} {{md}}

````{list-table}
---
widths: 25 25 25 25
header-rows: 1
---

   * - Name
     - Markup
     - Renders as
     - Description
   * - Bold
     - ```{code-block} md

       **Bold Text**
       ```
     - **Bold Text**
     - Use this markup to create boldface text. Note that there are no spaces next to the asterisks.
   * - Italics
     - ```{code-block} md

       *Italics*
       ```
     - *Italics*
     - Use this markup to create italic text. Note that there are no spaces next to the asterisks.
   * - Bolded Italics
     - ```{code-block} md
        
        ***Bolded Italics***
        ```
     - ***Bolded Italics***
     - Use this markup to create bolded italic text. Note that there are no spaces next to the asterisks.
   * - Code
     - ```{code-block} md

       `command line text`
       ```
     - `command line text`
     - Use this markup to create command line text. Note that there is one tick mark before and after the command and no spaces next to the tick marks.
````
`````

``````





