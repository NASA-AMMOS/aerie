package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Extends the {@link State} interface
 *
 * CumulativeStates store numeric values which can be changed by some delta, in
 * addition to being set directly.
 *
 * A CumulativeState's value can also be incremented or decremented by a delta.
 * Due to type erasure, we do not which Number type T is at compile time. Basic
 * math operations (+,-, etc.) cannot be done on Number objects. As a result, in
 * order to implement the increment and decrement methods, inner classes
 * specifically named for the Number object have been created.  The remaining Number types are Float, Long, Short,
 * and Byte, which will be implemented once these types have been merged and it is determined
 * collectively this approach is the best way forward
 *
 * If a set and one or more increments or decrements occur at the same time, the state has undefined behavior.  In this case,
 * it needs to be determined if an error/violation should be logged or if the "set" will be the dominant operation on the state.
 *
 * If a get and one or more increments or decrements occur at the same time, the state has undefined behavior and an
 * error/violation should be logged.  (Unless it is later determined that a get will retrieve the value in that instant
 * before any other changes have been made to it).
 *
 * If multiple increments or decrements occur at the same instant, they "stack" on top of one another.
 *
 * Example creation of a CumulativeState object: CumulativeState<Integer> state
 * = new CumulativeState.Integer("name", value);
 *
 * A CumulativeState is not a SettableState.  If an adapter wants to have a state that is settable, incrementable, and decrementable,
 * they need to implement both the SettableState and CumulativeState interface.
 *
 *
 * Future: Have to think about incon, consider other interfaces necessary to give all the answers other clients might be asking
 *
 *
 * @param <T> a datatype of class Number that represents the numeric value of
 *            the state
 */



public interface CumulativeState<T extends Number> extends State<T> {

    /**
     * Increments the state value by delta
     * @param delta
     */
    public void increment(T delta);

    /**
     * decrements the state value by delta
     * @param delta
     */
    public void decrement(T delta);


    public static class Integer implements CumulativeState<java.lang.Integer> {

        private int value;
        private String name;
        private SimulationEngine engine;
        private Map<Time, java.lang.Integer> stateHistory = new LinkedHashMap<>();

        /**
         * Only public constructor
         * Adapter must specify the String name and Integer amount that the state has
         * This prevents a getValue() from being done on a state with an un-initialized value
         * @param name
         * @param value
         */
        public Integer(String name, int value){
            this.name = name;
            this.value = value;
        }

        /**
         * @return Integer value
         */
        @Override
        public java.lang.Integer get(){
            return this.value;
        }

        /**
         * @return String name
         */
        @Override
        public String getName() { return this.name; }

        /**
         * increments state value by a Integer delta
         * @param delta
         */
        @Override
        public void increment(java.lang.Integer delta) {
            value = value + delta;
        }

        /**
         * decrements state value by Integer delta
         * @param delta
         */
        @Override
        public void decrement(java.lang.Integer delta) {
            value = value - delta;
        }

        //this is a temporary method in order to integrate w/ the current SimulationEngine
        @Override
        public void setEngine(SimulationEngine engine) {
            this.engine = engine;
        }

        //this is a temporary method in order to integrate w/ the current SimulationEngine
        @Override
        public Map<Time, java.lang.Integer> getHistory() {
            return stateHistory;
        }
    }


    public static class Double implements CumulativeState<java.lang.Double> {

        private double value;
        private String name;
        //will need to be set when using syntactic sugar or derived states
        private SimulationEngine engine;
        private Map<Time, java.lang.Double> stateHistory = new LinkedHashMap<>();

        /**
         * Only public constructor
         * Adapter must specify the String name and Double amount that the state has
         * This prevents a getValue() from being done on a state with an un-initialized value
         * @param name
         * @param value
         */
        public Double(String name, double value){
            this.name = name;
            this.value = value;
        }

        /**
         * @return Double value
         */
        @Override
        public java.lang.Double get() {
            return this.value;
        }

        /**
         * @return String name
         */
        @Override
        public String getName() { return this.name; }

        /**
         * increments state value by a Double delta
         * @param delta
         */
        @Override
        public void increment(java.lang.Double delta) {
            value = value + delta;
        }

        /**
         * decrements state value by a Double delta
         * @param delta
         */
        @Override
        public void decrement(java.lang.Double delta) {
            value = value - delta;
        }

        //this is a temporary method in order to integrate w/ the current SimulationEngine
        @Override
        public void setEngine(SimulationEngine engine) {
            this.engine = engine;
        }

        //this is a temporary method in order to integrate w/ the current SimulationEngine
        @Override
        public Map<Time, java.lang.Double> getHistory() {
            return stateHistory;
        }

    }
}
