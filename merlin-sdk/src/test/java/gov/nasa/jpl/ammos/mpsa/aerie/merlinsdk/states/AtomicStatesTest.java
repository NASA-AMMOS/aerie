package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class AtomicStatesTest {


    public boolean initValue = false;
    public String name = "NAC Available for Observation";
    AtomicState NAC = new AtomicState("NAC Available for Observation", false);

    public class MockStateContainer implements StateContainer {
        public List<State<?>> getStateList() {
            return List.of();
        }
    }
    public SimulationEngine<?> mockEngine = new SimulationEngine<StateContainer>(new Time(), List.of(), new MockStateContainer());

    @Before
    public void setup(){
        NAC.setEngine(mockEngine);
        NAC.setValue(initValue);
    }

    @After
    public void teardown(){
        NAC = null;
    }

    @Test
    public void getNameValue(){
        assert(NAC.getValue() == initValue);
        assert(NAC.getName() == name);
    }

    @Test
    public void claim(){
        NAC.claim();
        assert(NAC.getValue() == false);
        assert(NAC.isAvailable() == false);
    }

    @Test
    public void release(){
        NAC.release();
        assert(NAC.getValue() == true);
        assert(NAC.isAvailable() == true);
    }


}
