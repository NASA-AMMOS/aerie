package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

import java.util.List;
import java.util.Map;

public class SettableState<T> implements State<T> {
    private String name;
    private SystemModel dependentSystemModel;

    public SettableState(String name, SystemModel dependentSystemModel){
        this.name = name;
        this.dependentSystemModel = dependentSystemModel;
    }

    public void set(T value, Time t){
        SettableEvent<T> event = new SettableEvent<>(this.name, value, t);
        dependentSystemModel.getRegistry().addEvent(dependentSystemModel, event);
    }

    @Override
    public T get() {
        Getter getter = dependentSystemModel.getRegistry().getGetter(dependentSystemModel, name);
        List<Event> eventLog = dependentSystemModel.getRegistry().getEventLog(dependentSystemModel);
        dependentSystemModel.getEventAplier().applyEvents(dependentSystemModel.getSlice(), dependentSystemModel, eventLog);
        return (T) getter.getter.apply(dependentSystemModel.getSlice());
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
