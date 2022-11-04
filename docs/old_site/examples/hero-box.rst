Hero box
========

A custom directive to create a header on the root ``index.rst`` file.

.. warning:: Do not use the ``hero-box`` on the subordinate ``index.rst`` files.

Syntax
------

.. code-block:: rst

   .. hero-box::
      <options>

      <text>

Options
-------

The ``hero-box`` directive supports the following options:

.. list-table::
  :widths: 20 20 10 20 30
  :header-rows: 1

  * - Option
    - Type
    - Required
    - Example Value
    - Description
  * - ``title``
    - string
    - |v|
    - Lorem ipsum
    - Hero box title.
  * - ``class``
    - string
    -
    -
    - Custom CSS class.
  * - ``button_icon``
    - string
    -
    - fa fa-home
    - A list of CSS classes to render an icon, separated by comma or space.
  * - ``button text``
    - string
    -
    - Lorem ipsum
    - Text for the call to action.
  * - ``button_url``
    - string
    -
    -
    - Relative link or external URL for the call to action. Do not use leading and trailing ("/") symbols to define relative links. (e.g. instead of ``/getting-started/``, use ``getting-started``).
  * - ``image``
    - string
    -
    - /_static/img/mascots/scylla-enterprise.svg
    - Path to the image. The image should be located in the project's ``_static`` folder.
  * - ``search_box``
    - flag
    -
    -
    - If set, displays the site's search box.

Usage
-----

Default
.......

Using:

.. code-block:: rst

    .. hero-box::
        :title: Lorem Ipsum

        Lorem ipsum dolor sit amet, consectetur adipiscing elit.

Results in:

.. hero-box::
    :title: Lorem Ipsum

    Lorem ipsum dolor sit amet, consectetur adipiscing elit.

Hero box with image
...................

Using:

.. code-block:: rst

    .. hero-box::
        :title: Lorem Ipsum
        :image: /_static/img/mascots/scylla-enterprise.svg

        Lorem ipsum dolor sit amet, consectetur adipiscing elit.

Results in:

.. hero-box::
    :title: Lorem Ipsum
    :image: /_static/img/mascots/scylla-enterprise.svg

    Lorem ipsum dolor sit amet, consectetur adipiscing elit.

Hero box with search box
........................

Using:

.. code-block:: rst

  .. hero-box::
      :title: Lorem Ipsum
      :search_box:

      Lorem ipsum dolor sit amet, consectetur adipiscing elit.

Results in:

.. hero-box::
    :title: Lorem Ipsum
    :search_box:

    Lorem ipsum dolor sit amet, consectetur adipiscing elit.

Hero box with button
....................

Using:

.. code-block:: rst

    .. hero-box::
        :title: Lorem Ipsum
        :image: /_static/img/mascots/scylla-enterprise.svg
        :button_icon: fa fa-github
        :button_url: #
        :button_text: Project Name

        Lorem ipsum dolor sit amet, consectetur adipiscing elit.

Results in:

.. hero-box::
    :title: Lorem Ipsum
    :image: /_static/img/mascots/scylla-enterprise.svg
    :button_icon: fa fa-github
    :button_url: #
    :button_text: Project Name

    Lorem ipsum dolor sit amet, consectetur adipiscing elit.
