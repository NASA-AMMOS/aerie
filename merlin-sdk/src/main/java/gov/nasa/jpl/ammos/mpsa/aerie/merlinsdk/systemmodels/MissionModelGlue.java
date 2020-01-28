package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;


class Getter<T> {
    public final Class<T> klass;
    public final Function<Slice, T> getter;

    public Getter(Class<T> klass, Function<Slice, T> getter) {
        this.klass = klass;
        this.getter = getter;
    }

    public T apply(Slice slice) {
        return (T) this.getter.apply(slice);
    }
}

class Setter<T> {
    public final Class<T> klass;
    public final BiConsumer<Slice, T> setter;

    public Setter(Class<T> klass, BiConsumer<Slice, T> setter) {
        this.klass = klass;
        this.setter = setter;
    }

    public void accept(Slice slice, Object x) {
        if (!klass.isInstance(x)) {
            throw new ClassCastException("Type mismatch!");
        }

        setter.accept(slice, klass.cast(x));
    }
}

public class MissionModelGlue {

    private Registry registry;
    private EventApplier eventApplier;

    public MissionModelGlue(){
        registry = this.new Registry();
        eventApplier = this.new EventApplier();
    }

    public Registry registry(){
        return this.registry;
    }

    public EventApplier eventApplier(){
        return this.eventApplier;
    }

    public class Registry{

        private Map<SystemModel, List<Event>> modelToEventLog = new HashMap<>();

        private Map<Pair<SystemModel, String>, Getter<?>> modelToGetter = new HashMap<>();

        private Map<Pair<SystemModel, String>, Setter<?>> modelToSetter = new HashMap<>();

        public Getter getGetter(SystemModel model,  String stateName){
            Pair<SystemModel, String> key = new Pair<>(model, stateName);
            return modelToGetter.get(key);
        }

        public Setter getSetter(SystemModel model,  String stateName){
            Pair<SystemModel, String> key = new Pair<>(model, stateName);
            return modelToSetter.get(key);
        }

        public void addEvent(SystemModel model,  Event<?> event){
            if(modelToEventLog.containsKey(model)){
                modelToEventLog.get(model).add(event);
            }
            else {
                List<Event> eventList = new ArrayList<>();
                eventList.add(event);
                modelToEventLog.put(model, eventList);
            }
        }

        public <T> void registerGetter(SystemModel model,  String stateName, Class<T> resourceType,
                                       Function<Slice, T> getter){
            Pair<SystemModel, String> key = new Pair<>(model, stateName);
            modelToGetter.put(key, new Getter<>(resourceType, getter));
        }

        public <T> void registerSetter(SystemModel model, String stateName, Class<T> resourceType,
                                       BiConsumer<Slice,T> setter){
            Pair<SystemModel, String> key = new Pair<>(model, stateName);
            modelToSetter.put(key, new Setter<>(resourceType, setter));
        }

        public <T> void provideSettable(SystemModel model, String stateName, Class<T> resourceType,
                                        BiConsumer<Slice,T> setter, Function<Slice,T> getter){
            registerGetter(model, stateName, resourceType, getter);
            registerSetter(model, stateName, resourceType, setter);
        }

        public List<Event> getEventLog(SystemModel model){
            return modelToEventLog.get(model);
        }
    }

    public class EventApplier{

        public Slice applyEvents(Slice initialSlice, SystemModel model, List<Event> eventLog){
            Registry registry = model.getRegistry();
            Slice slice = initialSlice.cloneSlice();

            for (Event<?> event : eventLog){
                Duration dt = event.time().subtract(slice.time());
                model.step(slice, dt);
                registry.getSetter(model, event.name()).accept(slice, event.value());
                slice.setTime(event.time());
                slice.printSlice();
            }

            return slice;
        }
    }
}
