package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class CumulativeStatesTest {


    public String name1 = "state 1";
    public String name2 = "state 2";

    public CumulativeState<Integer> state1 = new CumulativeState.Integer(name1, 0);
    public CumulativeState<Double> state2 = new CumulativeState.Double(name2, 0.0);

    public int value1 = 10;
    public double value2 = 15.5;

    public class MockStateContainer implements StateContainer {
        public List<State<?>> getStateList() {
            return List.of();
        }
    }
    public SimulationEngine<?> mockEngine = new SimulationEngine<StateContainer>(new Time(), List.of(), new MockStateContainer());


    @Before
    public void setup(){
        state1.setEngine(mockEngine);
        state2.setEngine(mockEngine);
        assert(state1.getName() == name1);
        assert(state2.getName() == name2);
        assert(state1.getValue() == 0);
        assert(state2.getValue() == 0.0);
        state1.setValue(value1);
        state2.setValue(value2);
    }

    @After
    public void teardown(){
        state1 = null;
        state2 = null;
    }

    @Test
    public void getValue(){
        assert(state1.getValue() == value1);
        assert(state2.getValue() == value2);
    }

    @Test
    public void increment(){
        int inc1 = 10;
        double inc2 = 10.0;
        state1.increment(inc1);
        state2.increment(inc2);
        assert(state1.getValue() == value1 + inc1);
        assert(state2.getValue() == value2 + inc2);
    }

    @Test
    public void decrement(){
        int dec1 = 13;
        double dec2 = -34.2;
        state1.decrement(dec1);
        state2.decrement(dec2);
        assert(state1.getValue() == value1 - dec1);
        assert(state2.getValue() == value2 - dec2);
    }








}
