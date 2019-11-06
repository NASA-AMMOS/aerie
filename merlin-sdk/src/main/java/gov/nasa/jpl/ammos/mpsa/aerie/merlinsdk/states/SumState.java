package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface SumState<T extends Number> extends State<T> {

    public T sum();

    public static class Integer implements SumState<java.lang.Integer>{

        private List<State<java.lang.Integer>> states = new ArrayList<>();
        private String name;
        private SimulationEngine<?> engine;
        private Map<Time, java.lang.Integer> stateHistory = new LinkedHashMap<>();

        //currently nothing is being invalidated so this doesn't make sense
        //keeping this code here as a reminder for now, can remove for PR
        //TODO: add a way for states that are in this sum to invalidate the value
        private final LazyEvaluator<java.lang.Integer> evaluator = LazyEvaluator.of(() -> {
            return sum();
        });

        public Integer(String name, State<java.lang.Integer>... states){
            this.name = name;
            for (State<java.lang.Integer> x : states){
                this.states.add(x);
            }
        }

        /**
         * @return String name
         */
        @Override
        public String getName() { return this.name; }

        @Override
        public java.lang.Integer sum(){
            int sum = 0;
            for (State<java.lang.Integer> x : this.states){
                sum +=  x.get().intValue();
            }
            return sum;
        }

        @Override
        public java.lang.Integer get() {
            return this.evaluator.get();
        }

        @Override
        public void setEngine(SimulationEngine engine) {
            this.engine = engine;
        }

        @Override public Map<Time, java.lang.Integer> getHistory(){
            return this.stateHistory;
        }
    }

    public static class Double implements SumState<java.lang.Double>{

        private List<State<java.lang.Double>> states = new ArrayList<>();
        private String name;
        private SimulationEngine<?> engine;
        private Map<Time, java.lang.Double> stateHistory = new LinkedHashMap<>();

        public Double(String name, State<java.lang.Double>... states){
            this.name = name;
            for (State<java.lang.Double> x : states){
                this.states.add(x);
            }
        }

        @Override
        public java.lang.Double sum(){
            double sum = 0;
            for (State<java.lang.Double> x : this.states){
                sum +=  x.get().doubleValue();
            }
            return sum;
        }

        @Override
        public java.lang.Double get() {
            return sum();
        }

        /**
         * @return String name
         */
        @Override
        public String getName() { return this.name; }

        @Override
        public void setEngine(SimulationEngine engine) {
            this.engine = engine;
        }

        @Override public Map<Time, java.lang.Double> getHistory(){
            return this.stateHistory;
        }
    }
}
