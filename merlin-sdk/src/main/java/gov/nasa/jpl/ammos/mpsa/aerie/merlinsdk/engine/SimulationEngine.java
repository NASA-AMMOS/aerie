package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityThread;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.SettableState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

public class SimulationEngine<T extends StateContainer> {

    //TODO: various detachments/map deletions to encourage garbage collection of old events

    Time currentSimulationTime;
    PendingEventQueue pendingEventQueue;
    Map<Activity<T>, ActivityThread<T>> activityToThreadMap;
    Map<Activity<T>, List<Activity<T>>> parentChildMap;
    Map<ActivityThread<T>, List<ActivityThread<T>>> activityListenerMap;
    Thread engineThread;
    T states;

    public void simulate() {
        engineThread = Thread.currentThread();

        while (!pendingEventQueue.isEmpty()) {
            // TODO: figure out the right usage here to avoid casts

            ActivityThread<T> thread = (ActivityThread<T>) pendingEventQueue.remove();
            // ActivityThread<T> thread = pendingEventQueue.remove();
            System.out.println("Dequeued activity thread: " + thread.toString());

            System.out.println("Advancing from T=" + currentSimulationTime.toString() + " to T=" + thread.getEventTime());
            currentSimulationTime = thread.getEventTime();

            this.executeActivity(thread);

            System.out.println("============ EFFECT MODEL COMPLETE OR PAUSED ============");
            System.out.println();
        }
    }

    public SimulationEngine(Time simulationStartTime, List<ActivityThread<T>> activityThreads, T states) {
        this.states = states;
        registerStates(states.getStateList());

        currentSimulationTime = simulationStartTime;
        pendingEventQueue = new PendingEventQueue();
        activityToThreadMap = new HashMap<>();
        parentChildMap = new HashMap<>();
        activityListenerMap = new HashMap<>();

        for (ActivityThread<T> thread: activityThreads) {
            pendingEventQueue.add(thread);
            activityToThreadMap.put(thread.getActivity(), thread);
        }
    }

    public void dispatchContext(ActivityThread<T> activityThread) {
        SimulationContext<T> ctx = new SimulationContext<>(this, activityThread);
        activityThread.setContext(ctx);
        activityThread.setStates(states);
    }

    public Time getCurrentSimulationTime() {
        return this.currentSimulationTime;
    }

    public void executeActivity(ActivityThread<T> thread) {
        ControlChannel channel;

        if (thread.isSuspended()) {
            channel = thread.getChannel();
            System.out.println("Resuming activity thread");
        } else {
            this.dispatchContext(thread);
            channel = new ControlChannel();
            thread.execute(channel);
            System.out.println("Starting activity thread");
        }
        System.out.println("Main thread yielding control!");
        channel.yieldControl();
        channel.takeControl();
        System.out.println("Main thread taking control!");
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

}