package gov.nasa.jpl.aerie.command_model.activities.commands;

import gov.nasa.jpl.aerie.command_model.Mission;
import gov.nasa.jpl.aerie.command_model.generated.ActivityActions;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

@ActivityType("CMD_WAIT")
public class CMD_WAIT implements CommandActivity {
    @Export.Parameter
    public Double seconds;

    @ActivityType.EffectModel
    public void run(Mission mission) {
        delay(Duration.roundNearest(seconds, Duration.SECONDS));
    }

    @Override
    public void call(Mission mission) {
        ActivityActions.call(mission, this);
    }
}
