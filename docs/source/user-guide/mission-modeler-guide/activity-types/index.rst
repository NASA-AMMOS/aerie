==============
Activity Types
==============

An **activity type** defines a simulated behavior that may be invoked by
a planner, separate from the autonomous behavior of the mission model
itself. Activity types may define **parameters**, which are filled with
**arguments** by a planner and provided to the activity upon execution.
Activity types may also define **validations** for the purpose of
informing a planner when the parameters they have provided may be
problematic.

.. code:: java

   // examples/banananation/activities/PeelBananaActivity.java
   @ActivityType("PeelBanana")
   public final class PeelBananaActivity {
     private static final double MASHED_BANANA_AMOUNT = 1.0;

     @Parameter
     public String peelDirection = "fromStem";

     @Validation("peel direction must be fromStem or fromTip")
     @Validation.Subject("peelDirection")
     public boolean validatePeelDirection() {
       return List.of("fromStem", "fromTip").contains(this.peelDirection);
     }

     @EffectModel
     public void run(final Mission mission) {
       if (peelDirection.equals("fromStem")) {
         mission.fruit.subtract(MASHED_BANANA_AMOUNT);
       }
       mission.peel.subtract(1.0);
     }
   }

Merlin automatically generates parameter serialization boilerplate for
every activity type defined in the mission model’s
``package-info.java``. Moreover, the generated ``Model`` base class
provides helper methods for spawning each type of activity as children
from other activities.


Activity Type Annotations
-------------------------

In order for Merlin to detect an activity type, its class must be
annotated with the ``@ActivityType`` tag. An activity type is declared
with its name using the following annotation:

.. code:: java

   @ActivityType("TurnInstrumentOff")

By doing so, the Merlin annotation processor can discover all activity
types declared in the mission model, and validate that activity type
names are unique.


Activity Type Metadata
----------------------

Metadata of activities are structured such that the Merlin annotation
processor can extract this metadata given particular keywords.
Currently, the Merlin annotation processor recognizes the following
tags:
``contact, subsystem, brief_description,`` and ``verbose_description.``

These metadata tags are placed in a JavaDocs style comment block above
the Activity Type to which they refer. For example:

.. code:: java

   /**
    * @subsystem Data
    * @contact ccamargo
    * @brief_description A data management activity that deletes old files
    */


.. toctree::
  :includehidden:
  :maxdepth: 2

  activity-parameters
  effect-model
