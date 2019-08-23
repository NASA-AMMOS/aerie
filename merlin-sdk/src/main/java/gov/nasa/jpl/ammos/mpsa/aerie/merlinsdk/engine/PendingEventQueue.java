package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import java.util.PriorityQueue;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityThread;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

/**
 * A priority queue of activity threads organized by event time
 * 
 * @param <T> the type of the adapter-provided state index structure
 */
public class PendingEventQueue<T extends StateContainer> extends PriorityQueue<ActivityThread<T>> {
    
    /**
     * Returns the event time of the first element in the queue
     *
     * @return the event time of the first element in the queue
     */
    public Time getNextEventTime() {
        return this.peek().getEventTime();
    }
    
}
