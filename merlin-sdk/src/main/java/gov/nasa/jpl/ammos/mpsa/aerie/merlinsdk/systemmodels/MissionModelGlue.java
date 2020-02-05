package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import org.apache.commons.lang3.tuple.Pair;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

class Getter<ResourceType> {
    public final Class<ResourceType> resourceClass;
    public final Function<SystemModel, ResourceType> getter;

    public Getter(final Class<ResourceType> resourceClass, final Function<SystemModel, ResourceType> getter) {
        this.resourceClass = resourceClass;
        this.getter = getter;
    }

    public ResourceType apply(final SystemModel model) {
        return this.getter.apply(model);
    }
}

class Setter<StimulusType> {
    public final Class<StimulusType> stimulusClass;
    public final BiConsumer<SystemModel, StimulusType> setter;

    public Setter(final Class<StimulusType> stimulusClass, final BiConsumer<SystemModel, StimulusType> setter) {
        this.stimulusClass = stimulusClass;
        this.setter = setter;
    }

    public void accept(final SystemModel model, final Stimulus stimulus) {
        if (!stimulusClass.isInstance(stimulus)) {
            throw new ClassCastException("Type mismatch!");
        }

        setter.accept(model, stimulusClass.cast(stimulus));
    }
}

interface ResourceRegistrar<ModelType extends SystemModel> {
    <ResourceType>
    void provideResource(
        final String resourceName,
        final Class<ResourceType> resourceClass,
        final Function<ModelType, ResourceType> getter);
}

final class Event {
    public final Instant time;
    public final String resourceName;
    public final Stimulus stimulus;

    public Event(final Instant time, final String resourceName, final Stimulus stimulus) {
        this.time = time;
        this.resourceName = resourceName;
        this.stimulus = stimulus;
    }
}

final class Registry {
    private final Map<SystemModel, List<Event>> modelToEventLog = new HashMap<>();

    private final Map<String, Pair<Class<? extends SystemModel>, Getter<?>>> stateToGetter = new HashMap<>();
    private final Map<String, SystemModel> stateToModel = new HashMap<>();

    private static final Instant startTime = SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS);

    public Instant getStartTime() {
        return startTime;
    }

    public List<Event> getEventLog(final SystemModel model) {
        return List.copyOf(modelToEventLog.get(model));
    }

    public <ModelType extends SystemModel, ResourceType> Getter<ResourceType> getGetter(
        final ModelType model, final String stateName, final Class<ResourceType> resourceClass
    ) {
        final var entry = stateToGetter.get(stateName);

        if (!entry.getLeft().isInstance(model)) return null;

        // TODO: Use `resourceClass` to check that the Getter provides the expected type of resource.
        return (Getter<ResourceType>) entry.getRight();
    }

    public void addEvent(final Instant time, final String resourceName, final Stimulus stimulus) {
        final var initialModel = this.stateToModel.get(resourceName);

        modelToEventLog
            .computeIfAbsent(initialModel, k -> new ArrayList<>())
            .add(new Event(time, resourceName, stimulus));
    }

    public <ModelType extends SystemModel> void registerModel(final ModelType model, final Consumer<ResourceRegistrar<ModelType>> registrant) {
        registrant.accept(new Registrar<>(model, (Class<ModelType>)model.getClass()));
    }

    public <ResourceType>
    SettableState<ResourceType> getSettable(final String stateName, final Class<ResourceType> resourceClass) {
        final var actualResourceClass = stateToGetter.get(stateName).getRight().resourceClass;
        if (!Objects.equals(resourceClass, actualResourceClass)) {
            // TODO: Throw a finer-grained type of exception.
            throw new RuntimeException(
                "`" + stateName + "` has resource type `" + actualResourceClass.getSimpleName()
                    + "`, not `" + resourceClass.getSimpleName() + "`");
        }

        return new SettableState<>(this, stateName, resourceClass, stateToModel.get(stateName));
    }

    public <ResourceType, DeltaType>
    CumulableState<ResourceType, DeltaType> getCumulable(
        final String stateName,
        final Class<ResourceType> resourceClass,
        final Class<DeltaType> deltaClass
    ) {
        final var actualResourceClass = stateToGetter.get(stateName).getRight().resourceClass;
        if (!Objects.equals(resourceClass, actualResourceClass)) {
            // TODO: Throw a finer-grained type of exception.
            throw new RuntimeException(
                "`" + stateName + "` has resource type `" + actualResourceClass.getSimpleName()
                    + "`, not `" + resourceClass.getSimpleName() + "`");
        }

        // TODO: Verify that the deltaClass is acceptable for the stimulus the resource was defined against.

        final var model = stateToModel.get(stateName);
        return new CumulableState<>(this, stateName, resourceClass, model);
    }

    private final class Registrar<ModelType extends SystemModel> implements ResourceRegistrar<ModelType> {
        private final ModelType model;
        private final Class<ModelType> modelClass;

        public Registrar(final ModelType model, final Class<ModelType> modelClass) {
            this.model = model;
            this.modelClass = modelClass;
        }

        public <ResourceType> void provideResource(
            final String resourceName,
            final Class<ResourceType> resourceClass,
            final Function<ModelType, ResourceType> getter
        ) {
            stateToModel.put(resourceName, model);
            stateToGetter.put(
                resourceName,
                Pair.of(modelClass, new Getter<>(resourceClass, s -> getter.apply(modelClass.cast(s)))));
        }
    }
}
