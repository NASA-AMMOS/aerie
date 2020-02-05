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
    public final Function<Slice, ResourceType> getter;

    public Getter(final Class<ResourceType> resourceClass, final Function<Slice, ResourceType> getter) {
        this.resourceClass = resourceClass;
        this.getter = getter;
    }

    public ResourceType apply(final Slice slice) {
        return this.getter.apply(slice);
    }
}

class Setter<StimulusType> {
    public final Class<StimulusType> stimulusClass;
    public final BiConsumer<Slice, StimulusType> setter;

    public Setter(final Class<StimulusType> stimulusClass, final BiConsumer<Slice, StimulusType> setter) {
        this.stimulusClass = stimulusClass;
        this.setter = setter;
    }

    public void accept(final Slice slice, final Stimulus stimulus) {
        if (!stimulusClass.isInstance(stimulus)) {
            throw new ClassCastException("Type mismatch!");
        }

        setter.accept(slice, stimulusClass.cast(stimulus));
    }
}

interface ResourceRegistrar<SliceType extends Slice> {
    <ResourceType>
    void provideResource(
        final String resourceName,
        final Class<ResourceType> resourceClass,
        final Function<SliceType, ResourceType> getter);
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
    private final Map<Slice, List<Event>> modelToEventLog = new HashMap<>();

    private final Map<String, Pair<Class<? extends Slice>, Getter<?>>> stateToGetter = new HashMap<>();
    private final Map<String, Slice> stateToModel = new HashMap<>();

    private static final Instant startTime = SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS);

    public Instant getStartTime() {
        return startTime;
    }

    public List<Event> getEventLog(final Slice initialSlice) {
        return List.copyOf(modelToEventLog.get(initialSlice));
    }

    public <SliceType extends Slice, ResourceType> Getter<ResourceType> getGetter(
        final SliceType slice, final String stateName, final Class<ResourceType> resourceClass
    ) {
        final var entry = stateToGetter.get(stateName);

        if (!entry.getLeft().isInstance(slice)) return null;

        // TODO: Use `resourceClass` to check that the Getter provides the expected type of resource.
        return (Getter<ResourceType>) entry.getRight();
    }

    public void addEvent(final Instant time, final String resourceName, final Stimulus stimulus) {
        final var initialSlice = this.stateToModel.get(resourceName);

        modelToEventLog
            .computeIfAbsent(initialSlice, k -> new ArrayList<>())
            .add(new Event(time, resourceName, stimulus));
    }

    public <SliceType extends Slice> void registerModel(final SliceType slice, final Consumer<ResourceRegistrar<SliceType>> registrant) {
        registrant.accept(new Registrar<>(slice, (Class<SliceType>)slice.getClass()));
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

        final var slice = stateToModel.get(stateName);
        return new CumulableState<>(this, stateName, resourceClass, slice);
    }

    private final class Registrar<SliceType extends Slice> implements ResourceRegistrar<SliceType> {
        private final SliceType slice;
        private final Class<SliceType> sliceClass;

        public Registrar(final SliceType slice, final Class<SliceType> sliceClass) {
            this.slice = slice;
            this.sliceClass = sliceClass;
        }

        public <ResourceType> void provideResource(
            final String resourceName,
            final Class<ResourceType> resourceClass,
            final Function<SliceType, ResourceType> getter
        ) {
            stateToModel.put(resourceName, slice);
            stateToGetter.put(
                resourceName,
                Pair.of(sliceClass, new Getter<>(resourceClass, s -> getter.apply(sliceClass.cast(s)))));
        }
    }
}
