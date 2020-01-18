package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

import java.util.Map;
import java.util.function.Supplier;

public class SettableState<T> implements State<T> {

    private String name;
    private SystemModel dependentSystemModel;

    public SettableState(String name, SystemModel dependentSystemModel){
        this.name = name;
        this.dependentSystemModel = dependentSystemModel;
    }

    public void set(T value, Time t){
        SettableEvent<T> event = new SettableEvent<>(this.name, value, t);
        MissionModelGlue.Registry.addEvent(dependentSystemModel, event);
    }

    @Override
    public T get() {
        Supplier<?> supplier = MissionModelGlue.Registry.getSupplier(dependentSystemModel, this.name);
        return (T) supplier.get();
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
