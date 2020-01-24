package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


class Getter<T> {
    public final Class<T> klass;
    public final Function<Slice, T> getter;

    public Getter(Class<T> klass, Function<Slice, T> getter) {
        this.klass = klass;
        this.getter = getter;
    }
}

class Setter<T> {
    public final Class<T> klass;
    public final BiConsumer<Slice, T> setter;

    public Getter(Class<T> klass, BiConsumer<Slice, T> setter) {
        this.klass = klass;
        this.setter = setter;
    }

    public void accept(Slice slice, Object x) {
        if (!klass.isInstance(x)) {
            throw VeryBadThing("Type mismatch!");
        }

        setter.accept(slice, klass.cast(x));
    }
}

public class MissionModelGlue {

    public class Registry{


        private Map<SystemModel, List<Event>> modelToEventLog = new HashMap<>();

        //returns one value takes in one value (getter)
        private Map<Pair<SystemModel, String>, Getter<?>> modelToGetter = new HashMap<>();
//        private Map<Pair<SystemModel, String>, Function<Slice,?>> modelToGetter = new HashMap<>();

        //returns no value takes in two values (setter)
        private Map<Pair<SystemModel, String>, BiConsumer<Slice,?>> modelToSetter = new HashMap<>();

        public Getter getGetter(SystemModel model,  String stateName){
            Pair<SystemModel, String> key = new Pair<>(model, stateName);
            return modelToGetter.get(key);
        }

        public BiConsumer<Slice,?> getSetter(SystemModel model,  String stateName){
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

        public <T> void registerGetter(SystemModel model,  String stateName, Class<T> resourceType, Function<Slice, T> getter){
            Pair<SystemModel, String> key = new Pair<>(model, stateName);
            modelToGetter.put(key, new Getter<>(resourceType, getter));
        }

        public void registerSetter(SystemModel model,  String stateName, BiConsumer<Slice,?> setter){
            Pair<SystemModel, String> key = new Pair<>(model, stateName);
            modelToSetter.put(key, setter);
        }


        public List<Event> getEventLog(SystemModel model){
            return modelToEventLog.get(model);
        }


        //private static final Map<Pair<SystemModel, String>, Supplier<?>> modelToSupplierMap = new HashMap<>();
        //private static final Map<Pair<SystemModel, String>, Consumer<?>> modelToConsumerMap = new HashMap<>();


/*

        public static void provide(SystemModel model, String stateName, Supplier supplier) {
            Pair<SystemModel, String> key = new Pair<>(model, stateName);
            modelToSupplierMap.put(key, supplier);

        }

        public static void provideDouble(SystemModel model,  String stateName, Consumer<Double> consumer){
            Pair<SystemModel, String> key = new Pair<>(model, stateName);
            modelToConsumerMap.put(key, consumer);
        }

        public static void provideString(SystemModel model,  String stateName, Consumer<String> consumer){
            Pair<SystemModel, String> key = new Pair<>(model, stateName);
            modelToConsumerMap.put(key, consumer);
        }

        public static Supplier<?> getSupplier(SystemModel model, String stateName){
            Pair<SystemModel, String> key = new Pair<>(model, stateName);
            return modelToSupplierMap.get(key);
        }

        public static Consumer<?> getConsumer(SystemModel model,  String stateName){
            Pair<SystemModel, String> key = new Pair<>(model, stateName);
            return modelToConsumerMap.get(key);
        }*/
    }


    public void applyEvents(Slice slice, SystemModel model, List<Event> eventLog){
        Registry registry = new Registry();

        if (eventLog.size()<=0){
            return;
        }


        for (Event<?> x : eventLog){
            Duration dt = x.time().subtract(slice.time());
            slice = model.step(slice, dt);
            //BiConsumer<Slice, ?> setter = registry.getSetter(model, x.name());
            //setter.accept(slice, x.value());

            registry.getSetter(model, x.name()).accept(slice, x.value());


            //registry.modelToSetter.get(model, x.name()).accept(slice, x.value());
        }
    }

    /*public static class EventApplier{

        //take slice instead of a system model
        //slice can be cloned and then create a new slice
        //system model takes slice and applies to the system model
        //we can have a slice that knows who it's parent is eventually
        //make custom provider and consumer interfaces
        //system model is only a lookup key for now
        public static void applyEvents(SystemModel model,  List<Event> eventLog){
            if (eventLog.size()<=0){
                return;
            }

            for (int i = 1; i < eventLog.size()-1; i++){
                Event eventLeft = eventLog.get(i-1);
                Event eventRight = eventLog.get(i);
                Duration dt = eventRight.time().subtract(eventLeft.time());

                Consumer consumer = Registry.getConsumer(model, eventLeft.name());
                consumer.accept(eventLeft.value());
                model.saveToSlice();
       //         model.step(dt);
            }
        }
    }*/

}
