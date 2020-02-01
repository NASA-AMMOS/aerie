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
        final var slice = this.systemModel.getInitialSlice();
        final var getter = this.registry.getGetter(slice, this.name, this.resourceClass);

        // TODO: Obtain a simulation context from some central authority (who itself is provided via constructor).
        //   This central authority is probably responsible for performing simulation and choosing an optimal position
        //   to resume simulation from. We're currently just re-simulating everything on every `get`.
        var simulationTime = this.registry.getStartTime();
        for (final Event event : this.registry.getEventLog(this.systemModel)) {
            final var setter = registry.getSetter(slice, event.resourceName);
            if (setter == null) continue;
            // ^^^ Currently, it isn't necessary to skip events for which we don't have a setter,
            // because events are currently partitioned by which system model receives them. So we should
            // never see an event for which we don't have a setter, so long as we perform this partitioning.
            // However, it will eventually become possible for multiple system models to subscribe to events
            // on a single resource, which may complicate things. We perform this check defensively for when
            // this situation arises in the future.

            // Step the slice forward to the next event time.
            if (simulationTime.isBefore(event.time)) {
                slice.step(event.time.durationFrom(simulationTime));
                simulationTime = event.time;
            }

            // Apply the event to the slice.
            setter.accept(slice, event.stimulus);
        }

        return resourceClass.cast(getter.apply(slice));
    }

    @Override
    public final Map<Instant, ResourceType> getHistory() {
        final var stateHistory = new TreeMap<Instant, ResourceType>();

        final var slice = this.systemModel.getInitialSlice();
        final var getter = this.registry.getGetter(slice, this.name, resourceClass);

        var simulationTime = this.registry.getStartTime();
        stateHistory.put(simulationTime, getter.apply(slice));
        for (final var event : this.registry.getEventLog(this.systemModel)) {
            final Setter<?> setter = this.registry.getSetter(slice, event.resourceName);
            if (setter == null) continue;

            // Step the slice forward to the next event time.
            if (simulationTime.isBefore(event.time)) {
                slice.step(event.time.durationFrom(simulationTime));
                simulationTime = event.time;
            }

            // Apply the event to the slice.
            setter.accept(slice, event.stimulus);

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