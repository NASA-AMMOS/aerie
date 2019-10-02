package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states;

import java.util.LinkedHashMap;
import java.util.Map;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

/**
 * SettableStates can store values that are any type. An adapter can get the
 * value stored, or can set it directly. A set value overwrites the state's
 * prior value. There is no relationship between the new set value and the
 * state's previous value.
 *
 * A CumulativeState's value can also be incremented or decremented by a delta.
 * Due to type erasure, we do not which Number type T is at compile time. Basic
 * math operations (+,-, etc.) cannot be done on Number objects. As a result, in
 * order to implement the increment and decrement methods, inner classes
 * specifically named for the Number object have been created.
 *
 * Example creation of a SettableState object: SettableState<Integer> state =
 * new SettableState<>("my name", value);
 *
 *
 * @param <T> a datatype of class Number that represents the numeric value of
 *            the state
 */


public class SettableState<T> implements State<T>{

    private T value;
    private String name;
    private SimulationEngine<?> engine;
    private Map<Time, T> stateHistory = new LinkedHashMap<>();

    //force user to initialize
    public SettableState(String name, T value){
        this.name = name;
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
        stateHistory.put(this.engine.getCurrentSimulationTime(), this.value);
    }

    public String getName() {
        return name;
    }

    public void setEngine(SimulationEngine<?> engine) {
        this.engine = engine;
    }

    public Map<Time, T> getHistory() {
        return this.stateHistory;
    }

}
