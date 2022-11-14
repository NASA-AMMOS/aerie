======================
Create a Mission Model
======================

A template Mission Model Repository is available `here <https://github.com/NASA-AMMOS/aerie-mission-model-template>`_. Choose "Use this template" -> "Create a new repository". Once your new repository has been created, clone it to your computer using `git clone <https://docs.github.com/en/repositories/creating-and-managing-repositories/cloning-a-repository>`_.

.. note::
   These instructions are written with MacOS in mind. They should work on Linux as well. Windows instructions will be added in the future.

On your computer, you'll need the following software installed:

- `Java JDK 19 <https://jdk.java.net/19/>`_
- A code editor for java code (we recommend `IntelliJ <https://www.jetbrains.com/idea/download/>`_)

Navigate inside the cloned repository in your terminal. There should
be a `gradlew` executable at the top level. This is the executable
that you will use to build and test your mission model.

To run the tests that come with the basic mission model, run:

.. code:: shell

   ./gradlew test

This will start by downloading the gradle client, and then it should
compile your mission model and run the tests. If this step succeeds,
you're ready to start modeling!

The file's you'll want to edit first will be in the
``src/main/java/firesat`` directory. There, you'll find the
`package-info.java
<https://github.com/NASA-AMMOS/aerie-mission-model-template/blob/main/src/main/java/firesat/package-info.java>`_
class, which contains a reference to the mission model class, and
listing of the activity types defined by this mission model.


Uploading a Mission Model
-------------------------

In order to use a mission model to simulate a plan on the Aerie
platform, it must be packaged as a JAR file with all of its non-Merlin
dependencies bundled in. The built mission model JAR can be uploaded to
Aerie through the Aerie web UI.

To assemble a mission model jar, run:

.. code:: shell

   ./gradlew assemble

If this succeeds, it will place the jar in the ``build/libs``
directory. When uploading a mission model through the Aerie UI,
navigate to this directory in the file picker and select your mission
model jar.
