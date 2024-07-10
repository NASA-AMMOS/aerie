package gov.nasa.jpl.aerie.command_model.activities;

import gov.nasa.jpl.aerie.command_model.Mission;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;

// This is a bit of a strange class...
// It's an activity, but it only makes sense to be called from the Sequencing engine.
// It doesn't really make sense for the user to place it directly.
// In fact, we had to define a fake RunnableValueMapper that just returns a dummy value to even get this to work.
@ActivityType("Command")
public class CommandSpan {
    @Export.Parameter
    public Runnable behavior;

    // Constructor to satisfy Activity signature
    public CommandSpan() {
        this(() -> {});
    }

    // Constructor used internally:
    public CommandSpan(Runnable behavior) {
        this.behavior = behavior;
    }

    @ActivityType.EffectModel
    public void run(Mission mission) {
        behavior.run();
    }
}
