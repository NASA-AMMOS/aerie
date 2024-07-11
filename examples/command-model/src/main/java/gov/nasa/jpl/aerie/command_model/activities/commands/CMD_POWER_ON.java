package gov.nasa.jpl.aerie.command_model.activities.commands;

import gov.nasa.jpl.aerie.command_model.Mission;
import gov.nasa.jpl.aerie.command_model.generated.ActivityActions;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

@ActivityType("CMD_POWER_ON")
public class CMD_POWER_ON implements CommandActivity {
    @Export.Parameter
    public String device;

    @ActivityType.EffectModel
    public void run(Mission mission) {
        // Effects handled by hooks instead of direct implementation
        delay(DEFAULT_COMMAND_DURATION);
    }

    @Override
    public void call(Mission mission) {
        ActivityActions.call(mission, this);
    }
}
