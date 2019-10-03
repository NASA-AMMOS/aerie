package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states;

import java.util.LinkedHashMap;
import java.util.Map;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

/**
 * CumulativeStates store numeric values which can be changed by some delta, in
 * addition to being set directly. A set value overwrites the state's prior
 * value. There is no relationship between the new set value and the state's
 * previous value.
 *
 * A CumulativeState's value can also be incremented or decremented by a delta.
 * Due to type erasure, we do not which Number type T is at compile time. Basic
 * math operations (+,-, etc.) cannot be done on Number objects. As a result, in
 * order to implement the increment and decrement methods, inner classes
 * specifically named for the Number object have been created.
 *
 * Example creation of a CumulativeState object: CumulativeState<Integer> state
 * = new CumulativeState.Integer("name", value);
 *
 *
 * @param <T> a datatype of class Number that represents the numeric value of
 *            the state
 */



public interface CumulativeState<T extends Number> extends State<T>  {

   public void increment(T delta);
   public void decrement(T delta);


   public static class Integer implements CumulativeState<java.lang.Integer> {

        private int value;
        private String name;
        private SimulationEngine<?> engine;
        private Map<Time, java.lang.Integer> stateHistory = new LinkedHashMap<>();

        public Integer(String name, int value){
            this.name = name;
            this.value = value;
        }

        public String getName(){
           return this.name;
        }

        public java.lang.Integer getValue(){
            return this.value;
        }

       public void setValue(java.lang.Integer value) {
            this.value = value;
            stateHistory.put(engine.getCurrentSimulationTime(), value);
        }

        @Override
        public void increment(java.lang.Integer delta) {
            this.value += delta;
            stateHistory.put(engine.getCurrentSimulationTime(), value);
        }

        @Override
        public void decrement(java.lang.Integer delta) {
            this.value -= delta;
            stateHistory.put(engine.getCurrentSimulationTime(), value);
        }

        @Override
        public void setEngine(SimulationEngine<?> engine) {
            this.engine = engine;
        }
        
        public Map<Time, java.lang.Integer> getHistory() {
            return this.stateHistory;
        }
    }


    public static class Double implements CumulativeState<java.lang.Double> {

        private double value;
        private String name;
        private SimulationEngine<?> engine;
        private Map<Time, java.lang.Double> stateHistory = new LinkedHashMap<>();

        public Double(String name, double value){
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public java.lang.Double getValue() {
            return this.value;
        }

        public void setValue(java.lang.Double value) {
            this.value = value;
            stateHistory.put(this.engine.getCurrentSimulationTime(), this.value);
        }

        @Override
        public void increment(java.lang.Double delta) {
            this.value += delta;
            stateHistory.put(this.engine.getCurrentSimulationTime(), this.value);
        }

        @Override
        public void decrement(java.lang.Double delta) {
            this.value -= delta;
            stateHistory.put(this.engine.getCurrentSimulationTime(), this.value);
        }

        public void setEngine(SimulationEngine<?> engine) {
            this.engine = engine;
        }
    
        public Map<Time, java.lang.Double> getHistory() {
            return this.stateHistory;
        }
    }


}