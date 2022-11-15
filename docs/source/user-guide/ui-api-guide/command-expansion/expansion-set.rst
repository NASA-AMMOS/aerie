==============
Expansion Sets
==============


Create an Expansion Set
-----------------------

.. tabs::

  .. group-tab:: User Interface

    An expansion set is merely a collection of expansion logic with the restriction of oe logic per activity type. Aerie requires an expansion set to expand simulated activities into time ordered commands. All simulated instances of activity types that has an expansion logic in the set will be expanded. It is possible to create different expansion sets for different groups of activity types, such that they can be expended independently. 

    To view existing expansion sets or to create a new one, click on the **Sets** tab on the upper right corner of the Expansion page. This view lists all existing expansion sets along with the mission model and command dictionary id that they are created with. This means that all the expansion logic in the set are valid against the given mission model and command dictionaries. Clicking on the expansion lists all of its contents in a read only mode. To create a new expansion set click the **New** button on the upper right corner of the left pane with the table. 

    .. image:: ../images/cmdexp_set.png
        :alt: expansion set 

    Limitation: Aerie plans to provide more metadata like name or user defined version number to differentiate mission models and command dictionaries more efficiently. 

    In the expansion set creation view, users must select a command dictionary and mission model version. This action filters all expansion logic valid for the two inputs. In the image below users can select as many activity types, but must select a single logic per type. 

    .. image:: ../images/cmdexp_author_set.png
        :alt: create expansion set 

    Limitation: Aerie plans to provide more metadata like name or user defined version number to differentiate expansion sets more efficiently. 

    Limitation: Expansion sets are immutable objects such that their contents can't be altered for the purposes of keeping a record for what produced the expansion outputs. Currently users need to create the expansion set from scratch if they need to modify it. Options for duplicating an existing set in editable mode or versioning expansion sets are under consideration.  

  .. group-tab:: API

    This is how to create an expansion set via the API
