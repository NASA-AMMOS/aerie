package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.events.Event;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;

import java.util.List;
import java.util.function.Predicate;

public class APGenStateFactory {

    private EventGraph<Event> eventGraph;
    private final StateModel stateModel;
    private final StateEffectEvaluator stateModelProjection;
    private final StateModelApplicator stateModelApplicator;

    public APGenStateFactory(){
        stateModelProjection = new StateEffectEvaluator();
        stateModelApplicator = new StateModelApplicator();
        eventGraph = EventGraph.empty();
        stateModel = stateModelApplicator.initial();
    }

    public ConsumableState createConsumableState(String name, double value){
        addState(name, value);
        return new ConsumableState(name, value, this);
    }

    public SettableState createSettableState(String name, double value){
        addState(name, value);
        return new SettableState(name, value, this);
    }

    private void addState(String name, double initialValue){ this.stateModel.addState(name, initialValue);}

    public EventGraph<Event> graph(){
        return this.eventGraph;
    }

    public void add(String name, double delta){
        this.eventGraph = EventGraph.sequentially(this.graph(), EventGraph.atom(Event.add(name, delta)));

        double result = this.get(name);
        this.stateModel.logChangedValue(name, result);
    }

    public void set(String name, double value){
        this.eventGraph = EventGraph.sequentially(this.graph(), EventGraph.atom(Event.set(name, value)));

        double result = this.get(name);
        this.stateModel.logChangedValue(name, result);
    }

    public List<Window> stateThreshold(String name, Predicate<Double> lambda){
        return this.stateModel.stateThreshold(name, lambda);
    }

    public double get(String name){
        var forkedStateModel = this.stateModelApplicator.duplicate(this.stateModel);
        this.stateModelApplicator.apply(forkedStateModel, this.eventGraph.evaluate(this.stateModelProjection));

        return forkedStateModel.getState(name).get();
    }

    public void step(Duration duration){
        this.stateModel.step(duration);
    }
}

