package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.events.Event;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph;

public class APGenStateFactory {

    private EventGraph<Event> eventGraph;
    private final StateModel stateModel;
    private final StateModelProjection stateModelProjection;

    public APGenStateFactory(){
        eventGraph = EventGraph.empty();
        stateModel = new StateModel();
        stateModelProjection = new StateModelProjection();
    }

    public ConsumableState createConsumableState(String name, double value){
        return new ConsumableState(name, value, this);
    }

    public void setGraph(EventGraph<Event> eventGraph){
        this.eventGraph = eventGraph;
    }

    public StateModel model(){
        return this.stateModel;
    }

    public EventGraph<Event> graph(){
        return this.eventGraph;
    }

    public StateModelProjection projection(){
        return this.stateModelProjection;
    }
}
