package gov.nasa.jpl.aerie.command_model.activities.commands;

import gov.nasa.jpl.aerie.command_model.Mission;
import gov.nasa.jpl.aerie.command_model.generated.ActivityActions;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;

import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Logging.LOGGER;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

@ActivityType("CMD_ECHO")
public class CMD_ECHO implements CommandActivity {
    @Export.Parameter
    public String message;

    @ActivityType.EffectModel
    public void run(Mission mission) {
        LOGGER.info("CMD_ECHO: %s", message);
        delay(DEFAULT_COMMAND_DURATION);
    }

    @Override
    public void call(Mission mission) {
        ActivityActions.call(mission, this);
    }
}
