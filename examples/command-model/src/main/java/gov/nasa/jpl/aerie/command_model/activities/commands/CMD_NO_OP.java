package gov.nasa.jpl.aerie.command_model.activities.commands;

import gov.nasa.jpl.aerie.command_model.Mission;
import gov.nasa.jpl.aerie.command_model.generated.ActivityActions;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

@ActivityType("CMD_NO_OP")
public class CMD_NO_OP implements CommandActivity {
    @ActivityType.EffectModel
    public void run(Mission mission) {
        delay(DEFAULT_COMMAND_DURATION);
    }

    @Override
    public void call(Mission mission) {
        ActivityActions.call(mission, this);
    }
}
