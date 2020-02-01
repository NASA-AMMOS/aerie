package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

public interface Slice {
    void step(Duration dt);
    Slice duplicate();
}
