package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Windows;

import java.util.*;

public final class ActivityModel implements ActivityModelQuerier {
    private Map<String, List<Duration>> activityInstanceMap = new HashMap<>();
    private Map<String, List<String>> activityTypeMap = new HashMap<>();

    private Duration elapsedTime = Duration.ZERO;

    public ActivityModel(){};

    public ActivityModel(ActivityModel activityModel) {
        this.elapsedTime = activityModel.elapsedTime;

        for (var x : activityModel.activityInstanceMap.entrySet()){
            var otherList = x.getValue();
            var list = new ArrayList<Duration>();
            list.add(otherList.get(0));
            if (otherList.size() == 2){
                list.add(otherList.get(1));
            }
            this.activityInstanceMap.put(x.getKey(), list);
        }

        for (var x : activityModel.activityTypeMap.entrySet()) {
            List<String> idList = new ArrayList<>();
            for (var y : x.getValue()){
                idList.add(y);
            }

            this.activityTypeMap.put(x.getKey(), idList);
        }
    }

    public void step(Duration duration) {
        elapsedTime = elapsedTime.plus(duration);
    }

    public void activityStart(String activityID, SerializedActivity activityType) {
        if (activityInstanceMap.containsKey(activityID)) {
            throw new RuntimeException("Activity " + activityID + " already started!");
        }

        var list = new ArrayList<Duration>();
        list.add(this.elapsedTime);
        activityInstanceMap.put(activityID, list);

        this.activityTypeMap.computeIfAbsent(activityType.getTypeName(), x -> new ArrayList<>());
        this.activityTypeMap.get(activityType.getTypeName()).add(activityID);
    }

    public void activityEnd(String activityID) {
        if (!activityInstanceMap.containsKey(activityID)) {
            throw new RuntimeException("Activity " + activityID + " never started!");
        } else if (activityInstanceMap.get(activityID).size() != 1) {
            throw new RuntimeException("Activity " + activityID + " already ended!");
        }

        activityInstanceMap.get(activityID).add(this.elapsedTime);
    }

    @Override
    public List<String> getActivitiesOfType(final String activityType) {
        return List.copyOf(this.activityTypeMap.getOrDefault(activityType, Collections.emptyList()));
    }

    @Override
    public Window getCurrentInstanceWindow(String activityID) {
        final var times = this.activityInstanceMap.get(activityID);

        return Window.between(
            times.get(0),
            (times.size() == 1) ? this.elapsedTime : times.get(1));
    }

    @Override
    public Windows getTypeWindows(String activityType) {
        final var windows = new Windows();

        for (var activityId : this.activityTypeMap.get(activityType)) {
            windows.add(this.getCurrentInstanceWindow(activityId));
        }

        return windows;
    }
}
