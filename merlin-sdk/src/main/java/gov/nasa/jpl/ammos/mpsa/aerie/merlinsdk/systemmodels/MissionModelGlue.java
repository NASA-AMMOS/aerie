package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
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
    private MasterSystemModel masterSystemModel;

    public MissionModelGlue(){
        registry = this.new Registry();
    }

    private Map<String, String> stateNameToSystemModelName = new HashMap<>();

    public void mapStateNameToSystemModelName(String stateName, String systemModelname){
        stateNameToSystemModelName.put(stateName, systemModelname);
    }

    public String getSystemModelNameFromStateName(String stateName){
        return stateNameToSystemModelName.get(stateName);
    }

    public Registry registry(){
        return this.registry;
    }

    public void createMasterSystemModel(Instant time, SystemModel... models){
        masterSystemModel = this.new MasterSystemModel(time, models);
    }

    public MasterSystemModel MasterSystemModel() { return this.masterSystemModel; }

    public class MasterSystemModel{

        private List<SystemModel> systemModels = new ArrayList<>();

        private MasterSlice initialMasterSlice;

        MasterSystemModel(Instant initialTime, SystemModel... models){

            List<Pair<String,Slice>> initialSlices = new ArrayList<>();

            for (var model : models){
                systemModels.add(model);
                initialSlices.add(Pair.of(model.getName(), model.getInitialSlice()));
            }

            initialMasterSlice = new MasterSlice(initialTime,  initialSlices.toArray(new Pair[initialSlices.size()]));
        }

        //each get() on a state returns a cloned initial slice, compuation begins from initial slice by construction
        public MasterSlice getInitialMasterSlice(){
            return this.initialMasterSlice.cloneSlice();
        }

        public SystemModel getSystemModel(String systemModelName){
            for(var x : this.systemModels){
                if (x.getName().equals(systemModelName)){
                    return x;
                }
            }
            throw new RuntimeException("system model does not exist");
        }

        public void step(Slice aSlice, Duration dt){
            MasterSlice masterSlice = (MasterSlice) aSlice;

            //slice is stored in a map and modified in place
            for (var model : systemModels){
                Slice slice = masterSlice.systemModelNameToSlice.get(model.getName());
                model.step(slice, dt);
                masterSlice.time = slice.time();
            }

        }

        public class MasterSlice implements Slice{
            private Instant time;
            private Map<String, Slice> systemModelNameToSlice = new HashMap<>();

            public MasterSlice(Instant time, Pair<String, Slice>... namedSlices){
                this.time = time;

                for (var pair : namedSlices){
                    this.systemModelNameToSlice.put(pair.getKey(), pair.getValue());
                }
            }

            public String getSystemModelName(String stateName){
                return MissionModelGlue.this.getSystemModelNameFromStateName(stateName);
            }

            public <T> T getState(String stateName){
                String systemModelName = getSystemModelName(stateName);

                Slice slice = systemModelNameToSlice.get(systemModelName);
                SystemModel model = getSystemModel(systemModelName);

                Getter<T> getter = registry.getGetter(model, stateName);
                return getter.apply(slice);
            }

            public Slice getSlice(String name){
                return systemModelNameToSlice.get(name);
            }

            @Override
            public Instant time() {
                return this.time;
            }

            @Override
            public void setTime(Instant time) {
                this.time = time;
            }

            public MasterSlice cloneSlice() {
                List<Pair<String, Slice>> clonedMasterSlice = new ArrayList<>();

                for (Map.Entry<String, Slice> entry : this.systemModelNameToSlice.entrySet()){
                    clonedMasterSlice.add(Pair.of(entry.getKey(), entry.getValue().cloneSlice()));
                }

                return new MasterSlice(time, clonedMasterSlice.toArray(new Pair[clonedMasterSlice.size()]));
            }

        }
    }

    public class Registry{

        private Map<Pair<SystemModel, String>, Getter<?>> modelToGetter = new HashMap<>();

        private Map<Pair<SystemModel, String>, Setter<?>> modelToSetter = new HashMap<>();

        public Getter getGetter(SystemModel model,  String stateName){
            return modelToGetter.get(Pair.of(model, stateName));
        }

        public Setter getSetter(SystemModel model,  String stateName){
            return modelToSetter.get(Pair.of(model, stateName));
        }

        public <T> void registerGetter(SystemModel model,  String stateName, Class<T> resourceType,
                                       Function<Slice, T> getter){
            modelToGetter.put(
                    Pair.of(model, stateName),
                    new Getter<>(resourceType, getter));
        }

        public <T> void registerSetter(SystemModel model, String stateName, Class<T> resourceType,
                                       BiConsumer<Slice,T> setter){
            modelToSetter.put(
                    Pair.of(model, stateName),
                    new Setter<>(resourceType, setter));
        }

        public <T> void provideSettable(SystemModel model, String stateName, Class<T> resourceType,
                                        BiConsumer<Slice,T> setter, Function<Slice,T> getter){
            registerGetter(model, stateName, resourceType, getter);
            registerSetter(model, stateName, resourceType, setter);
        }

        public Map<String, Getter<?>> getStateGetters(){
            var stateGetters = new HashMap<String, Getter<?>>();

            for (var entry : modelToGetter.entrySet()){
                String name = entry.getKey().getRight();
                Getter<?> getter = entry.getValue();

                stateGetters.put(name, getter);
            }
            return stateGetters;
        }

        public void applyStateEvents(SystemModel model, Slice aMasterSlice, EventLog eventLog){

            MasterSystemModel.MasterSlice  masterSlice = (MasterSystemModel.MasterSlice) aMasterSlice;

            //this can be taken out of the for loop b/c there should only be one master system model for a set of models
            MasterSystemModel masterSystemModel = model.getMasterSystemModel();

            for(Event<?> event : eventLog.getEventLog()){

                if (event.eventType().equals(EventType.ACTIVITY)){
                    continue;
                }

                //step the model up
                Duration dt = event.time().durationFrom(masterSlice.time());
                masterSystemModel.step(masterSlice, dt);

                //now apply event
                String systemModelName = MissionModelGlue.this.getSystemModelNameFromStateName(event.name());
                Setter<?> setter = registry.getSetter(model, event.name());

                Slice slice = masterSlice.getSlice(systemModelName);
                setter.accept(slice, event.value());
            }
        }
    }
}
