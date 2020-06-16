package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.Constraint;

import java.util.function.Function;
import java.util.function.Predicate;

public class SettableState {

    private final String name;
    private final APGenStateFactory factory;

    public SettableState(String name, double value, APGenStateFactory factory){
        this.name = name;
        this.factory = factory;
    }

    public void set(double value){
        this.factory.set(this.name, value);
    }

    public double get(){
        return this.factory.get(this.name);
    }

    public Constraint when(Predicate<Double> lambda){
        var windows = this.factory.stateThreshold(this.name, lambda);
        return () -> windows;
    }

}
