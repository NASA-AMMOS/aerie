package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class StateModel {
    private final Map<String, State> states;

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
    }

    public void addState(String name, double initialValue){
        this.states.put(name, new State(name, initialValue));
    }

    public State getState(final String name) {
        return this.states.get(name);
    }

    @Override
    public String toString() {
        return this.states.toString();
    }
}
