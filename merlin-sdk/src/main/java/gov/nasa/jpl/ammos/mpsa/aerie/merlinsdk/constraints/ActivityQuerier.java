package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels.ActivityEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;

import java.util.*;

public class ActivityQuerier {

    //todo: remove this as part of a refactor PR to the previous constraint work
    private List<ActivityEvent> eventLog;

    //todo: remove this as part of a refactor PR to the previous constraint work
    public void provideEvents(List<ActivityEvent> events){
        eventLog = events;
    }

    private Map<String, List<ActivityEvent>> completeActivityEventMap;

    public void provideActivityEventMap(Map<String, List<ActivityEvent>> completeActivityEventMap){
        this.completeActivityEventMap = completeActivityEventMap;
    }

    //todo: remove this and make it work with the map
    public List<Window> whenActivityExists(){
        final var windows = new ArrayList<Window>();

        for (ActivityEvent<?> event : eventLog){
            windows.add(Window.between(event.startTime(), event.endTime()));
        }
        return windows;
    }

    public List<Window> getWindowsFromActivityEvents(String activityName){
        List<ActivityEvent> activityEvents = Collections.unmodifiableList(this.completeActivityEventMap.get(activityName));
        final var windows = new ArrayList<Window>();

        for (var event : activityEvents){
            windows.add(Window.between(event.startTime(), event.endTime()));
        }

        return windows;
    }

    public List<Window> whenActivityDoesNotHaveDuration(String activityName, Duration requiredDuration){
        final var windows = new ArrayList<Window>();
        final var activityWindows = getWindowsFromActivityEvents(activityName);

        for (Window window : activityWindows){
            Duration activityDuration = window.start.durationTo(window.end);;
            if (activityDuration.compareTo(requiredDuration)!=0){
                windows.add(Window.between(window.start, window.end));
            }
        }
        return windows;
    }

    public List<Window> whenActivityHasDurationGreaterThan(String activityName, Duration maximumDuration){
        final var windows = new ArrayList<Window>();
        final var activityWindows = getWindowsFromActivityEvents(activityName);

        for (Window window : activityWindows){
            Duration activityDuration = window.start.durationTo(window.end);;
            if (activityDuration.compareTo(maximumDuration)>0){
                windows.add(Window.between(window.start, window.end));
            }
        }
        return windows;
    }

    public List<Window> whenActivityHasDurationLessThan(String activityName, Duration minimumDuration){
        final var windows = new ArrayList<Window>();
        final var activityWindows = getWindowsFromActivityEvents(activityName);

        for (Window window : activityWindows){
            Duration activityDuration = window.start.durationTo(window.end);;
            if (activityDuration.compareTo(minimumDuration)<0){
                windows.add(Window.between(window.start, window.end));
            }
        }
        return windows;
    }

}
