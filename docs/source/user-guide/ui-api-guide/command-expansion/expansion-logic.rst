=======================
Command Expansion Logic
=======================


Managing Expansion Logic
-------------------------


.. tabs::
  .. group-tab:: User Interface

   Navigate to the "Expansion" page from the Aerie top level menu, to view, edit or delete existing expansion logic, or to create a new one. The Expasions page shown in Figure 1 below lists all expansion logic authored by any user who has access to the Aerie venue. Click on New above the table to the right, to create a new expansion logic. 


   .. figure:: ../images/cmdexp_manage_logic.png
      :alt: expansion logic


      Figure 1: Navigate to the Expansion page from Aerie top level menu to manage expansion logic. 

  The tabular view allows filtering expansion logic by activity type when the filter icon is clicked to the right of the activity type column. 
    .. image:: ../images/cmdexp_logic_filter.png
      :alt: filter expansion logic
  
  Clicking on the expansion logic reveals its contents in a read only mode in the editor on the right. To edit or delete the expansion logic click icons on the right handside of the row as shown in the image below. 

    .. image:: ../images/cmdexp_logic_edit_delete.png
      :alt: edit delete expansion logic


  .. group-tab:: API

    This is how to query/mutate/delete expansion logic via the API
   

Authoring Expansion Logic
-------------------------

.. tabs::
  .. group-tab:: User Interface

    Navigate to the expansion logic editor editor by clicking New on the Expansion main page, or by clicking the edit icon for any existing expasion logic listed there. The editor page allows selecting a command dictionary version and a mission model version. Once the mission model version is selected, the Activity Type drop down menu lists all the activity types defined in the selected mission model. Once the activity type is selected, its parameters can be accessed within the logic. 

    .. figure:: ../images/cmdexp_author.png
      :alt: expansion authoring


      Figure 1: Command expansion editor view. 

    Note that the expansion logic starts with a boiler plate code, and users are expected to add their commands as a comma seperated objects in ``return []``  structured as the following: ``Timing.CommandStem(argument_1, argument_2,..),`` 

    Each command stem must be preceded by a time type including one of the following: 
    | ``C`` command complete,
    | ``A(YYYY-DDDThh:mm:ss)`` absolute time,
    | ``E(durationOffset)`` epoch relative, 
    | ``R(durationOffset)`` command relative. 
    | All of these time types are supported in the AMPCS command dictionary. durationn offset for epoch and command relative time types should be defined as a ``Temporal.Duration`` object. Details of how to utilize this object can be found `here <https://tc39.es/proposal-temporal/docs/duration.html>`_  To learn about the behavior of these time tags, refer to the flight software sequence behavior functional documentation of your mission. 
    
    Once a time type is selected and ended with a ``.``, the editor provides a drop down list of all commands available in the selected dictionary. The list of commands will be filtered as the user starts typing the command stem. Once a command stem is selected, the editor will list all the arguments and expected types for those arguments. Values for ENUMs will be listed as strings, and must be input as strings. See the below images for details. 

      .. image:: ../images/cmdexp_commands_list.png
        :alt: Editor lists all commands in the selected command dictionary. 


      .. image:: ../images/cmdexp_args_list.png
        :alt: Editor lists all arguments and their expected types once a command stem is typed. 

    The ultimate power of command expansion is to be able to map activity parameter values to command arguments. Note that the beginning of the expansion code block provides a reference to an activity instance ``const { activityInstance } = props``. This reference allows accessing various attributes of instances of the activity type selected.  Typing ``props.activityInstance.`` lists properties like start time, end time, duration and type, which can be used to inform logic statements or timing of commands.  ``props.activityInstance.attributes.arguments.`` lists all parameter names of the type. Note that expansion request will replace these with actual values from as simulated activity instances. 

      .. image:: ../images/cmdexp_props.png
        :alt: Editor lists all commands in the selected command dictionary. 


      .. image:: ../images/cmdexp_props_args.png
        :alt: Editor lists all arguments and their expected types once a command stem is typed. 

    Expansion logic call also access computed attributes of an activity. These are values computed during the activity simulation and returned. The mechanism can be used to report value of a specific resource at the time of activity simulation, or an internal parameter derived from other parameters along with other inputs, such as a static lookup table. 

    Warning: Aerie expansion API exposes duration, computed attributes and similar data that is available only post simulation. Hence input to expansion is a simulation data set. Any change to activity plan that invalidates simulation also invalidates command expansion outputs. 
    
    

  .. group-tab:: API
    This is how to insert expansion logic via the API
    

