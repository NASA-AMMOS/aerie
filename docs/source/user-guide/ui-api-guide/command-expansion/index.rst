==========================
Activity Command Expansion
==========================

The Command Dictionary
----------------------

Uploading a Command Dictionary
==============================

.. tabs::

  .. group-tab:: User Interface

    Aerie provides command expansion capability that can translate a simulated activity into time ordered commands using the logic provided by the users. Expansion logic is provided for an activity type, but applied to all simulated instances of the activity during the expansion. 

    Prerequisites: Aerie installation must have a mission model and command dictionary uploaded. To upload a mission model see instructions here. To upload a command dictionary, open the top level Aerie menu at the upper left corner as shown in the first figure, and navigate to the "Command Expansions" page. All command dictionaries available at the menu will be listed here. If the you would like to use a dictionary version that is available to you and not available at the venue, you can click "Choose File" to upload a command dictionary available locally on your machine. 
    Restrictions: Aerie only supports AMPCS command dictionary schema standard. This schema provides a mission name and version in the command dictionary file. Aerie won't allow uploading the command dictionary files that has the same mission name and version.  

    .. figure:: ../images/home_topmenu.png
      :alt: aerie top menu


      Figure 1: Aerie top level navigation menu. 

    .. figure:: ../images/cmdexp_uploadCMD.png
      :alt: aerie top menu


      Figure 2: Command dictionaries page accessible from the top menu allows to view, delete and upload command dictionaries that comply with AMPCS standard.
    



    




  .. group-tab:: API

    How to insert/delete a command dictionary via the API


.. toctree::
  :includehidden:

  expansion-logic
  expansion-set
  sequences
  trigger-command-expansion
