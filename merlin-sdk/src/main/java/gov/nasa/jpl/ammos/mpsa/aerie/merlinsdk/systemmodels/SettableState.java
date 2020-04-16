package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

import java.util.Map;

public class SettableState<T> implements State<T> {
    private String name;
    private SystemModel dependentSystemModel;
    private EventLog eventLog;

    public SettableState(String name, SystemModel dependentSystemModel, EventLog eventLog){
        this.name = name;
        this.dependentSystemModel = dependentSystemModel;
        dependentSystemModel.mapStateNameToSystemModelName(this.name);
        this.eventLog = eventLog;
    }

    public void set(T value, Instant t){
        SettableEvent<T> event = new SettableEvent<>(this.name, value, t);
        eventLog.addEvent(event);
    }

    @Override
    public T get() {
        var masterSlice = dependentSystemModel.getMasterSystemModel().getInitialMasterSlice();
        dependentSystemModel.getRegistry().applyStateEvents(dependentSystemModel, masterSlice, eventLog);

        return masterSlice.getState(name);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Map<Instant, T> getHistory() {
        return null;
    }
}
