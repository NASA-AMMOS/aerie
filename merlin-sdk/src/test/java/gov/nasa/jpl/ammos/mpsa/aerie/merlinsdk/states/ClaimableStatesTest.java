package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class ClaimableStatesTest {


    public String name1 = "State 1";
    public String name2 = "State 2";
    public ClaimableState<Integer> state1 = new ClaimableState.Integer(name1, 0);
    public ClaimableState<Double> state2 = new ClaimableState.Double(name2, 0);
    public int value1 = 12;
    public double value2 = 5.5;

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
        state1.setValue(value1);
        state2.setValue(value2);
    }

    @After
    public void teardown(){
        state1 = null;
        state2 = null;
    }

    @Test
    public void getName(){
        assert(state1.getName() == name1);
        assert(state2.getName() == name2);
    }

    @Test
    public void getValue(){
        assert(state1.getValue() == value1);
        assert (state2.getValue() == value2);
    }

    @Test
    public void setAndGetAvailableAmount(){
        int newValue1 = 100;
        double newValue2 = 99.99;
        state1.setAvailableAmount(newValue1);
        state2.setAvailableAmount(newValue2);
        assert(state1.getValue() == newValue1);
        assert(state1.getAvailableAmount() == newValue1);
        assert(state2.getValue() == newValue2);
        assert(state2.getAvailableAmount() == newValue2);
    }

    @Test
    public void claimPart(){
        int request1 = 10;
        double request2 = 1.1;
        state1.claim(request1);
        state2.claim(request2);
        assert(state1.getAvailableAmount() == (value1 - request1));
        assert(state2.getAvailableAmount() == (value2 - request2));
    }

    @Test
    public void claimPart2(){
        int request1 = value1 + 1;
        double request2 = value2 + 1;
        state1.claim(request1);
        state2.claim(request2);
        assert(state1.getAvailableAmount() == 0);
        assert(state2.getAvailableAmount() == 0);
    }

    @Test
    public void claimAll(){
        state1.claimAllAvailable();
        state2.claimAllAvailable();
        assert(state1.getAvailableAmount() == 0);
        assert(state2.getAvailableAmount() == 0);
    }

    @Test
    public void release(){
        int x = 3;
        double y = 9;
        state1.release(x);
        state2.release(y);
        assert(state1.getAvailableAmount() == x + value1);
        assert(state2.getAvailableAmount() == y + value2);
    }
}
