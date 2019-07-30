package gov.nasa.jpl.europa.clipper.merlin.gnc.utilities;

import gov.nasa.jpl.ammos.mpsa.merlin.multimissionmodels.gnc.classes.Attitude;

public static class PointingUtils {

    public static Attitude getEarthPointAttitude() {
        return new Attitude(
            "EARTH", "ECLIP2000_POSZ", "NEGY", "POSX",
            new Double[] {0.0, 0.0}, new Double[] {0.0, 0.0});
    }

    public static Attitude getSunPointAttitude() {
        return new Attitude(
            "SUN", "EARTH_CROSS_SUN", "NEGY", "POSX",
            new Double[] {0.0, 0.0}, new Double[] {0.0, 0.0});
    }
    
}