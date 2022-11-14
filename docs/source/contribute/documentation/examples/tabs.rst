Tabs
====

When there are several languages or options available and the reader will use one and keep using that option throughout the procedure, a tabbed content box is the best way to display this information.

For example:

.. code-block:: none

  .. tabs::

    .. group-tab:: MacOS (BSD sed)

      To replace every instance of the word "Hello" with the word "World" in a file using BSD sed, use the following command:

      .. code-block:: shell

         sed -i '' 's/Hello/World/g' index.rst

    .. group-tab:: Linux (GNU sed)

      To replace every instance of the word "Hello" with the word "World" in a file using GNU sed, use the following command:

      .. code-block:: shell

         sed -i -e 's/README/index/g' index.rst

    .. group-tab:: Windows (Powershell)

      Windows has no ``sed`` equivalent natively installed. Instead, in order to replace every instance of the word "Hello" with the word "World" in a file,
      use the following command in Powershell:

      .. code-block:: shell

         (Get-Content index.rst) -replace 'Hello', 'World' | Out-File -encoding ASCII index.rst


Renders as:

.. tabs::

   .. group-tab:: MacOS (BSD sed)

      To replace every instance of the word "Hello" with the word "World" in a file using BSD sed, use the following syntax:

      .. code-block:: shell

         sed -i '' 's/Hello/World/g' index.rst

   .. group-tab:: Linux (GNU sed)

      To replace every instance of the word "Hello" with the word "World" in a file using GNU sed, use the following syntax:

      .. code-block:: shell

         sed -i -e 's/README/index/g' index.rst

   .. group-tab:: Windows (Powershell)

      Windows has no ``sed`` equivalent natively installed. Instead, in order to replace every instance of the word "Hello" with the word "World" in a file,
      use the following command in Powershell:

      .. code-block:: shell

         (Get-Content index.rst) -replace 'Hello', 'World' | Out-File -encoding ASCII index.rst
