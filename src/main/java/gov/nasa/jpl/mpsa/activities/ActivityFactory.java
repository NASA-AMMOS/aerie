package gov.nasa.jpl.mpsa.activities;

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
    private Activity parent;
    private List<Activity> children;
    private Condition condition;

    // these are only used for printing out to reports or displays and should never be used for any modeling
    // to use other args besides time for modeling, set them in the subclass Activity
    private Object[] parameterObjects;

    public static Time now;

    @Override
    public Activity createActivity(Time t, Object... varargs) {

        ActivityInstanceList.getActivityList().add(this);
        start = t;
        duration = new Duration(1);
        parent = null;
        children = new ArrayList<>();

        // because condition is attached to the activity type, not instance, we can build it here
        condition = setCondition();

        // the default is that an Activity's name gets set to its type, but this can be overridden in subclass constructor
        name = getType();

        // we store this so activities can be edited later then work when those edits are undone
        storeParameterValues(varargs);

        // if we're reading in information from a file, we get both our IDs and children from the file. otherwise we generate them here
        if (!ModelingEngine.getEngine().isCurrentlyReadingInFile()) {
            assignID();
            // if we're a scheduler, we should automatically set up to be scheduled to eliminate SCHEDULE call
            schedule();
        }
    }
}
