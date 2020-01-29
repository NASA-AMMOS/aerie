package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

import java.util.PriorityQueue;

/**
 * A priority queue of activity jobs organized by event time
 */
public class PendingEventQueue extends PriorityQueue<ActivityJob<?>> {
    
    /**
     * Returns the event time of the first element in the queue
     *
     * @return the event time of the first element in the queue
     */
    public Instant getNextEventTime() {
        return this.peek().getEventTime();
    }
    
}
