package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels.ActivityEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;

import java.util.ArrayList;
import java.util.List;

public class DataActivityQuerier {

    private List<ActivityEvent> eventLog;

    public void provideEvents(List<ActivityEvent> events){
        //probaby should clone this so issues don't arise if we modify later
        eventLog = events;
    }

    public List<Window> whenActivityExists(){
        final var windows = new ArrayList<Window>();

        for (ActivityEvent event : eventLog){
            windows.add(Window.between(event.startTime(), event.endTime()));
        }
        return windows;
    }
}
