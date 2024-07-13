package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;
import gov.nasa.jpl.aerie.streamline_demo.PrimenessModel.DeviceState;

import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.set;

@ActivityType("ChangePrimenessDeviceState")
public class ChangePrimenessDeviceState {
    @Export.Parameter
    public PrimenessModel.SideChoice side;

    @Export.Parameter
    public DeviceState state;

    @ActivityType.EffectModel
    public void run(Mission mission) {
        var resource = switch (side) {
            case A -> mission.primenessModel.deviceA;
            case B -> mission.primenessModel.deviceB;
            case PRIME -> mission.primenessModel.devicePrime;
            case BACKUP -> mission.primenessModel.deviceBackup;
        };
        set(resource, state);
    }
}
