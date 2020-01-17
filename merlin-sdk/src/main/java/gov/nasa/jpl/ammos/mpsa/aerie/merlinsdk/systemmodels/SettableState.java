package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

import java.util.Map;

public class SettableState<T> implements State<T> {

    private String name;
    private Registry registry;
    private EventApplier eventApplier;
    private SystemModel dependentSystemModel;

    public SettableState(String name, Registry registry, EventApplier eventApplier, SystemModel dependentSystemModel){
        this.name = name;
        this.registry = registry;
        this.eventApplier = eventApplier;
        this.dependentSystemModel = dependentSystemModel;
    }

    public void set(Time t, T value){
       SettableEvent<T> event = new SettableEvent<>(this.name, value, t);
    }







    @Override
    public T get() {
        return null;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Map<Time, T> getHistory() {
        return null;
    }
}
