package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels.ActivityEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.GeneralSearch.Predicate;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.GeneralSearch.bisectBy;

public class ActivityQuerier {

    //todo: remove this as part of a refactor PR to the previous constraint work
    private List<ActivityEvent> eventLog;

    //todo: remove this as part of a refactor PR to the previous constraint work
    public void provideEvents(List<ActivityEvent> events) {
        eventLog = events;
    }

    private Map<String, List<ActivityEvent>> completeActivityEventMap;


    public void provideActivityEventMap(Map<String, List<ActivityEvent>> completeActivityEventMap) {
        this.completeActivityEventMap = completeActivityEventMap;
    }

    //todo: remove this and make it work with the map
    public List<Window> whenActivityExists() {
        final var windows = new ArrayList<Window>();


        for (ActivityEvent<?> event : eventLog){
            windows.add(Window.between(event.startTime(), event.endTime()));
        }
        return windows;
    }

    public List<Window> getWindowsFromActivityEvents(String activityName) {
        List<ActivityEvent> activityEvents = Collections.unmodifiableList(this.completeActivityEventMap.get(activityName));
        final var windows = new ArrayList<Window>();

        for (var event : activityEvents) {
            windows.add(Window.between(event.startTime(), event.endTime()));
        }

        return windows;
    }

    public List<Window> whenActivityExists(String activityName) {
        return getWindowsFromActivityEvents(activityName);
    }

    public List<Window> whenActivityDoesNotHaveDuration(String activityName, Duration requiredDuration) {
        final var windows = new ArrayList<Window>();
        final var activityWindows = getWindowsFromActivityEvents(activityName);

        for (Window window : activityWindows) {
            Duration activityDuration = window.start.durationTo(window.end);

            if (!activityDuration.equals(requiredDuration)) {
                windows.add(Window.between(window.start, window.end));
            }
        }
        return windows;
    }

    public List<Window> whenActivityHasDurationGreaterThan(String activityName, Duration maximumDuration) {
        final var windows = new ArrayList<Window>();
        final var activityWindows = getWindowsFromActivityEvents(activityName);

        for (Window window : activityWindows) {
            Duration activityDuration = window.start.durationTo(window.end);

            if (activityDuration.longerThan(maximumDuration)) {
                windows.add(Window.between(window.start, window.end));
            }
        }
        return windows;
    }

    public List<Window> whenActivityHasDurationLessThan(String activityName, Duration minimumDuration) {
        final var windows = new ArrayList<Window>();
        final var activityWindows = getWindowsFromActivityEvents(activityName);

        for (Window window : activityWindows) {
            Duration activityDuration = window.start.durationTo(window.end);

            if (activityDuration.shorterThan(minimumDuration)) {
                windows.add(Window.between(window.start, window.end));
            }
        }
        return windows;
    }

    //returns the windows during which the starttime of A equals the starttime of B and the endtime of A equals the endtime of B
    public List<Window> allInstacesAAndBAreEqual(String activityNameA, String activityNameB) {
        var windowsA = getWindowsFromActivityEvents(activityNameA);
        var windowsB = getWindowsFromActivityEvents(activityNameB);

        return new ArrayList<>(CollectionUtils.intersection(windowsA, windowsB));
    }

    //the end of any Instance of Activity A must occur before the start of any B activity
    public List<Window> allABeforeFirstB(final String activityNameA, final String activityNameB){
        final var windowsB = getWindowsFromActivityEvents(activityNameB);
        final var windowsA = getWindowsFromActivityEvents(activityNameA);

        if (windowsB.isEmpty()) return windowsA;

        final var windowB = windowsB.get(0);
        final Predicate<Window> isBeforeB = (window) -> window.end.isBefore(windowB.start);

        return windowsA.subList(0, bisectBy(windowsA, isBeforeB));
    }

}
