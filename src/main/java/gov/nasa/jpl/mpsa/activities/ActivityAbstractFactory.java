package gov.nasa.jpl.mpsa.activities;

import gov.nasa.jpl.mpsa.time.Time;

import java.util.UUID;

public abstract class ActivityAbstractFactory {

    abstract Activity createActivity(Time t, Object... varargs);

}
