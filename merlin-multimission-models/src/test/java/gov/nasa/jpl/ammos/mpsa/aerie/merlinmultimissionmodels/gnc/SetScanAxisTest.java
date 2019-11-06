package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.gnc;


import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.gnc.activities.SetScanAxis;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityJob;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import org.junit.Test;

import java.util.List;

public class SetScanAxisTest {

    @Test
    public void setScanAxis(){
        double x = 12.2;
        double y = 0.0;
        double z = 0.0;

        Time simStart = new Time();
        SetScanAxis setScanAxisActivity = new SetScanAxis(x,y,z);
        ActivityJob<GNCStates> gncActivities = new ActivityJob<>(setScanAxisActivity, simStart);
        GNCStates gncStates = new GNCStates();

        SimulationEngine<GNCStates> engine = new SimulationEngine<>(simStart, List.of(gncActivities), gncStates);
        engine.simulate();


    }

}
