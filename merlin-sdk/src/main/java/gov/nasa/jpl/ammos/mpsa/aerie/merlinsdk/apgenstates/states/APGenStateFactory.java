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
        addState(name, value);
        return new ConsumableState(name, value, this);
    }

    public SettableState createSettableState(String name, double value){
        addState(name, value);
        return new SettableState(name, value, this);
    }

    public void setGraph(EventGraph<Event> eventGraph){
        this.eventGraph = eventGraph;
    }

    public StateModel model(){
        return this.stateModel;
    }

    private void addState(String name, double initialValue){ this.model().addState(name, initialValue);}

    public EventGraph<Event> graph(){
        return this.eventGraph;
    }

    public StateModelProjection projection(){
        return this.stateModelProjection;
    }

    public void add(String name, double delta){
        EventGraph<Event> newSegment = EventGraph.sequentially(this.graph(), EventGraph.atom(Event.add(name, delta)));
        this.setGraph(newSegment);
    }

    public void set(String name, double value){
        EventGraph<Event> newSegment = EventGraph.sequentially(this.graph(), EventGraph.atom(Event.set(name, value)));
        this.setGraph(newSegment);
    }

    public double get(String name){
        var forkedStateModel = this.projection().fork(this.model());
        this.graph().evaluate(this.projection()).apply(forkedStateModel);

        return forkedStateModel.getState(name).get();
    }
}
