# Tables

We use the List Table directive for tables.
If possible give your table a name as this helps with SEO and screen readers for Accessibility.
In the width declaration, determine the number of columns and the width for each.

Using:

```````{tabs}
````{group-tab} {{rst}}

```{code-block} rst

.. list-table:: Caption Text
  :widths: 33 33 33
  :header-rows: 1

  * - header name
    - header name
    - header name
  * - body text
    - body text
    - body text
```

For information on the alternative grid-table syntax, see [DocUtils](https://docutils.sourceforge.io/docs/ref/rst/restructuredtext.html#tables).
````

`````{group-tab} {{md}}

````{code-block} md

```{list-table} Caption Text
---
widths: 33 33 33
header-rows: 1
---

* - header name
  - header name
  - header name
* - body text
  - body text
  - body text
```

For information on the traditional Markdown-table syntax, see [this guide](https://www.markdownguide.org/extended-syntax/#tables).
````
`````
```````

Results in:

```{list-table} Caption Text
---
widths: 33 33 33
header-rows: 1
---

* - header name
  - header name
  - header name
* - body text
  - body text
  - body text
```
