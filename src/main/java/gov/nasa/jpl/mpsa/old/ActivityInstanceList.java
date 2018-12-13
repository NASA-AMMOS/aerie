package gov.nasa.jpl.mpsa.old;


import gov.nasa.jpl.mpsa.time.Time;

import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;

public class ActivityInstanceList {
    private List<ActivityType> allActivities;

    // we keep a main activity instance list with all activities in it in this class
    // however we do make other instances of this class for filtering activities for writing outfiles
    private static ActivityInstanceList instance = null;

    public ActivityInstanceList() {
        allActivities = new ArrayList<ActivityType>();
    }

    public static ActivityInstanceList getActivityList() {
        if (instance == null) {
            instance = new ActivityInstanceList();
        }
        return instance;
    }

    public void prepareActivitiesForModeling() {
        Collections.sort(allActivities);
        for (ActivityType act : allActivities) {
            // we want each condition to evaluate by default to its profile
            act.setInitialCondition();
        }
    }

    /**
     * For now we only need to sort the activities before sequencing.
     */
    public void prepareActivitiesForSequencing() {
        Collections.sort(allActivities);
    }

    public void stopAllSchedulers() {
        for (ActivityType act : allActivities) {
            act.stopScheduling();
        }
    }

    public void add(ActivityType act) {
        allActivities.add(act);
    }

    /**
     * Takes in an activity instance and removes it from the ActivityInstanceList.
     *
     * @param act
     */
    public void remove(ActivityType act) {
        allActivities.remove(act);
    }

    /**
     * Takes in an activity id and removes it from the ActivityInstanceList.
     *
     * @param id
     */
    public void remove(UUID id) {
        for (int i = 0; i < allActivities.size(); i++) {
            // if the input id matches the id of the activity then remove the activity and exit the loop
            if (allActivities.get(i).getIDString().equals(id.toString())) {
                allActivities.remove(i);
                return;
            }
        }
        // throw exception if activity id was not found
        throw new RuntimeException("Could not find activity instance with id " + id + ".");
    }

    public int length() {
        return allActivities.size();
    }

    public void clear() {
        allActivities = new ArrayList<>();
    }

    public ActivityType get(int i) {
        return allActivities.get(i);
    }

    /**
     * Returns an activity instance from the ActivityInstanceList given an input activity id.
     *
     * @param activityID
     * @return
     */
    public ActivityType findActivityByID(UUID activityID) {
        for (int i = 0; i < allActivities.size(); i++) {
            // if the input id matches the id of the activity then return the activity
            if (allActivities.get(i).getIDString().equals(activityID.toString())) {
                return allActivities.get(i);
            }
        }
        // throw exception if activity id was not found
        throw new RuntimeException("Could not find activity instance with id " + activityID + ".");
    }

    /**
     * Returns true if the ID can be found in the activity instance list, false if not.
     *
     * @param activityID
     * @return
     */
    public boolean containsID(UUID activityID) {
        for (int i = 0; i < allActivities.size(); i++) {
            // if the input id matches the id of the activity then return the activity
            if (allActivities.get(i).getIDString().equals(activityID.toString())) {
                return true;
            }
        }
        return false;
    }

    public Time getFirstActivityStartTime() {
        // list should be sorted before we call this, so we can just take the first element
        if (allActivities.isEmpty()) {
            return null;
        }
        else {
            return allActivities.get(0).getStart();
        }
    }

    public Time getLastActivityEndTime() {
        // list should be sorted before we call this, so we can just take the last element
        if (allActivities.isEmpty()) {
            return null;
        }
        else {
            return allActivities.get(allActivities.size() - 1).getEnd();
        }
    }

    /*
     * The time is the time that the activity either begins or ends, the boolean
     * is true if the activity is a start and false if it is an end, and the activity
     * is the one that either starts or ends at that time
     */
    public List<Map.Entry<Time, Map.Entry<Boolean, ActivityType>>> createListOfActivityBeginAndEndTimes() {
        ArrayList<Map.Entry<Time, Map.Entry<Boolean, ActivityType>>> listOfAllBeginAndEndTimes = new ArrayList();
        for (int i = 0; i < allActivities.size(); i++) {
            ActivityType ofInterest = allActivities.get(i);
            listOfAllBeginAndEndTimes.add(new SimpleImmutableEntry<Time, Map.Entry<Boolean, ActivityType>>(ofInterest.getStart(), new SimpleImmutableEntry<Boolean, ActivityType>(true, ofInterest)));
            listOfAllBeginAndEndTimes.add(new SimpleImmutableEntry<Time, Map.Entry<Boolean, ActivityType>>(ofInterest.getEnd(), new SimpleImmutableEntry<Boolean, ActivityType>(false, ofInterest)));
        }
        // now we have to sort this list since we have no idea when end times are
        Collections.sort(listOfAllBeginAndEndTimes, Map.Entry.comparingByKey());
        return listOfAllBeginAndEndTimes;
    }

    private boolean isSorted() {
        for (int i = 1; i < allActivities.size(); i++) {
            if (allActivities.get(i - 1).compareTo(allActivities.get(i)) > 0) {
                return false;
            }
        }

        return true;
    }
}
