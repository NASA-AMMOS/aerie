package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states;

import java.util.LinkedHashMap;
import java.util.Map;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

// FIXME: integrate with Meghana's state work
public class SettableState<T>{

    private T value;

    private SimulationEngine<?> engine;

    private Map<Time, T> stateHistory = new LinkedHashMap<>();

    public void setValue(T value) {
        this.value = value;
        stateHistory.put(engine.getCurrentSimulationTime(), value);
    }

    public T getValue() {
        return value;
    }

    public void setEngine(SimulationEngine<?> engine) {
        this.engine = engine;
    }

    public Map<Time, T> getHistory() {
        return this.stateHistory;
    }

}