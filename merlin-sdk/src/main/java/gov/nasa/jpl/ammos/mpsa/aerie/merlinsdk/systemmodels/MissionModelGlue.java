package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public interface MissionModelGlue {

    public static class Registry{

        //in the future we can have multiple event logs

        private static final Map<Pair<SystemModel, String>, Supplier<?>> modelToSupplierMap = new HashMap<>();
        private static final Map<SystemModel, List<Event<?>>> modelToEventLog = new HashMap<>();

        public static void addEvent(SystemModel model,  Event<?> event){
            if(modelToEventLog.containsKey(model)){
                modelToEventLog.get(model).add(event);
            }
            else {
                List<Event<?>> eventList = new ArrayList<>();
                eventList.add(event);
                modelToEventLog.put(model, eventList);
            }
        }

        public static void provide(SystemModel model, String stateName, Supplier<?> supplier) {
            Pair<SystemModel, String> key = new Pair<>(model, stateName);
            modelToSupplierMap.put(key, supplier);

        }

        public static Supplier<?> getSupplier(SystemModel model, String stateName){
            Pair<SystemModel, String> key = new Pair<>(model, stateName);
            return modelToSupplierMap.get(key);
        }

        public static List<Event<?>> getEventLog(SystemModel model){
            return modelToEventLog.get(model);
        }
    }

    public static class EventApplier{

        public static Double get(Supplier<Double> supplier){



            return supplier.get();
        }
    }



}
