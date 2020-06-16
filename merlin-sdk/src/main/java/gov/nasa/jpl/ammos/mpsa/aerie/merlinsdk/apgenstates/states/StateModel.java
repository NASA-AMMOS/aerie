package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.QueryUtilityMethods;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public class StateModel {
    private final Map<String, State> states;
    private final Map<String, Map<Duration, Double>> changes = new HashMap<>();
    private Duration elapsedTime = Duration.ZERO;

    public StateModel() {
        this.states = new HashMap<>();
    }

    public StateModel(final StateModel other) {
        this.states = new HashMap<>(other.states.size());
        for (final var entry : other.states.entrySet()) {
            final var key = Objects.requireNonNull(entry.getKey());
            final var initialValues = Objects.requireNonNull(entry.getValue());

            this.states.put(key, new State(initialValues));
        }
    }

    public void step(final Duration duration) {
        this.elapsedTime = this.elapsedTime.plus(duration);
    }

    public void addState(String name, double initialValue){
        this.states.put(name, new State(name, initialValue));

        //Assumption: all states are initialized at the very start of the simulation
        final Map<Duration, Double> stateChanges = new HashMap<>();
        stateChanges.put(elapsedTime, initialValue);
        this.changes.put(name, stateChanges);
    }

    public void logChangedValue(String name, double newValue){
        this.changes.get(name).put(this.elapsedTime, newValue);
    }

    public State getState(final String name) {
        return this.states.get(name);
    }

    public List<Window> stateThreshold(String name, Predicate<Double> lambda){
        var loggedValues = this.changes.get(name).entrySet();
        return QueryUtilityMethods.stateThreshold(loggedValues, SimulationInstant.ORIGIN.plus(this.elapsedTime), lambda);
    }

    @Override
    public String toString() {
        return this.states.toString();
    }

}
