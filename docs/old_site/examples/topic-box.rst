Topic box
=========

A custom directive that creates graphical boxes (cards) on the root ``index.rst`` file.

.. warning:: Do not use the ``topic-box`` on subordinate ``index.rst`` files.

Syntax
------

.. code-block:: rst

   .. topic-box::
      <options>

      <text>

Options
-------

The ``topic-box`` directive supports the following options:

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
    - Topic box title.
  * - ``class``
    - string
    -
    -
    - Custom CSS class.
  * - ``icon``
    - string
    -
    - fa fa-home
    - A list of CSS classes to render icons, separated by comma or space.
  * - ``image``
    - string
    -
    - /_static/img/mascots/scylla-enterprise.svg
    - Path to the image. The image should be located in the project's ``_static`` folder.
  * - ``link``
    - string
    -
    - getting-started
    - Relative link or external URL for the call to action. Do not use leading and trailing ("/") symbols to define relative links. (e.g. instead of ``/getting-started/``, use ``getting-started``).
  * - ``anchor``
    - string
    -
    - Getting Started >
    - Text for the call to action.

Usage
-----

Topic with icon
...............

Using:

.. code-block:: rst

    .. topic-box::
        :title: Lorem Ipsum
        :icon: fa fa-github
        :link: #
        :anchor: Lorem ipsum

        Lorem ipsum dolor sit amet, consectetur adipiscing elit.

Results in:

.. topic-box::
    :title: Lorem Ipsum
    :icon: fa fa-github
    :link: #
    :anchor: Lorem ipsum

    Lorem ipsum dolor sit amet, consectetur adipiscing elit.

Topic with image
................

Using:

.. code-block:: rst

    .. topic-box::
        :title: Lorem Ipsum
        :image: /_static/img/mascots/scylla-enterprise.svg
        :link: #
        :anchor: Lorem ipsum

        Lorem ipsum dolor sit amet, consectetur adipiscing elit.

Results in:

.. topic-box::
    :title: Lorem Ipsum
    :image: /_static/img/mascots/scylla-enterprise.svg
    :link: #
    :anchor: Lorem ipsum

    Lorem ipsum dolor sit amet, consectetur adipiscing elit.

Topic with external link
........................

Using:

.. code-block:: rst

    .. topic-box::
        :title: Lorem Ipsum
        :link: https://scylladb.com
        :anchor: Lorem ipsum

        Lorem ipsum dolor sit amet, consectetur adipiscing elit.

Results in:

.. topic-box::
    :title: Lorem Ipsum
    :link: https://scylladb.com
    :anchor: Lorem ipsum

    Lorem ipsum dolor sit amet, consectetur adipiscing elit.


Topic with horizontal scroll (mobile)
.....................................

Using:

.. code-block::

    .. raw:: html

        <div class="topics-grid topics-grid--scrollable grid-container full">

        <div class="grid-x grid-margin-x hs">

    .. topic-box::
        :title: Lorem ipsum
        :link: scylla-cloud
        :class: large-4
        :anchor: Lorem ipsum

        Lorem ipsum dolor sit amet, consectetur adipiscing elit.

    .. topic-box::
        :title: Lorem ipsum
        :link: scylla-cloud
        :class: large-4
        :anchor: Lorem ipsum

        Lorem ipsum dolor sit amet, consectetur adipiscing elit.

    .. topic-box::
        :title: Lorem ipsum
        :link: scylla-cloud
        :class: large-4
        :anchor: Lorem ipsum

        Lorem ipsum dolor sit amet, consectetur adipiscing elit.

    .. raw:: html

        </div></div>


Results in:

.. raw:: html

    <div class="topics-grid topics-grid--scrollable grid-container full">

    <div class="grid-x grid-margin-x hs">

.. topic-box::
    :title: Lorem ipsum
    :link: scylla-cloud
    :class: large-4
    :anchor: Lorem ipsum

    Lorem ipsum dolor sit amet, consectetur adipiscing elit.

.. topic-box::
    :title: Lorem ipsum
    :link: scylla-cloud
    :class: large-4
    :anchor: Lorem ipsum

    Lorem ipsum dolor sit amet, consectetur adipiscing elit.

.. topic-box::
    :title: Lorem ipsum
    :link: scylla-cloud
    :class: large-4
    :anchor: Lorem ipsum

    Lorem ipsum dolor sit amet, consectetur adipiscing elit.

.. raw:: html

    </div></div>

Product topic
.............

Using:

.. code-block:: rst

    .. topic-box::
        :title: Lorem Ipsum
        :link: #
        :image: /_static/img/mascots/scylla-enterprise.svg
        :class: topic-box--product

    Lorem ipsum dolor sit amet, consectetur adipiscing elit.

Results in:

.. topic-box::
    :title: Lorem Ipsum
    :link: #
    :image: /_static/img/mascots/scylla-enterprise.svg
    :class: topic-box--product

    Lorem ipsum dolor sit amet, consectetur adipiscing elit.


Topic grid
..........

Create powerful, multi-device, topic box grids using Foundation's 12-column grid system.
To make the columns wider or smaller, you can use the option ``:class:``.
For example, ``:class: large-3`` means that the topic box will take 3 out of 12 columns in desktop devices.

For more information, see `The Grid System <https://get.foundation/sites/docs/grid.html>`_.

Using:

.. code-block:: rst

    .. raw:: html

        <div class="topics-grid topics-grid--products">

            <h2 class="topics-grid__title">Lorem Ipsum</h2>
            <p class="topics-grid__text">Lorem ipsum dolor sit amet, consectetur adipiscing elit.</p>

            <div class="grid-container full">
                <div class="grid-x grid-margin-x">

    .. topic-box::
        :title: Lorem Ipsum
        :link: #
        :image: /_static/img/mascots/scylla-enterprise.svg
        :class: topic-box--product,large-3,small-6

        Lorem ipsum dolor sit amet, consectetur adipiscing elit.

    .. topic-box::
        :title: Lorem Ipsum
        :link: #
        :image: /_static/img/mascots/scylla-enterprise.svg
        :class: topic-box--product,large-3,small-6

        Lorem ipsum dolor sit amet, consectetur adipiscing elit.

    .. topic-box::
        :title: Lorem Ipsum
        :link: #
        :image: /_static/img/mascots/scylla-enterprise.svg
        :class: topic-box--product,large-3,small-6

        Lorem ipsum dolor sit amet, consectetur adipiscing elit.

    .. topic-box::
        :title: Lorem Ipsum
        :link: #
        :image: /_static/img/mascots/scylla-enterprise.svg
        :class: topic-box--product,large-3,small-6

        Lorem ipsum dolor sit amet, consectetur adipiscing elit.

    .. topic-box::
        :title: Lorem Ipsum
        :link: #
        :image: /_static/img/mascots/scylla-enterprise.svg
        :class: topic-box--product,large-3,small-6

        Lorem ipsum dolor sit amet, consectetur adipiscing elit.

    .. topic-box::
        :title: Lorem Ipsum
        :link: #
        :image: /_static/img/mascots/scylla-enterprise.svg
        :class: topic-box--product,large-3,small-6

        Lorem ipsum dolor sit amet, consectetur adipiscing elit.

    .. topic-box::
        :title: Lorem Ipsum
        :link: #
        :image: /_static/img/mascots/scylla-enterprise.svg
        :class: topic-box--product,large-3,small-6

        Lorem ipsum dolor sit amet, consectetur adipiscing elit.

    .. topic-box::
        :title: Lorem Ipsum
        :link: #
        :image: /_static/img/mascots/scylla-enterprise.svg
        :class: topic-box--product,large-3,small-6

        Lorem ipsum dolor sit amet, consectetur adipiscing elit.

    .. raw:: html

        </div></div></div>


Results in:

.. raw:: html

    <div class="topics-grid topics-grid--products">

        <h2 class="topics-grid__title">Lorem Ipsum</h2>
        <p class="topics-grid__text">Lorem ipsum dolor sit amet, consectetur adipiscing elit.</p>

        <div class="grid-container full">
            <div class="grid-x grid-margin-x">

.. topic-box::
    :title: Lorem Ipsum
    :link: #
    :image: /_static/img/mascots/scylla-enterprise.svg
    :class: topic-box--product,large-3,small-6

    Lorem ipsum dolor sit amet, consectetur adipiscing elit.

.. topic-box::
    :title: Lorem Ipsum
    :link: #
    :image: /_static/img/mascots/scylla-enterprise.svg
    :class: topic-box--product,large-3,small-6

    Lorem ipsum dolor sit amet, consectetur adipiscing elit.

.. topic-box::
    :title: Lorem Ipsum
    :link: #
    :image: /_static/img/mascots/scylla-enterprise.svg
    :class: topic-box--product,large-3,small-6

    Lorem ipsum dolor sit amet, consectetur adipiscing elit.

.. topic-box::
    :title: Lorem Ipsum
    :link: #
    :image: /_static/img/mascots/scylla-enterprise.svg
    :class: topic-box--product,large-3,small-6

    Lorem ipsum dolor sit amet, consectetur adipiscing elit.

.. topic-box::
    :title: Lorem Ipsum
    :link: #
    :image: /_static/img/mascots/scylla-enterprise.svg
    :class: topic-box--product,large-3,small-6

    Lorem ipsum dolor sit amet, consectetur adipiscing elit.

.. topic-box::
    :title: Lorem Ipsum
    :link: #
    :image: /_static/img/mascots/scylla-enterprise.svg
    :class: topic-box--product,large-3,small-6

    Lorem ipsum dolor sit amet, consectetur adipiscing elit.

.. topic-box::
    :title: Lorem Ipsum
    :link: #
    :image: /_static/img/mascots/scylla-enterprise.svg
    :class: topic-box--product,large-3,small-6

    Lorem ipsum dolor sit amet, consectetur adipiscing elit.

.. topic-box::
    :title: Lorem Ipsum
    :link: #
    :image: /_static/img/mascots/scylla-enterprise.svg
    :class: topic-box--product,large-3,small-6

    Lorem ipsum dolor sit amet, consectetur adipiscing elit.

.. raw:: html

    </div></div></div>
