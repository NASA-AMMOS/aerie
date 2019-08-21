package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import java.util.PriorityQueue;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityThread;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

public class PendingEventQueue<T extends StateContainer> extends PriorityQueue<ActivityThread<T>> {

    // FIXME: what is this??
    private static final long serialVersionUID = 1L;
    
    public Time getNextEventTime() {
        return this.peek().getEventTime();
    }
    
}
