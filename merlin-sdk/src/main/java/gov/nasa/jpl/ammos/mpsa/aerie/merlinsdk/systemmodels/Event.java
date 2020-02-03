package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

public interface Event<T> {
    //placeholder for now

    public String name();
    public T value();
    public Instant time();


}
