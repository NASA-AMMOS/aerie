package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;


public class AdaptationTest {

    @Test
    public void testRunsim() {
        Runsim.runSimulation( new Config() );
    }

}