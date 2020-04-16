package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.events;

import java.util.*;

public class EventLog {

    /*
    Categories of event produers:
    Reactors -> Accept events and generate their own events
    Accumulators -> accept events but do not create their own events

    Specific things that can produce events:
    States when changed
    System Models

    What are the event types?
    ActivityEvents -> an event that captures information about the duration and starttime of an activity instance, along with other data
    SettableEvents -> an event that changes the value of a state

    What is the purpose of the EventLog?
    1. To store these events.
    2. To retrieve requested events.
     */

    /*----------- functionality one: storage of events -----------*/

    private List<Event> eventLog = new ArrayList<>();

    public void addEvent(Event<?> event){
        this.eventLog.add(event);
    }

    public List<Event> getEventLog(){
        return Collections.unmodifiableList(this.eventLog);
    }


    /*----------- functionality two: targeted event retrieval -----------*/

    public List<ActivityEvent> getAllEventsForActivity(String name){
        ArrayList<ActivityEvent> activityEvents = new ArrayList<>();

        for (Event<?> event : this.eventLog){
            if(event.eventType().equals(EventType.ACTIVITY) && event.name().equals(name)){
                activityEvents.add((ActivityEvent) event);
            }
        }
        return Collections.unmodifiableList(activityEvents);
    }

    public Map<String, List<ActivityEvent>> getCompleteActivityMap(){
        Map<String, List<ActivityEvent>> activityMap = new HashMap<>();

        for (Event<?> event : this.eventLog){
            activityMap.computeIfAbsent(event.name(), value -> new ArrayList<>())
                    .add((ActivityEvent) event);
        }

        return Collections.unmodifiableMap(activityMap);
    }

    public List<SettableEvent> getAllSettableEvents(){
        ArrayList<SettableEvent> settableEvents = new ArrayList<>();

        for (Event<?> event : this.eventLog){
            if(event.eventType().equals(EventType.SETTABLE)){
                settableEvents.add((SettableEvent) event);
            }
        }
        return Collections.unmodifiableList(settableEvents);
    }
}
