package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

public abstract class DerivedState<T> implements State<T> {
    
    protected SimulationEngine engine;
    protected Map<Instant, T> stateHistory = new LinkedHashMap<>();

    @Override
    public T get() {
        throw new NotImplementedException("This derived state must provide its own `get()` method");
    }

    @Override
    public Map<Instant, T> getHistory() {
        return stateHistory;
    }

    @Override
    public String getName() {
        // TODO: get the annotation name rather than using reflection here
        return this.getClass().getName();
    }

    public void setEngine(SimulationEngine engine) {
        this.engine = engine;
    }
}