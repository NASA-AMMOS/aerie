package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.MutableResourceViews;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources;

import static gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.$enum;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.constant;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.discreteResource;

/**
 * A demonstration model for primeness-style multiplexing.
 */
public class PrimenessModel {
    public enum DeviceState { OFF, STANDBY, ON }
    public enum Side { A, B }
    public enum SideChoice { A, B, PRIME, BACKUP }

    public final MutableResource<Discrete<DeviceState>> deviceA, deviceB, devicePrime, deviceBackup;
    public final MutableResource<Discrete<Side>> primeSide;

    public PrimenessModel(Registrar registrar) {
        deviceA = discreteResource(DeviceState.OFF);
        deviceB = discreteResource(DeviceState.OFF);
        primeSide = discreteResource(Side.A);

        var muxResult = MutableResourceViews.multiplex(
                DiscreteResources.equals(primeSide, constant(Side.A)),
                deviceA,
                deviceB);
        devicePrime = muxResult.get(true);
        deviceBackup = muxResult.get(false);

        registrar.discrete("primeness/deviceA", deviceA, $enum(DeviceState.class));
        registrar.discrete("primeness/deviceB", deviceB, $enum(DeviceState.class));
        registrar.discrete("primeness/devicePrime", devicePrime, $enum(DeviceState.class));
        registrar.discrete("primeness/deviceBackup", deviceBackup, $enum(DeviceState.class));
        registrar.discrete("primeness/primeSide", primeSide, $enum(Side.class));
    }
}
