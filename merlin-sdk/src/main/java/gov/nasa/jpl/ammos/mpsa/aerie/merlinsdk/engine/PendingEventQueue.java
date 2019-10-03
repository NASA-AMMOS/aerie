package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import java.util.PriorityQueue;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityJob;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

/**
 * A priority queue of activity jobs organized by event time
 * 
 * @param <T> the type of the adapter-provided state index structure
 */
public class PendingEventQueue<T extends StateContainer> extends PriorityQueue<ActivityJob<T>> {
    
    /**
     * Returns the event time of the first element in the queue
     *
     * @return the event time of the first element in the queue
     */
    public Time getNextEventTime() {
        return this.peek().getEventTime();
    }
    
}
