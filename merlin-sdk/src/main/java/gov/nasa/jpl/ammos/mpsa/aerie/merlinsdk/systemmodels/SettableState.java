package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

abstract class EventBasedState<ResourceType> implements State<ResourceType> {
    protected final String name;
    protected final Class<ResourceType> resourceClass;
    protected final Registry registry;
    protected final SystemModel<?> systemModel;

    public EventBasedState(
        final Registry registry,
        final String name,
        final Class<ResourceType> resourceClass,
        final SystemModel<?> systemModel
    ) {
        this.registry = registry;
        this.name = name;
        this.resourceClass = resourceClass;
        this.systemModel = systemModel;
    }

    @Override
    public final String getName() {
        return this.name;
    }

    @Override
    public final ResourceType get() {
        final var slice = this.registry.getInitialSlice(this.systemModel);
        final var getter = this.registry.getGetter(slice, this.name, this.resourceClass);

        // TODO: Obtain a simulation context from some central authority (who itself is provided via constructor).
        //   This central authority is probably responsible for performing simulation and choosing an optimal position
        //   to resume simulation from. We're currently just re-simulating everything on every `get`.
        var simulationTime = this.registry.getStartTime();
        for (final Event event : this.registry.getEventLog(this.systemModel)) {
            // Step the slice forward to the next event time.
            if (simulationTime.isBefore(event.time)) {
                slice.step(event.time.durationFrom(simulationTime));
                simulationTime = event.time;
            }

            // Apply the event to the slice.
            slice.react(event.resourceName, event.stimulus);
        }

        return resourceClass.cast(getter.apply(slice));
    }

    @Override
    public final Map<Instant, ResourceType> getHistory() {
        final var stateHistory = new TreeMap<Instant, ResourceType>();

        final var slice = this.registry.getInitialSlice(this.systemModel);
        final var getter = this.registry.getGetter(slice, this.name, resourceClass);

        var simulationTime = this.registry.getStartTime();
        stateHistory.put(simulationTime, getter.apply(slice));
        for (final var event : this.registry.getEventLog(this.systemModel)) {
            // Step the slice forward to the next event time.
            if (simulationTime.isBefore(event.time)) {
                slice.step(event.time.durationFrom(simulationTime));
                simulationTime = event.time;
            }

            // Apply the event to the slice.
            slice.react(event.resourceName, event.stimulus);

            // Record the current value of this resource in the slice.
            if (Objects.equals(event.resourceName, this.name)) {
                stateHistory.put(event.time, getter.apply(slice));
            } else {
                stateHistory.put(event.time, getter.apply(slice));
            }
        }

        return stateHistory;
    }
}

public final class SettableState<ResourceType> extends EventBasedState<ResourceType> {
    public SettableState(
        final Registry registry,
        final String name,
        final Class<ResourceType> resourceClass,
        final SystemModel<?> dependentSystemModel
    ) {
        super(registry, name, resourceClass, dependentSystemModel);
    }

    public void set(final ResourceType value, final Instant t) {
        // TODO: Obtain the "current" simulation time from some central authority (who itself is provided via constructor).
        this.registry.addEvent(t, this.name, new SetStimulus(value));
    }
}

class CumulableState<ResourceType, DeltaType> extends EventBasedState<ResourceType> {
    private final Class<DeltaType> deltaClass;

    public CumulableState(
        final Registry registry,
        final String name,
        final Class<ResourceType> resourceClass,
        final Class<DeltaType> deltaClass,
        final SystemModel<?> dependentSystemModel
    ) {
        super(registry, name, resourceClass, dependentSystemModel);
        this.deltaClass = deltaClass;
    }

    public void add(final DeltaType delta, final Instant t) {
        // TODO: Obtain the "current" simulation time from some central authority (who itself is provided via constructor).
        this.registry.addEvent(t, this.name, new AccumulateStimulus(delta));
    }
}

class RateState implements State<Double> {
    private final CumulableState<Double, Double> state;

    public RateState(final CumulableState<Double, Double> state) {
        this.state = state;
    }

    @Override
    public Double get() {
        return this.state.get();
    }

    public void increaseBy(final double delta, final Instant t) {
        this.state.add(delta, t);
    }

    public void decreaseBy(final double delta, final Instant t) {
        this.state.add(-delta, t);
    }
}