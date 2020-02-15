package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

public interface SystemModel {
    SystemModel duplicate();

    default void step(final Duration dt) {}
    default void react(final String key, final Object stimulus) {}
}
