Tabs
====

When there are several languages or options available and the reader will use one and keep using that option throughout the procedure, a tabbed content box is the best way to display this informaiton.

For example:

.. code-block:: none

   .. tabs::

      .. group-tab:: CentOS 7, Ubuntu 16.04/18.04, Debian 8/9

         .. code-block:: shell

            sudo systemctl stop scylla-server

      .. group-tab:: Ubuntu 14.04, Debian 7

         .. code-block:: shell

            sudo service scylla-server stop

      .. group-tab:: Docker

         .. code-block:: shell

            docker exec -it some-scylla supervisorctl stop scylla

         (without stopping *some-scylla* container)

Renders as:

.. tabs::

   .. group-tab:: CentOS 7, Ubuntu 16.04/18.04, Debian 8/9

      .. code-block:: shell

         sudo systemctl stop scylla-server

   .. group-tab:: Ubuntu 14.04, Debian 7

      .. code-block:: shell

         sudo service scylla-server stop

   .. group-tab:: Docker

      .. code-block:: shell

         docker exec -it some-scylla supervisorctl stop scylla

      (without stopping *some-scylla* container)
