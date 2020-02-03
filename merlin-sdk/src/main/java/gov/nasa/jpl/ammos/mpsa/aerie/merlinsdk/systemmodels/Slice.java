package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

public interface Slice {
    //for now, this is the only method we need to make
    //system models work in this configuration
    public Instant time();

    public void setTime(Instant time);

    public void printSlice();

    public Slice cloneSlice();
}
