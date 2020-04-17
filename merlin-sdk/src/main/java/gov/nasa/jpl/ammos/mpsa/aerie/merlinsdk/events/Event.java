package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.events;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

enum EventType {
    SETTABLE, ACTIVITY;
}

public interface Event<T> {
    String name();
    T value();
    Instant time();
    EventType eventType();
}
