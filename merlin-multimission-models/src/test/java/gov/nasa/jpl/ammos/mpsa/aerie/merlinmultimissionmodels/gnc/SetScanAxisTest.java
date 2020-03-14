package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.gnc;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.gnc.activities.SetScanAxis;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects.spawn;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

public class SetScanAxisTest {
    @Test
    public void setScanAxis(){
        double x = 0.2;
        double y = 0.2;
        double z = 0.2;

        final var expected = new Vector3D(x, y, z);
        expected.normalize();

        final var simStart = SimulationInstant.ORIGIN;
        final var gncStates = new GNCStates();

        SimulationEngine.simulate(simStart, gncStates, () -> {
            spawn(new SetScanAxis(x, y, z)).await();

            final var vector = gncStates.getVectorState(GNCStates.scanAxisName).get();

            assertThat(vector.getX()).isEqualTo(expected.getX());
            assertThat(vector.getY()).isEqualTo(expected.getY());
            assertThat(vector.getZ()).isEqualTo(expected.getZ());
        });
    }
}
