==========================
Building Aerie for Windows
==========================

.. warning::
   Aerie does not currently officially support Windows. As such, the below guide is not guaranteed to currently work or to continue to work.

-------------
Prerequisites
-------------

^^^^^^^^^^^^^^^^^^^^^^^
Hardware Virtualization
^^^^^^^^^^^^^^^^^^^^^^^

Enable Hardware Virtualization in your BIOS.

If you aren't sure if Hardware Virtualization is enabled, there are three places you can check before going into the BIOS menu. If _any_ of these places indicate that Virtualization is enabled, then you can proceed to the next step without modifying your BIOS.

1. In ``System Information``, go to the bottom of the page ``System Summary``. Either of the below options indicate that Virtualization is enabled:

   1. The entry ``Hyper-V - Virtualization Enabled in Firmware`` has ``Yes`` as a value.

      .. image:: ../images/deployment-windows/hyper-v-off.png
         :align: center
         :height: 100

   2. The entry ``A hypervisor has been detected. Features required for Hyper-V will not be displayed`` exists.

      .. image:: ../images/deployment-windows/hyper-v-on.png
         :align: center
         :height: 300

2. In ``Task Manager``, go to ``Performance>CPU``. On the right column underneath the graph you will see a line with the text ``Virtualization: Enabled``.

   .. image:: ../images/deployment-windows/task-manager.png
         :align: center
         :height: 300

3. In ``Turn Windows features on or off``, the ``Hyper-V`` entry *and all of its subfolders* are checkmarked.

   .. image:: ../images/deployment-windows/windows-features.png
         :align: center
         :height: 300

^^^^
WSL2
^^^^

Install the WSL2 (Windows Subsystem for Linux) driver from `here <https://learn.microsoft.com/en-us/windows/wsl/install>`_.

Ensure that WSL2 is the default version of WSL by entering into the Command Line ``wsl --set-default-version 2``.

^^^^
Unix
^^^^

Install and set up a Unix distro, for example, `Ubuntu from the Microsoft Store <https://www.microsoft.com/store/productId/9PDXGNCFSCZV>`_. If you already have one, you do not need to install a new one.

Verify that the distro is using WSL2 with the command `wsl -l -v` from the Windows Command Line. If its version isn't 2, change it using ``wsl --set-version 2``.

------
Docker
------

Install `Docker Desktop <https://docs.docker.com/desktop/install/windows-install/>`_. When asked to choose between the Hyper-V and WSL2 backends, choose WSL2. If Docker Desktop was installed prior to installing WSL2, go into `Settings>General` and ensure that `Use the WSL 2 Based Engine` is selected.

.. image:: ../images/deployment-windows/docker-wsl2.png
   :align: center
   :height: 300

In Docker Desktop, go to ``Settings>Resources>WSL Integration``. Enable integration with at least your distro of choice. Press Apply and Restart.

.. image:: ../images/deployment-windows/docker-distro.png
   :align: center
   :height: 300

-----
Aerie
-----

.. note::
   From here on out, all terminal commands are implied to be happening from within the distro's terminal unless otherwise specified.

   If you are instead trying to deploy Aerie, see `the deployment directory <https://github.com/NASA-AMMOS/aerie/tree/develop/deployment>`__.
   Ensure that you use the Unix terminal to unzip Deployment.zip and that the location it is unzipped to is in the distro's filesystem.
   If you recieve a permissions error while attempting to deploy the unzipped code, see the important note below.

Pull `Aerie <https://github.com/NASA-AMMOS/aerie/>`_ to the Unix distro using ``git clone`` or any preferred alternative. If you accidentally download Aerie to the Windows side, move the project files to your Unix distro. (Tip: if you want to see your distro's filesystem using Windows Explorer, enter `explorer.exe .`.)

.. note::
   Set the owner and group of the ``aerie`` directory and all files within it to your Unix account name using ``chown -R <name>:<group> aerie`` from within the parent directory.
   If you get a permissions error, instead run ``sudo chown -R <name>:<group> aerie``. Verify this change was successful with ``ls -l``.
   The third and fourth entries for ``aerie`` should be set to your Unix name and group.

Install the recommended JDK in your Unix distro. The recommended JDK for Aerie can be found in :doc:`Building Aerie <building>`.

Run ``./gradlew build`` to generate the build files.

Run ``docker compose up --detach`` to start the project.

Run ``docker compose down`` when finished.

For further information on building Aerie, see :doc:`Building Aerie <building>`.

