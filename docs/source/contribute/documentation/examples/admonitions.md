# Admonitions

Although {{rst}} allows for more, we limit the admonitions we use to one of the following:

```{contents}
---
local:
backlinks: none
---
```

## Note

Use a note to point out something to the reader. This action does not have any risk.

```{eval-rst}
.. tabs:: 
  
  .. code-tab:: rst

    .. note:: Text follows here
  
  .. code-tab:: md
    
    ```{note}
    Text follows here
    ```
```

Renders as:

```{note}
Text follows here
```

## Caution

Use caution if there is any potential risk to data loss or lower performance.

```{eval-rst}
.. tabs:: 
  
  .. code-tab:: rst

    .. caution:: Look out!
  
  .. code-tab:: md
    
    ```{caution}
    Look out!
    ```
```

Renders as

```{caution}
Look out!
```

## Warning

Use warning if there is any potential risk to total data loss or physical danger.

```{eval-rst}
.. tabs:: 
  
  .. code-tab:: rst

    .. warning:: Take care!
  
  .. code-tab:: md
    
    ```{warning}
    Take care!
    ```
```

Renders as:

```{warning}
Take care!
```

## Tip

This is a time-saving or performance enhancing option.

```{eval-rst}
.. tabs:: 
  
  .. code-tab:: rst

    .. tip:: Here's a tip.
  
  .. code-tab:: md
    
    ```{tip}
      Here's a tip.
    ```
```

Renders as:

```{tip}
Here's a tip.
```
