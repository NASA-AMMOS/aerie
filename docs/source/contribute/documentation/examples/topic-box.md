# Topic box

A custom directive that creates graphical boxes (cards) on the root ``index.rst`` file.

```{warning}
Do not use the ``topic-box`` on subordinate ``index.rst`` files.
```

## Syntax

`````{tabs}

````{code-tab} rst

.. topic-box::
  <options>
  
  Text
````

````{code-tab} md

```{topic-box}
---
<options>
---

Text
```
````

`````


## Options

The ``topic-box`` directive supports the following options:

```{list-table}
---
widths: 20 20 10 20 30
header-rows: 1
---

  * - Option
    - Type
    - Required
    - Example Value
    - Description
  * - `title`
    - string
    - {{v}}
    - Lorem ipsum
    - Topic box title.
  * - ``class``
    - string
    -
    -
    - Custom CSS class.
  * - `icon`
    - string
    -
    - fa fa-home
    - A list of CSS classes to render icons, separated by comma or space.
  * - `image`
    - string
    -
    - /_static/img/logos/aerie-logo-light.svg
    - Path to the image. The image should be located in the project's ``_static`` folder.
  * - `link`
    - string
    -
    - getting-started
    - Relative link or external URL for the call to action. Do not use leading and trailing ("/") symbols to define relative links. (e.g. instead of ``/getting-started/``, use ``getting-started``).
  * - `anchor`
    - string
    -
    - Getting Started >
    - Text for the call to action.
```

## Usage

```{contents}
---
local:
---
```

### Topic with icon

Using:

``````{tabs}
````{code-tab} rst

.. topic-box::
  :title: Lorem Ipsum
  :icon: fa fa-github
  :link: #
  :anchor: Lorem ipsum

  Lorem ipsum dolor sit amet, consectetur adipiscing elit.
````

````{code-tab} md

```{topic-box}
---
title: Lorem Ipsum
icon: fa fa-github
link: "#"
anchor: Lorem ipsum
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```
````
``````

Results in:

```{topic-box}
---
title: Lorem Ipsum
icon: fa fa-github
link: "#"
anchor: Lorem ipsum
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```

###  Topic with image

Using:

`````{tabs}

````{code-tab} rst
.. topic-box::
  :title: Lorem Ipsum
  :image: /_static/img/logos/aerie-logo-light.svg
  :link: #
  :anchor: Lorem ipsum

  Lorem ipsum dolor sit amet, consectetur adipiscing elit.
````

````{code-tab} md

```{topic-box}
---
title: Lorem Ipsum
image: /_static/img/logos/aerie-logo-light.svg
link: "#"
anchor: Lorem ipsum
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```

````

`````

Results in:

```{topic-box}
---
title: Lorem Ipsum
image: /_static/img/logos/aerie-logo-light.svg
link: "#"
anchor: Lorem ipsum
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```

### Topic with external link

Using:

`````{tabs}

````{code-tab} rst

.. topic-box::
  :title: Lorem Ipsum
  :link: https://nasa-ammos.github.io/aerie/stable/
  :anchor: Lorem ipsum
  
  Lorem ipsum dolor sit amet, consectetur adipiscing elit.
````

````{code-tab} md

```{topic-box}
---
title: Lorem Ipsum
link: https://nasa-ammos.github.io/aerie/stable/
anchor: Lorem ipsum
---
  
Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```
````

`````

Results in:

```{topic-box}
---
title: Lorem Ipsum
link: https://nasa-ammos.github.io/aerie/stable/
anchor: Lorem ipsum
---
  
Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```

### Topic with horizontal scroll (mobile)

Using:

`````{tabs}

````{code-tab} rst

.. raw:: html

  <div class="topics-grid topics-grid--scrollable grid-container full">
    <div class="grid-x grid-margin-x hs">

.. topic-box::
  :title: Lorem ipsum
  :link: https://nasa-ammos.github.io/aerie/stable/
  :class: large-4
  :anchor: Lorem ipsum
  
  Lorem ipsum dolor sit amet, consectetur adipiscing elit.

.. topic-box::
  :title: Lorem ipsum
  :link: https://nasa-ammos.github.io/aerie/stable/
  :class: large-4
  :anchor: Lorem ipsum
  
  Lorem ipsum dolor sit amet, consectetur adipiscing elit.

.. topic-box::
  :title: Lorem ipsum
  :link: https://nasa-ammos.github.io/aerie/stable/
  :class: large-4
  :anchor: Lorem ipsum
  
  Lorem ipsum dolor sit amet, consectetur adipiscing elit.

.. raw:: html

  </div></div>

````

````{code-tab} md

<div class="topics-grid topics-grid--scrollable grid-container full">
  <div class="grid-x grid-margin-x hs">

```{topic-box}
---
title: Lorem ipsum
link: https://nasa-ammos.github.io/aerie/stable/
class: large-4
anchor: Lorem ipsum
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```

```{topic-box}
---
title: Lorem ipsum
link: https://nasa-ammos.github.io/aerie/stable/
class: large-4
anchor: Lorem ipsum
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```

```{topic-box}
---
title: Lorem ipsum
link: https://nasa-ammos.github.io/aerie/stable/
class: large-4
anchor: Lorem ipsum
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```


</div></div>
```

`````

Results in:

<div class="topics-grid topics-grid--scrollable grid-container full">
  <div class="grid-x grid-margin-x hs">

```{topic-box}
---
title: Lorem ipsum
link: https://nasa-ammos.github.io/aerie/stable/
class: large-4
anchor: Lorem ipsum
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```

```{topic-box}
---
title: Lorem ipsum
link: https://nasa-ammos.github.io/aerie/stable/
class: large-4
anchor: Lorem ipsum
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```

```{topic-box}
---
title: Lorem ipsum
link: https://nasa-ammos.github.io/aerie/stable/
class: large-4
anchor: Lorem ipsum
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```

</div></div>

### Product topic

Using:

`````{tabs}

````{code-tab} rst

.. topic-box::
    :title: Lorem Ipsum
    :link: #
    :image: /_static/img/logos/aerie-logo-light.svg
    :class: topic-box--product

    Lorem ipsum dolor sit amet, consectetur adipiscing elit.
````

````{code-tab} md

```{topic-box}
---
title: Lorem Ipsum
link: "#"
image: /_static/img/logos/aerie-logo-light.svg
class: topic-box--product
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```

````

`````

Results in:

```{topic-box}
---
title: Lorem Ipsum
link: "#"
image: /_static/img/logos/aerie-logo-light.svg
class: topic-box--product
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```


### Topic grid

Create powerful, multi-device, topic box grids using Foundation's 12-column grid system.
To make the columns wider or smaller, you can use the `class` option.
For example, `class: large-3` means that the topic box will take 3 out of 12 columns in desktop devices.

For more information, see [The Grid System](https://get.foundation/sites/docs/grid.html).

Using:

`````{tabs}

````{code-tab} rst
.. raw:: html

  <div class="topics-grid topics-grid--products">

    <h2 class="topics-grid__title">Lorem Ipsum</h2>
    <p class="topics-grid__text">Lorem ipsum dolor sit amet, consectetur adipiscing elit.</p>

    <div class="grid-container full">
      <div class="grid-x grid-margin-x">

.. topic-box::
    :title: Lorem Ipsum
    :link: #
    :image: /_static/img/logos/aerie-logo-light.svg
    :class: topic-box--product,large-3,small-6

    Lorem ipsum dolor sit amet, consectetur adipiscing elit.

.. topic-box::
    :title: Lorem Ipsum
    :link: #
    :image: /_static/img/logos/aerie-logo-light.svg
    :class: topic-box--product,large-3,small-6

    Lorem ipsum dolor sit amet, consectetur adipiscing elit.

.. topic-box::
    :title: Lorem Ipsum
    :link: #
    :image: /_static/img/logos/aerie-logo-light.svg
    :class: topic-box--product,large-3,small-6

    Lorem ipsum dolor sit amet, consectetur adipiscing elit.

.. topic-box::
    :title: Lorem Ipsum
    :link: #
    :image: /_static/img/logos/aerie-logo-light.svg
    :class: topic-box--product,large-3,small-6

    Lorem ipsum dolor sit amet, consectetur adipiscing elit.

.. topic-box::
    :title: Lorem Ipsum
    :link: #
    :image: /_static/img/logos/aerie-logo-light.svg
    :class: topic-box--product,large-3,small-6

    Lorem ipsum dolor sit amet, consectetur adipiscing elit.

.. topic-box::
    :title: Lorem Ipsum
    :link: #
    :image: /_static/img/logos/aerie-logo-light.svg
    :class: topic-box--product,large-3,small-6

    Lorem ipsum dolor sit amet, consectetur adipiscing elit.

.. topic-box::
    :title: Lorem Ipsum
    :link: #
    :image: /_static/img/logos/aerie-logo-light.svg
    :class: topic-box--product,large-3,small-6

    Lorem ipsum dolor sit amet, consectetur adipiscing elit.

.. topic-box::
    :title: Lorem Ipsum
    :link: #
    :image: /_static/img/logos/aerie-logo-light.svg
    :class: topic-box--product,large-3,small-6

    Lorem ipsum dolor sit amet, consectetur adipiscing elit.

.. raw:: html

    </div></div></div>
````

````{code-tab} md

<div class="topics-grid topics-grid--products">

  <h2 class="topics-grid__title">Lorem Ipsum</h2>
  <p class="topics-grid__text">Lorem ipsum dolor sit amet, consectetur adipiscing elit.</p>

  <div class="grid-container full">
    <div class="grid-x grid-margin-x">

```{topic-box}
---
title: Lorem Ipsum
link: "#"
image: /_static/img/logos/aerie-logo-light.svg
class: topic-box--product,large-3,small-6
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```

```{topic-box}
---
title: Lorem Ipsum
link: "#"
image: /_static/img/logos/aerie-logo-light.svg
class: topic-box--product,large-3,small-6
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```

```{topic-box}
---
title: Lorem Ipsum
link: "#"
image: /_static/img/logos/aerie-logo-light.svg
class: topic-box--product,large-3,small-6
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```

```{topic-box}
---
title: Lorem Ipsum
link: "#"
image: /_static/img/logos/aerie-logo-light.svg
class: topic-box--product,large-3,small-6
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```

```{topic-box}
---
title: Lorem Ipsum
link: "#"
image: /_static/img/logos/aerie-logo-light.svg
class: topic-box--product,large-3,small-6
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```

```{topic-box}
---
title: Lorem Ipsum
link: "#"
image: /_static/img/logos/aerie-logo-light.svg
class: topic-box--product,large-3,small-6
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```

```{topic-box}
---
title: Lorem Ipsum
link: "#"
image: /_static/img/logos/aerie-logo-light.svg
class: topic-box--product,large-3,small-6
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```

```{topic-box}
---
title: Lorem Ipsum
link: "#"
image: /_static/img/logos/aerie-logo-light.svg
class: topic-box--product,large-3,small-6
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```

</div></div></div>
````

`````

Results in:

<div class="topics-grid topics-grid--products">

  <h2 class="topics-grid__title">Lorem Ipsum</h2>
  <p class="topics-grid__text">Lorem ipsum dolor sit amet, consectetur adipiscing elit.</p>

  <div class="grid-container full">
    <div class="grid-x grid-margin-x">

```{topic-box}
---
title: Lorem Ipsum
link: "#"
image: /_static/img/logos/aerie-logo-light.svg
class: topic-box--product,large-3,small-6
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```

```{topic-box}
---
title: Lorem Ipsum
link: "#"
image: /_static/img/logos/aerie-logo-light.svg
class: topic-box--product,large-3,small-6
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```

```{topic-box}
---
title: Lorem Ipsum
link: "#"
image: /_static/img/logos/aerie-logo-light.svg
class: topic-box--product,large-3,small-6
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```

```{topic-box}
---
title: Lorem Ipsum
link: "#"
image: /_static/img/logos/aerie-logo-light.svg
class: topic-box--product,large-3,small-6
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```

```{topic-box}
---
title: Lorem Ipsum
link: "#"
image: /_static/img/logos/aerie-logo-light.svg
class: topic-box--product,large-3,small-6
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```

```{topic-box}
---
title: Lorem Ipsum
link: "#"
image: /_static/img/logos/aerie-logo-light.svg
class: topic-box--product,large-3,small-6
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```

```{topic-box}
---
title: Lorem Ipsum
link: "#"
image: /_static/img/logos/aerie-logo-light.svg
class: topic-box--product,large-3,small-6
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```

```{topic-box}
---
title: Lorem Ipsum
link: "#"
image: /_static/img/logos/aerie-logo-light.svg
class: topic-box--product,large-3,small-6
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```

</div></div></div>
