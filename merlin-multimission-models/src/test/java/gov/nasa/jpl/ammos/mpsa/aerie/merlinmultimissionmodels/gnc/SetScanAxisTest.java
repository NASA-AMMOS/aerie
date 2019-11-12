package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.gnc;


import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.gnc.activities.SetScanAxis;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityJob;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import static org.junit.Assert.assertTrue;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;


public class SetScanAxisTest {

    @Test
    public void setScanAxis(){
        double x = 0.2;
        double y = 0.2;
        double z = 0.2;

        Time simStart = new Time();
        SetScanAxis setScanAxisActivity = new SetScanAxis(x, y, z);
        ActivityJob<GNCStates> gncActivities = new ActivityJob<>(setScanAxisActivity, simStart);
        GNCStates gncStates = new GNCStates();

        SimulationEngine engine = new SimulationEngine(simStart, List.of(gncActivities), gncStates);
        engine.simulate();

        Map<Time, Vector3D> history = gncStates.getVectorState(GNCStates.scanAxisName).getHistory();

        Vector3D vector = gncStates.getVectorState(GNCStates.scanAxisName).get();
        Vector3D temp = new Vector3D(x, y, z);
        temp.normalize();
        System.out.println(temp.getX());

        assertTrue(vector.getX() == temp.getX());
        assertTrue(vector.getY() == temp.getY());
        assertTrue(vector.getZ() == temp.getZ());
    }

}
