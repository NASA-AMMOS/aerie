package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.streamline_demo.PrimenessModel.Side;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.set;

@ActivityType("SwapPrimeness")
public class SwapPrimeness {
    @ActivityType.EffectModel
    public void run(Mission mission) {
        var primeSide = currentValue(mission.primenessModel.primeSide);
        var backupSide = switch (primeSide) {
            case A -> Side.B;
            case B -> Side.A;
        };
        set(mission.primenessModel.primeSide, backupSide);
    }
}
