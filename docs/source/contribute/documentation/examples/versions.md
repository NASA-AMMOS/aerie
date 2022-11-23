# Versions

This is an inline directive which should be used when introducing or deprecating a feature

## Syntax

``````{tabs}

````{group-tab} {{rst}}

```{note}
When using these directives a blank line must follow.
```

```{code-block} rst
.. versionadded:: version
```

```{code-block} rst
.. versionchanged:: version
```

```{code-block} rst
.. deprecated:: version
```

````

`````{group-tab} {{md}}

````{code-block} md

```{versionadded} version
```

````

````{code-block} md

```{versionchanged} version
```

````

````{code-block} md

```{deprecated} version
```

````

`````

``````

## Usage

```{versionadded} 1.1
```

```{versionchanged} 2018.1
```

```{deprecated} 2.0
```

