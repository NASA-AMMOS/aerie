package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.mocks.MockState;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

/**
 * exercises the solar panel power model to ensure that it satisfies an inverse square
 * law with respect to solar distance
 *
 * uses a battery of test vectors injected by the junit parameterized runner
 */
@RunWith(Parameterized.class)
public class SolarArrayPowerDinstanceInverseSquareLawTest {

    /**
     * a set of test data points (inputs x expected outputs) used to ensure that the power
     * data model indeed obeys an inverse square law vs solar distance; the junit
     * parameterized runner injects each tuple in turn to the \@Parameter annotated fields
     * below.
     *
     * @return array of ( inputs x expected output ) tuples
     */
    @Parameterized.Parameters
    public static Double[][] data() {
        return new Double[][]{
                // { refPow(W), refDist(m), queryDist(m), expectedPow(W) }
                {360.0, 120.0,     120.0,     360.0}, //d*1 so P*1
                {360.0, 120.0,      60.0,    1440.0}, //d/2 so P*4
                {360.0, 120.0,     240.0,      90.0}, //d*2 so P/4
                {360.0, 120.0,      40.0,    3240.0}, //d/3 so P*9
                {360.0, 120.0,     360.0,      40.0}, //d*3 so P/9
                {160.0,   1.0e+11,   1.0e+11, 160.0}, //d*1 so P*1
                {160.0,   1.0e+11,   4.0e+11,  10.0}, //d*4 so P/16
                {160.0,   1.0e+11,   0.5e+11, 640.0}  //d/2 so P*4
        };
    }

    /**
     * the reference power for the solar array model configuration, generated at the
     * reference distance (populated according to data() via injection by the junit
     * parameterized runner)
     */
    @Parameterized.Parameter(0)
    public double refPower_W = 360.0;

    /**
     * the reference distance for the reference power for the solar array model, in meters
     * (populated according to data() via injection by the junit parameterized runner)
     */
    @Parameterized.Parameter(1)
    public double refDist_m = 120.0;

    /**
     * the single keypoint query distance for the solar array power model, in meters
     * (populated according to data() via injection by the junit parameterized runner)
     */
    @Parameterized.Parameter(2)
    public double queryDist_m = 240.0;

    /**
     * the single expected result value of the solar array model, in Watts (populated
     * according to data() via injection by the junit parameterized runner)
     */
    @Parameterized.Parameter(3)
    public double expectedPower_W = 90.0;

    /**
     * verify that the power model obeys an inverse square law vs solar distance by
     * checking the single configuration x query x expected output tuple injected into
     * this test object by the junit parameterized runner
     */
    @Test
    public void testSingleTuple() {

        //set up a stub state that just returns a constant value
        final RandomAccessState<Double> mockDistanceState_m = new MockState<>( queryDist_m );

        //configure the solar power model using test vector values injected by junit
        final SolarArrayPower testPowerState_W = new SolarArrayPower(
                mockDistanceState_m,
                refPower_W,
                refDist_m
                );

        //run the model using inputs
        final double resultPower_W = testPowerState_W.get();

        //confirm match to expected, withing allowed accuracy (fixed for now)
        assertThat( resultPower_W ).isCloseTo( expectedPower_W, withinPercentage( 0.01 ) );

    }
}
