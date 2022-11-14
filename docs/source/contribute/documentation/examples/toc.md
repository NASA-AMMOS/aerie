# Table of Contents

The {abbr}`TOC (Table of Contents)` is automatically generated in sphinx when you build the site.

Each index.rst needs to have a `toctree` directive in order to build the left side nav menu.

## Syntax

`````{tabs}

````{code-tab} rst

.. toctree::
  :maxdepth: 2
  :hidden:
````

````{code-tab} md

```{toctree}
---
maxdepth: 2
hidden:
---
```
````

`````

The `hidden` flag hides the TOCTree, but still generates it for the page. Remove it to display the TOCTree on the page.

For more details, see [the toctree documentation](https://www.sphinx-doc.org/en/master/usage/restructuredtext/directives.html#directive-toctree).

## Usage

## Mini-TOC

Every topic which has more than one heading in it can have a mini-toc.
The Contents directive creates a mini-TOC using the headings you have in the document.
You can set the level of headings to include in the TOC. The recommended depth is 2 for H1 and H2.

For example, this mini-TOC:
```{contents} Mini-TOC
---
depth: 2
local:
---
```

is generated from the following syntax:

### Syntax

`````{tabs}

````{code-tab} rst

.. contents:: Mini-TOC
  :depth: 2
  :local:
````

````{code-tab} md

```{contents} Mini-TOC
---
depth: 2
local:
---
```
````
`````

For more details, see [DocUtils](https://docutils.sourceforge.io/docs/ref/rst/directives.html#table-of-contents).


