package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.events.Event;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph;

public class ConsumableState {

    private final State state;
    private final APGenStateFactory factory;

    public ConsumableState(String name, double value, APGenStateFactory factory){
        this.state = new State(name, value);
        this.factory = factory;
        this.factory.model().addState(this.state);
    }

    public void add(double delta){
        EventGraph<Event> newSegment = EventGraph.sequentially(factory.graph(), EventGraph.atom(Event.add(state.name(), delta)));
        factory.setGraph(newSegment);
    }

    public double get(){
        var forkedStateModel = factory.projection().fork(factory.model());
        factory.graph().evaluate(factory.projection()).apply(forkedStateModel);

        return forkedStateModel.getState(state.name()).get();
    }

}
