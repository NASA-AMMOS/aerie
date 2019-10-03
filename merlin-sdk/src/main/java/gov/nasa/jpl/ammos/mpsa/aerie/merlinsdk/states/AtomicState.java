package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states;

import java.util.LinkedHashMap;
import java.util.Map;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

//cant implement claim because you have a parameter as an input
//shouldnt this be a type of boolean state?
//set your standard for available or not
public class AtomicState implements State<Boolean> {

    private boolean isAvailable;
    private String name;
    private SimulationEngine<?> engine;
    private Map<Time, Boolean> stateHistory = new LinkedHashMap<>();

    public AtomicState(String name, Boolean isAvailable){
        this.isAvailable= isAvailable;
        this.name = name;
    }

    public Boolean getValue() {
        return this.isAvailable;
    }

    public void setValue(Boolean value) {
        this.isAvailable = value;
        stateHistory.put(this.engine.getCurrentSimulationTime(), this.isAvailable);
    }

    public boolean isAvailable(){
        return getValue();
    }

    public String getName() {
        return this.name;
    }

    public void claim(){
        this.isAvailable = false;
        stateHistory.put(this.engine.getCurrentSimulationTime(), this.isAvailable);
    }

    public void release(){
        this.isAvailable = true;
        stateHistory.put(this.engine.getCurrentSimulationTime(), this.isAvailable);
    }

    @Override
    public void setEngine(SimulationEngine<?> engine) {
        this.engine = engine;
    }

    public Map<Time, Boolean> getHistory() {
        return this.stateHistory;
    }
}

