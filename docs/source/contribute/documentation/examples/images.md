# Images and Figures

There are two directives that can be used for images: `image` and `figure`. 
The `figure` directive operates like the `image` directive, except it also allows for an optional caption.

## Images

The syntax of the `image` directive is:

``````{tabs}
`````{group-tab} reStructuredText

```{code-block} rst
.. image:: relative/path/to/image.png
  :options:
```

```{note}
For more information, refer to [the Sphinx docs](https://www.sphinx-doc.org/en/master/usage/restructuredtext/basics.html#images).
```

`````


`````{group-tab} Markdown

````{code-block} md
```{image} relative/path/to/image.png
---
options:
---
```
````

```{note}
For more information, refer to [the MyST docs](https://myst-parser.readthedocs.io/en/latest/syntax/optional.html#syntax-images).
```

```{tip}
Markdown also supports the following syntax to include an image **without** any additional formatting options: 
`![alt-text](relative/path/to/image.png)`
```
`````
``````

For example:

`````{tabs}
```{code-tab} rst
.. image:: images/checkmark.png
  :alt: checkmark
```
````{code-tab} md
```{image} images/checkmark.png
---
alt: checkmark
---
```
````
`````

Renders the image as:

```{image} images/checkmark.png
---
alt: checkmark
---
```

For a complete list of options usable with `image`, refer to [DocUtils](https://docutils.sourceforge.io/docs/ref/rst/directives.html#images).

## Figures

The syntax of the `figure` directive is:

`````{tabs}
````{code-tab} rst
.. figure:: relative/path/to/image.png
  :options:
  
  Caption Text
````
````{code-tab} md
```{figure-md} relative/path/to/image.png
---
options:
---

Caption Text
```
````
`````

For example:

`````{tabs}
```{code-tab} rst
.. figure:: images/checkmark.png
  :alt: checkmark
  
  This captions the above checkmark.
```
````{code-tab} md
```{figure} images/checkmark.png
---
alt: checkmark
---

This captions the above checkmark
```
````
`````

Renders the image as:

```{figure} images/checkmark.png
---
alt: checkmark
---

This captions the above checkmark
```

For a complete list of options usable with `figure`, refer to [DocUtils](https://docutils.sourceforge.io/docs/ref/rst/directives.html#figure).

## Resizing images

You can resize images (and figures) with the option ``width``.
When a reader clicks on an image with this option set, the image opens in a popup in full size.
This is handy when images are too small to be read from the documentation page.

For example:

`````{tabs}
````{code-tab} rst
.. image:: images/checkmark.png
  :alt: checkmark
  :width: 200px

.. figure:: images/diagram.svg
  :alt: diagram
  :width: 200px
````
````{code-tab} md
```{image} images/checkmark.png
---
alt: checkmark
width: 200px
---
```

```{figure} images/diagram.svg
---
alt: diagram
width: 200px
---
```
````
`````

Renders the images as:

```{image} images/checkmark.png
---
alt: checkmark
width: 200px
---
```

```{figure} images/diagram.svg
---
alt: diagram
width: 200px
---
```

Click on the images to view them at full size.











