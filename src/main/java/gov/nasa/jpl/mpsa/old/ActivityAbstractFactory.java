package gov.nasa.jpl.mpsa.old;

import gov.nasa.jpl.mpsa.time.Time;

public abstract class ActivityAbstractFactory {

    abstract ActivityType createActivity(Time t, Object... varargs);

}
