package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.builders.AdaptationBuilder;

// TODO: Load Spice
// TODO: Collect constraints
public class MerlinSDK {
    public static AdaptationBuilder createAdaptation() {
        return new AdaptationBuilder();
    }
}
