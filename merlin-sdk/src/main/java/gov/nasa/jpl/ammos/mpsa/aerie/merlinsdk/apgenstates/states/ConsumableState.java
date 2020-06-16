package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.Constraint;

import java.util.function.Function;
import java.util.function.Predicate;

public class ConsumableState {

    private final String name;
    private final APGenStateFactory factory;

    public ConsumableState(String name, double value, APGenStateFactory factory){
        this.name = name;
        this.factory = factory;
    }

    public void add(double delta){
       this.factory.add(this.name, delta);
    }

    public double get(){
        return this.factory.get(this.name);
    }

    public Constraint when(Predicate<Double> lambda){
        var windows = this.factory.stateThreshold(this.name, lambda);
        return () -> windows;
    }

}
