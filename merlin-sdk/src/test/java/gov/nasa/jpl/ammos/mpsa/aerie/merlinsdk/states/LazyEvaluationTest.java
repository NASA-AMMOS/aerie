package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class LazyEvaluationTest {

    public class ExampleState implements DerivedState<Double>{

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
        public Double getValue(){
            return evaluator.get();
        }

        @Override
        public void setValue(Double value) {
            this.value = value;
            evaluator.invalidate();
        }

        @Override
        public String getName(){
            return "None";
        }

        @Override
        public void setEngine(SimulationEngine<?> engine) {
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

        state.getValue();
        assert("Recalculating".equals(outContent.toString()));
        outContent.reset();

        state.getValue();
        assert("".equals(outContent.toString()));
        outContent.reset();

        state.getValue();
        assert("".equals(outContent.toString()));
        outContent.reset();

        state.getValue();
        assert("".equals(outContent.toString()));
        outContent.reset();

        state.setValue(1123.3);
        state.getValue();
        assert("Recalculating".equals(outContent.toString()));
        outContent.reset();
    }
}
