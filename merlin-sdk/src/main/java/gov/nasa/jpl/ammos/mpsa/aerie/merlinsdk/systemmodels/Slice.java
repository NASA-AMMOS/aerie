package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

public interface Slice {
    Slice duplicate();

    default void step(final Duration dt) {}
    default void react(final String resourceName, final Stimulus stimulus) {}
}
