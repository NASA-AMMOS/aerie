package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

public interface SystemModel {

    public void step(Slice slice, Duration dt);

    public void registerSelf();

    public void applySlice(Slice slice);

    public Slice saveToSlice();

}
