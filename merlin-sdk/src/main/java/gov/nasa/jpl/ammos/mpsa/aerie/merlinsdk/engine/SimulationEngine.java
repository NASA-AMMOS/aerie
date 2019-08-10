package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityThread;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

public class SimulationEngine {

    Time currentSimulationTime;
    PendingEventQueue pendingEventQueue;
    Map<Activity<?>, ActivityThread> activityToThreadMap;
    Thread engineThread;

    public void simulate() {
        engineThread = Thread.currentThread();

        while (!pendingEventQueue.isEmpty()) {
            ActivityThread thread = pendingEventQueue.remove();
            System.out.println("Dequeued activity thread: " + thread.toString());

            System.out.println("Advancing from T=" + currentSimulationTime.toString() + " to T=" + thread.getEventTime());
            currentSimulationTime = thread.getEventTime();

            this.executeActivity(thread);

            System.out.println("============ EFFECT MODEL COMPLETE OR PAUSED ============");
            System.out.println();
        }
    }

    public SimulationEngine(Time simulationStartTime, List<ActivityThread> activityThreads) {
        currentSimulationTime = simulationStartTime;
        pendingEventQueue = new PendingEventQueue();
        activityToThreadMap = new HashMap<>();

        for (ActivityThread thread: activityThreads) {
            pendingEventQueue.add(thread);
            activityToThreadMap.put(thread.getActivity(), thread);
        }
    }

    public void dispatchContext(ActivityThread activityThread) {
        // TODO: see if we need to detach from this later
        SimulationContext ctx = new SimulationContext(this, activityThread);
        activityThread.setContext(ctx);
    }

    public Time getCurrentSimulationTime() {
        return this.currentSimulationTime;
    }

    public void executeActivity(ActivityThread thread) {
        ControlChannel channel;

        if (thread.isSuspended()) {
            channel = thread.getChannel();
        } else {
            this.dispatchContext(thread);
            channel = new ControlChannel();
            thread.execute(channel);
        }
        System.out.println("Main thread yielding control (in `executeActivity()`)!");
        channel.yieldControl();
        channel.takeControl();
        System.out.println("Main thread taking control (in `executeActivity()`)!");
    }

}