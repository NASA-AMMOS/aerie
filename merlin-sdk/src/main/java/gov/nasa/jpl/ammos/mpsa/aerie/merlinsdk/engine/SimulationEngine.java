package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityThread;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.SettableState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

public class SimulationEngine<T extends StateContainer> {

    //TODO: various detachments/map deletions to encourage garbage collection of old events

    private Time currentSimulationTime;
    private PendingEventQueue<T> pendingEventQueue;
    private Map<Activity<T>, ActivityThread<T>> activityToThreadMap;
    private Map<Activity<T>, List<Activity<T>>> parentChildMap;
    private Map<Activity<T>, Duration> activityDurationMap;
    private Map<ActivityThread<T>, List<ActivityThread<T>>> activityListenerMap;
    private Thread engineThread;
    private T states;

    public void simulate() {
        engineThread = Thread.currentThread();

        while (!pendingEventQueue.isEmpty()) {
            ActivityThread<T> thread = pendingEventQueue.remove();
            currentSimulationTime = thread.getEventTime();
            this.executeActivity(thread);
        }
    }

    public SimulationEngine(Time simulationStartTime, List<ActivityThread<T>> activityThreads, T states) {
        this.states = states;
        registerStates(states.getStateList());

        currentSimulationTime = simulationStartTime;
        pendingEventQueue = new PendingEventQueue<>();
        activityToThreadMap = new HashMap<>();
        parentChildMap = new HashMap<>();
        activityListenerMap = new HashMap<>();
        activityDurationMap = new HashMap<>();

        for (ActivityThread<T> thread: activityThreads) {
            pendingEventQueue.add(thread);
            activityToThreadMap.put(thread.getActivity(), thread);
        }
    }

    public void dispatchContext(ActivityThread<T> activityThread) {
        FullContext<T> ctx = new FullContext<>(this, activityThread);
        activityThread.setContext(ctx);
        activityThread.setStates(states);
    }

    public Time getCurrentSimulationTime() {
        return this.currentSimulationTime;
    }

    public void executeActivity(ActivityThread<T> thread) {
        ControlChannel channel;

        if (thread.hasStarted()) {
            // resume activity
            channel = thread.getChannel();
        } else {
            // start activity
            this.dispatchContext(thread);
            channel = new ControlChannel();
            thread.execute(channel);
        }
        channel.yieldControl();
        channel.takeControl();
    }

    public void addParentChildRelationship(Activity<T> parent, Activity<T> child) {
        List<Activity<T>> childList = parentChildMap.get(parent);
        if (childList == null) {
            List<Activity<T>> list = new ArrayList<>();
            list.add(child);
            parentChildMap.put(parent, list);
        } else {
            childList.add(child);
        }
    }

    public void addActivityListener(ActivityThread<T> target, ActivityThread<T> listener) {
        List<ActivityThread<T>> listenerList = activityListenerMap.get(target);
        if (listenerList == null) {
            List<ActivityThread<T>> list = new ArrayList<>();
            list.add(listener);
            activityListenerMap.put(target, list);
        } else {
            listenerList.add(listener);
        }
    }

    public void registerStates(List<SettableState<?>> stateList) {
        for (SettableState<?> state : stateList) {
            state.setEngine(this);
        }
    }

    public void insertIntoQueue(ActivityThread<T> activityThread) {
        pendingEventQueue.add(activityThread);
    }

    public void registerActivityAndThread(Activity<T> activity, ActivityThread<T> activityThread) {
        activityToThreadMap.put(activity, activityThread);
    }

    public ActivityThread<T> getActivityThread(Activity<T> activity) {
        return activityToThreadMap.get(activity);
    }

    public List<ActivityThread<T>> getActivityListeners(ActivityThread<T> thread) {
        return activityListenerMap.get(thread);
    }

    public List<Activity<T>> getActivityChildren(Activity<T> activity) {
        return parentChildMap.get(activity);
    }

    public void logActivityDuration(Activity<T> activity, Duration d) {
        activityDurationMap.put(activity, d);
    }

    public Duration getActivityDuration(Activity<T> activity) {
        return activityDurationMap.get(activity);
    }

}