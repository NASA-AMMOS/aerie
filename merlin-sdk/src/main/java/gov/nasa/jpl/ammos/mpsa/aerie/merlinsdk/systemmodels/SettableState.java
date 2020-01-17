package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

import java.util.Map;

public class SettableState<T> implements State<T> {

    private String name;
    private SystemModel dependentSystemModel;

    public SettableState(String name, SystemModel dependentSystemModel){
        this.name = name;
        this.dependentSystemModel = dependentSystemModel;
    }

    public void set(Time t, T value){
        SettableEvent<T> event = new SettableEvent<>(this.name, value, t);
        MissionModelGlue.Registry.addEvent(dependentSystemModel, event);
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
