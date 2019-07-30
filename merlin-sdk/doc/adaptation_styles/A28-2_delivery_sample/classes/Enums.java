package gov.nasa.jpl.europa.clipper.merlin.gnc.classes;

public class Enums {

    @ParameterType
    public enum GNCControlMode {
        RWA, RCS
    }

    @ParameterType
    public enum AttitudeMode {
        INERTIAL, INERTIAL_SUN, INERTIAL_EARTH, SUN, SUN_ROLL, ANTI_SUN, EARTH1, NADIR, NADIRTRUE, NADIRSUN, NADIRPIMS,
        SURFACE_POINT, SUNOCC, KEPLERRAM, RAMSUN, RAM2JUPITER, EARTH_FLYBY, LANDER_RELAY_COMM, DETUMBLE, SEPARATION
    }

    @ParameterType
    public enum SolarArrayMode {
        SUN, FIXED, HOLD
    }
}