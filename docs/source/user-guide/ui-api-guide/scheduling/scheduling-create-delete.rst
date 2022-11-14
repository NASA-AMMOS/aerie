==================================================
Creating, modifying, and deleting scheduling goals
==================================================


Creating a Scheduling Goal
--------------------------

For information on authoring scheduling goals, see :doc:`here <scheduling-goals>`.

.. tabs::

  .. group-tab:: User Interface

    This is how to create a scheduling goal via the UI.
    In the Aerie UI, open the scheduling pane by clicking on the top-right bar.

    .. image:: img/open-scheduling-pane.png
      :width: 400

    The scheduling pane opens in a new tab.

    .. image:: img/scheduling-panel.png
      :width: 400

    Click ``New``. This will open a text editor

    .. image:: img/goal-editor-new.png
      :width: 800

    with the following default text:

    .. code-block:: typescript

      export default (): Goal => {
        // Your code here
      }

    This is a TypeScript function that takes no arguments and returns a Goal.

    To unpack all of the parts:

    - ``export default`` signals to Aerie that this is the function that defines the Goal.
    - ``() => {}`` in TypeScript is called an `arrow function <https://www.tutorialsteacher.com/typescript/arrow-function>`_.
    - The parenthesis ``()`` represent the parameters that the function takes. Scheduling goals cannot take any parameters, so these parenthesis must be empty.
    - The curly braces ``{}`` represent the definition of the goal. The return statement for the function must go inside the braces.
    - The ``: Goal`` part signifies that this function returns a Goal. TypeScript will check that the function does indeed return a Goal - if it does not, it will underline your code in red.

    The code provided when you click ``New Goal`` is incomplete - the function does not yet return a Goal, so you should
    see the word ``Goal`` underlined in red:

    .. image:: https://user-images.githubusercontent.com/1189602/161592529-5abff638-bec7-4c19-a0c0-639e1bf35d98.png

    Mousing over the word Goal, you should see something akin to the following message:

    ``A function whose declared type is neither 'void' nor 'any' must return a value.``

    This message means that the function has promised to return a value, but it currently lacks a return statement.
    Between the curly braces, add the following code: ``return Goal.ActivityRecurrenceGoal()``:

    .. code-block:: typescript

      export default (): Goal => {
        return Goal.ActivityRecurrenceGoal() // <---- this is the new code
      }

    Now, the editor should tell you that ``ActivityRecurrenceGoal()`` takes one argument.

    .. image:: https://user-images.githubusercontent.com/1189602/161593612-b23560ea-c3b9-44c8-bac4-620e98d76356.png

    The argument that we're missing is the "options" object. Objects in typescript are defined using curly braces ``{}``
    with key-value pairs, like so:

    .. code-block:: typescript

      {
        key: value
      }

    If we pass an empty object ``{}`` to ``ActivityRecurrenceGoal``, we will get a new error message that will tell us what
    keys our object will need:

    .. code-block:: typescript

      export default (): Goal => {
        return Goal.ActivityRecurrenceGoal({}) // <---- the empty object is written as {}
      }

    ::

      Argument of type '{}' is not assignable to parameter of type '{ activityTemplate: ActivityTemplate;
      interval: number; }'.
      Type '{}' is missing the following properties from type '{ activityTemplate: ActivityTemplate; interval: number;
      }': activityTemplate, interval


    This error message tells us that our object is missing two keys: ``activityTemplate``, and ``interval``. If we look up
    the definition of ``ActivityRecurrenceGoal`` in the scheduling documentation, we will see that it does indeed need an
    activity template and an interval. Let's add those:

    .. code-block:: typescript

      export default (): Goal => {
          return Goal.ActivityRecurrenceGoal({
              activityTemplate: null,
              interval: Temporal.Duration.from({ hours: 24 })
          })
      }

    Now, we just need to finish specifying the **activityTemplate**. Start by typing ``ActivityTemplates.`` (note the period)
    , and select an activity type. Provide your activity an object with the arguments that that activity takes. Once
    the editor is no longer underlining your code, save your goal by clicking on ``Save``. Once your goal is saved, you can close the tab or leave it open.


  .. group-tab:: API

    This is how to insert a scheduling goal via the API.

Managing Scheduling Goals
-------------------------

.. tabs::

  .. group-tab:: User Interface

    This is how to modify/delete a scheduling goal via the UI.
    Open the scheduling pane by clicking in the top-right menu bar.

    .. image:: img/open-scheduling-pane.png
      :width: 400

    Right-click on the goal you want to modify

    .. image:: img/right-click-goal.png
      :width: 400

    Click on ``Delete Goal`` to delete the goal.
    If you click on ``Edit Goal``, it will open a new tab with a editor.

    .. image:: img/goal-editor.png
      :width: 800

  When you are done with editing the goal, click on ``Save``. Your goal is saved, you can close the tab.

  .. group-tab:: API

    This is how to modify/delete a scheduling goal via the API.

