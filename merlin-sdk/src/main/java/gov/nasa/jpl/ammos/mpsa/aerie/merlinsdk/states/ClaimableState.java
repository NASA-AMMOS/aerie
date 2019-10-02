package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states;

import java.util.LinkedHashMap;
import java.util.Map;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

/**
 *
 * ClaimableStates store numeric values that can be "claimed" and then returned.
 * For example, onboard memory can be represented as a ClaimableState.
 *
 * When adapters create a ClaimableState, they initialize the amount that is
 * available to claim. We do provide a way to set the value directly. However,
 * this feature may be removed. If so, then the State interface can no longer
 * have a set method, or ClaimableStates will have to extend another interface.
 *
 * If an Activity tries to claim more than is available from the claimable
 * state, the value of the state sets to 0.
 *
 * Example creation of a ClaimableState object: ClaimableState<Integer> state =
 * new Claimable.Integer("name", value);
 *
 *
 * @param <T> a datatype of class Number that represents the numeric value of
 *            the state
 */


public interface ClaimableState<T extends Number> extends State<T> {

    public void claim(T amount);

    public void release(T amount);

    public void claimAllAvailable();

    public void setAvailableAmount(T amount);

    public T getAvailableAmount();

    //add description category
    public static class Integer implements ClaimableState<java.lang.Integer> {

        private int availableAmount;
        private String name;
        private SimulationEngine<?> engine;
        private Map<Time, java.lang.Integer> stateHistory = new LinkedHashMap<>();

        public Integer(String name, int availableAmount){
            this.name = name;
            this.availableAmount = availableAmount;
        }

        public String getName() {
            return this.name;
        }

        public java.lang.Integer getValue(){
            return this.availableAmount;
        }

        public void setValue(java.lang.Integer value) {
            this.availableAmount = value;
            stateHistory.put(this.engine.getCurrentSimulationTime(), this.availableAmount);
        }

        @Override
        public void setAvailableAmount(java.lang.Integer availableAmount){
            setValue(availableAmount);
        }

        @Override
        public java.lang.Integer getAvailableAmount(){
            return getValue();
        }

        //Q: how do we notify the user of this?  should we log an attempted illegal behaviour?
        //probably should have a max and a min
        //attempt max claim
        //constraint violation
        @Override
        public void claim(java.lang.Integer amount) {
            if (amount > availableAmount){
                availableAmount = 0;
            }
            else {
                availableAmount -= amount;
            }
            stateHistory.put(this.engine.getCurrentSimulationTime(), this.availableAmount);
        }

        @Override
        public void release(java.lang.Integer amount) {
            availableAmount += amount;
            stateHistory.put(this.engine.getCurrentSimulationTime(), this.availableAmount);
        }

        @Override
        public void claimAllAvailable() {
            availableAmount = 0;
            stateHistory.put(this.engine.getCurrentSimulationTime(), this.availableAmount);
        }

        @Override
        public void setEngine(SimulationEngine<?> engine) {
            this.engine = engine;
        }
    
        public Map<Time, java.lang.Integer> getHistory() {
            return this.stateHistory;
        }
    }

    public static class Double implements ClaimableState<java.lang.Double> {

        private double availableAmount;
        private String name;
        private SimulationEngine<?> engine;
        private Map<Time, java.lang.Double> stateHistory = new LinkedHashMap<>();

        public Double(String name, double availableAmount){
            this.name = name;
            this.availableAmount = availableAmount;
        }

        public String getName() {
            return this.name;
        }

        public java.lang.Double getValue(){
            return this.availableAmount;
        }

        public void setValue(java.lang.Double value) {
            this.availableAmount = value;
            stateHistory.put(this.engine.getCurrentSimulationTime(), this.availableAmount);
        }

        @Override
        public void setAvailableAmount(java.lang.Double availableAmount){
            setValue(availableAmount);
        }

        @Override
        public java.lang.Double getAvailableAmount(){
            return getValue();
        }

        //Q: how do we notify the user of this?  should we log an attempted illegal behaviour?
        //probably should have a max and a min
        @Override
        public void claim(java.lang.Double amount) {
            if (amount > availableAmount){
                availableAmount = 0;
            }
            else {
                availableAmount -= amount;
            }
            stateHistory.put(this.engine.getCurrentSimulationTime(), this.availableAmount);
        }

        @Override
        public void release(java.lang.Double amount) {
            availableAmount += amount;
            stateHistory.put(this.engine.getCurrentSimulationTime(), this.availableAmount);
        }

        @Override
        public void claimAllAvailable() {
            availableAmount = 0;
            stateHistory.put(this.engine.getCurrentSimulationTime(), this.availableAmount);
        }

        @Override
        public void setEngine(SimulationEngine<?> engine) {
            this.engine = engine;
        }
    
        public Map<Time, java.lang.Double> getHistory() {
            return this.stateHistory;
        }
    
    }



}
