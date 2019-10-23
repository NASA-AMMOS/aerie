package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;

public class LazyEvaluationTest {

    public class ExampleState implements State<Double> {

        SimulationEngine<?> mockEngine = new SimulationEngine<>();
        double value;

        public ExampleState(double value){
            this.value = value;
        }

        //TODO: converting an equation to a Number object is proving to result in Type errors
        private final LazyEvaluator<Double> evaluator = LazyEvaluator.of(() -> {
            System.out.print("Recalculating");
            return value;
        });

        @Override
        public Double get(){
            return evaluator.get();
        }

        public void set(Double value) {
            this.value = value;
            evaluator.invalidate();
        }

        //merely implemented to satisfy interface
        @Override
        public void setEngine(SimulationEngine<?> engine) {
        }

        //merely implemented to satisfy interface
        @Override
        public Map<Time, Double> getHistory() {
            return null;
        }
    }

    public double var = 12.2;
    public ExampleState state = new ExampleState(var);

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    public void test(){

        state.get();
        assert("Recalculating".equals(outContent.toString()));
        outContent.reset();

        state.get();
        assert("".equals(outContent.toString()));
        outContent.reset();

        state.get();
        assert("".equals(outContent.toString()));
        outContent.reset();

        state.get();
        assert("".equals(outContent.toString()));
        outContent.reset();

        state.set(1123.3);
        state.get();
        
        assert("Recalculating".equals(outContent.toString()));
        outContent.reset();
    }
}
