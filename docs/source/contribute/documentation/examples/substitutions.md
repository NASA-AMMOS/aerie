# Substitutions

Substitutions are variables. They are declared in any document and defined in the ``conf.py`` file, ``rst_prolog`` setting.

```{caution}
 Do not use substitutions in {{rst}} headings. The text that replaces the variable may be longer than the line that is over or below the text and this will produce an error.
```

## List of substitutions

The following substitutions are available by default:

``````{tabs}

````{group-tab} {{rst}}

* `|v|` for {{v}}
* `|x|` for {{x}}
* `|rst|` for {{rst}}
* `|md|` for {{md}}
````

````{group-tab} {{md}}

* `{{v}}` for {{v}}
* `{{x}}` for {{x}}
* `{{rst}}` for {{rst}}
* `{{md}}` for {{md}}

```{note}
Substitutions do not work in [eval-rst](eval-rst.md), [code-block](code-blocks.md), or [code-tab](tabs.md#code-tabs) directives.
```

https://myst-parser.readthedocs.io/en/latest/syntax/optional.html#substitutions-with-jinja2
````

``````


## Adding substitutions

Additional substitutions can be added to a specific page. If any added substitutions share a name with one in `conf.py`, the page's substitution will override the one in `conf.py`.

`````{tabs}

````{group-tab} {{rst}}

At any point outside a directive, declare a new substitution using the following syntax:

```rst
.. |Substitution Name| directive:: data
```

For example,

```rst
.. |rst| replace:: reStructuredText
```

For more information, see [DocUtils](https://docutils.sourceforge.io/docs/ref/rst/restructuredtext.html#substitution-definitions).
````

````{group-tab} {{md}}

At the top of the page, declare the new substitutions in [front matter](https://myst-parser.readthedocs.io/en/latest/syntax/syntax.html#syntax-frontmatter) using the following syntax:

```yaml
---
myst:
  substitutions:
    Substitution Name: Replacement Content
---
```

For example,

```yaml
---
myst:
  substitutions:
    x: <i class="inline-icon fa fa-times" aria-hidden="true"></i>
---
```

For more information, see [the MyST Docs](https://myst-parser.readthedocs.io/en/latest/syntax/optional.html#substitutions-with-jinja2).
````
`````

## Substitutions within code blocks

In **{{rst}} only**, it is possible to use substitutions within [code blocks](code-blocks.md) by passing the option `substitutions` to the `code-block` directive.

For example:

```{code-block} rst

.. code-block:: rst
  :substitutions:

  |rst|
```

Renders as:

```{code-block} rst
reStructuredText
```
