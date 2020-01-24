package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
        Function<?,?> getter = dependentSystemModel.getRegistry().getGetter(dependentSystemModel, name);
        List<Event> eventLog = MissionModelGlue.Registry.getEventLog(dependentSystemModel);
        MissionModelGlue.applyEvents(dependentSystemModel.getSlice(), dependentSystemModel, eventLog);
        return (T) getter.apply(dependentSystemModel.getSlice());

        /*
        Supplier<?> supplier = MissionModelGlue.Registry.getSupplier(dependentSystemModel, this.name);
        List<Event> eventLog = MissionModelGlue.Registry.getEventLog(dependentSystemModel);
        MissionModelGlue.EventApplier.applyEvents(dependentSystemModel, eventLog);
        return (T) supplier.get();*/
        //generic cast acts as a check
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
