# Hero box

A custom directive to create a header on the root ``index.rst`` file.

## Syntax

`````{tabs}
````{code-tab} rst    
.. hero-box::
  <options>
      
  <text>
````
````{code-tab} md 
```{hero-box}
---
<options>
---
  
<text>
```
````
`````

## Options

The ``hero-box`` directive supports the following options:

| Option          |  Type  | Required | Example Value                                   | Description                                                                                                                                                                                    |
|-----------------|:------:|:--------:|-------------------------------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ``title``       | string |  {{v}}   | Lorem Ipsum                                     | Hero box title.                                                                                                                                                                                |
| ``class``       | string |          |                                                 | Custom CSS class.                                                                                                                                                                              |
| ``button_icon`` | string |          | fa fa-home                                      | A list of CSS classes to render an icon, separated by commas or spaces.                                                                                                                        |
| ``button_text`` | string |          | Lorem ipsum                                     | Text fro the call to action.                                                                                                                                                                   |
| ``button_url``  | string |          |                                                 | Relative link or external URL for the call to action. Do not use leading and trailing ("/") symbols to define relative links. (e.g. instead of ``/getting-started/``, use ``getting-started``) |
| ``image``       | string |          | ../../../_static/img/logos/aerie-logo-light.svg | Relative path to the image.                                                                                                                                                                    |
| ``search_box``  |  flag  |          |                                                 | If present, displays the site's search box.                                                                                                                                                    |

## Usage

### Default

Using:

`````{tabs}
````{code-tab} rst
.. hero-box::
  :title: Lorem Ipsum
  
  Lorem ipsum dolor sit amet, consectetur adipiscing elit.
````
````{code-tab} md
```{hero-box}
---
title: Lorem Ipsum
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```
````
`````

Results in:

```{hero-box}
---
title: Lorem Ipsum
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```

### Hero box with image

Using:

`````{tabs}
````{code-tab} rst
.. hero-box::
  :title: Lorem Ipsum
  :image: /_static/img/logos/aerie-logo-light.svg
  
  Lorem ipsum dolor sit amet, consectetur adipiscing elit.
````
````{code-tab} md
```{hero-box}
---
title: Lorem Ipsum
image: /_static/img/logos/aerie-logo-light.svg
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```
````
`````

Results in:

```{hero-box}
---
title: Lorem Ipsum
image: /_static/img/logos/aerie-logo-light.svg
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```

### Hero box with search box

Using:

`````{tabs}
````{code-tab} rst
.. hero-box::
      :title: Lorem Ipsum
      :search_box:

      Lorem ipsum dolor sit amet, consectetur adipiscing elit.
````
````{code-tab} md
```{hero-box}
---
title: Lorem Ipsum
search_box:
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```
````
`````

Results in:

```{hero-box}
---
title: Lorem Ipsum
search_box:
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```

### Hero box with button

Using:

````{eval-rst}
.. tabs::
  
  .. code-tab:: rst
    
    .. hero-box::
        :title: Lorem Ipsum
        :image: /_static/img/logos/aerie-logo-light.svg
        :button_icon: fa fa-github
        :button_url: #
        :button_text: Project Name

        Lorem ipsum dolor sit amet, consectetur adipiscing elit.__
  .. code-tab:: md
  
    ```{hero-box}
    ---
    title: Lorem Ipsum
    image: /_static/img/logos/aerie-logo-light.svg
    button_icon: fa fa-github
    button_url: "#"
    button_text: Project Name
    ---
    
    Lorem ipsum dolor sit amet, consectetur adipiscing elit.
    ```
````

Results in:

```{hero-box}
---
title: Lorem Ipsum
image: /_static/img/logos/aerie-logo-light.svg
button_icon: fa fa-github
button_url: "#"
button_text: Project Name
---

Lorem ipsum dolor sit amet, consectetur adipiscing elit.
```
