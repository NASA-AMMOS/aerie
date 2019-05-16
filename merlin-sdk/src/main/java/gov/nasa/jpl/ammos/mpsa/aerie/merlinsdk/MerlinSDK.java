package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.builders.AdaptationBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.spice.SpiceLoader;


// TODO: Collect constraints
public class MerlinSDK {
    
    static {
        SpiceLoader.loadSpice();
    }

    public static AdaptationBuilder createAdaptation() {
        return new AdaptationBuilder();
    }
}
