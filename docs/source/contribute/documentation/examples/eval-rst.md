(external_eval_rst_anchor)=
# Eval-RST

```{note}
This directive is exclusive to Markdown files.
```

The `eval-rst` directive is used to parse {{rst}} content **as {{rst}}** inside of a {{md}} file. 
Any valid {{rst}} syntax can go inside an `eval-rst` directive. 
Any [anchors](anchor) declared inside the directive can be referenced outside the directive and vice versa.

## Example

````rst
```{eval-rst}
.. _internal_eval_rst_anchor:

Interior Header
...............

:ref:`This internal text <internal_eval_rst_anchor>` links to an anchor defined inside this directive.

:ref:`This internal text <external_eval_rst_anchor>` links to an anchor defined outside this directive.
```

### Exterior Header

[This external text](internal_eval_rst_anchor) links to an anchor defined inside the earlier directive.

[This external text](external_eval_rst_anchor) links to an anchor defined outside the earlier directive.

````

Renders as:

```{eval-rst}
.. _internal_eval_rst_anchor:

Interior Header
...............

:ref:`This internal text <internal_eval_rst_anchor>` links to an anchor defined inside this directive.

:ref:`This internal text <external_eval_rst_anchor>` links to an anchor defined outside this directive.
```

### Exterior Header

[This external text](internal_eval_rst_anchor) links to an anchor defined inside the earlier directive.

[This external text](external_eval_rst_anchor) links to an anchor defined outside the earlier directive.
