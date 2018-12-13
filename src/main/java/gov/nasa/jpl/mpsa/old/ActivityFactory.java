package gov.nasa.jpl.mpsa.old;

import gov.nasa.jpl.mpsa.conditions.Condition;
import gov.nasa.jpl.mpsa.time.Duration;
import gov.nasa.jpl.mpsa.time.Time;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ActivityFactory extends ActivityAbstractFactory{

    private UUID id;
    protected Time start;
    protected Duration duration;
    protected String name;
    private ActivityType parent;
    private List<ActivityType> children;
    private Condition condition;

    // these are only used for printing out to reports or displays and should never be used for any modeling
    // to use other args besides time for modeling, set them in the subclass Activity
    private Object[] parameterObjects;

    public static Time now;

    @Override
    public ActivityType createActivity(Time t, Object... varargs) {

        ActivityInstanceList.getActivityList().add(this);
        start = t;
        duration = new Duration(1);
        parent = null;
        children = new ArrayList<>();

    }
}
