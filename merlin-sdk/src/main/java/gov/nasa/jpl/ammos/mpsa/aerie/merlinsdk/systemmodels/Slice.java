package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

public interface Slice {
    //for now, this is the only method we need to make
    //system models work in this configuration
    public Time time();

    public void setTime(Time time);

    public void printSlice();

    public Slice cloneSlice();
}
