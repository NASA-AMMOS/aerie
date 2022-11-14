# Include

The `include` directive allows you to include the entire contents of one text file directly into another.
This is the easiest way to control content re-use.

When given an absolute path, the directive takes it as relative to the root of the source directory (`nasa-ammos.
It is Aerie practice to place global include files in the *rst_include* directory.

## Syntax

`````{tabs}
````{code-tab} rst
.. include:: relative/path/to/file.rst
  :options:
```

````{code-tab} md
```{include} relative/path/to/file.md
---
options:
---
```
````
`````

```{note}
The above only works for files with the **same** extension as the file being worked on.
```

To parse a Markdown file or snippet within a {{rst}} file, use the `parser` option like so:

```{code-block} rst
.. include:: include.md
  :parser: myst_parser.sphinx_
  :other_options:
```

To parse a {{rst}} file or snippet within a Markdown file, use the {{rst}} syntax wrapped in an [eval-rst](./eval-rst.md) directive:
````{code-block} none
```{eval-rst}
.. include:: include.rst
  :options:
```
````

For more information on `include`, see [DocUtils](https://docutils.sourceforge.io/docs/ref/rst/directives.html#include).

## Including Snippets

Oftentimes, we only want to include part of a file, for example, inserting a given API query from [API Examples](../../../user-guide/ui-api-guide/api-examples.rst) onto another page.

By surrounding the snippet with comments like so:

````{tabs}
```{code-tab} rst
.. begin [unique title describing snippet]
[...snippet...]
.. end [unique title describing snippet]
```

```{code-tab} md
% begin [unique title describing snippet]
[...snippet...]
% end [unique title describing snippet]
```
````

It is possible to include only that snippet in other files using the `start-after` and `end-before` options:

`````{tabs}
````{code-tab} rst
.. include:: relative/path/to/file.rst
  :start-after: begin [unique title describing snippet]
  :end-before: end [unique title describing snippet]
```
````

````{code-tab} md
```{include} relative/path/to/file.md
---
start-after: [unique title describing snippet]
end-before: [unique title describing snippet]
---
```
````
`````

## LiteralInclude

If you want to insert a file as a code-block, or insert a file with an extension other than `.md` or `.rst`, use the `literalinclude` directive.

For example:

`````{tabs}
````{code-tab} rst
.. literalinclude:: ../../../conf.py
  :lines: 1-10
  :linenos:
````
````{code-tab} md
```{literalinclude} ../../../conf.py
---
lines: 1-10
linenos:
---
```
````
`````

Displays the first ten lines of the `conf.py`.

```{literalinclude} ../../../conf.py
---
lines: 1-10
linenos:
---
```

For more information on `literalinclude`, see [the Sphinx docs](https://www.sphinx-doc.org/en/master/usage/restructuredtext/directives.html#directive-literalinclude).
