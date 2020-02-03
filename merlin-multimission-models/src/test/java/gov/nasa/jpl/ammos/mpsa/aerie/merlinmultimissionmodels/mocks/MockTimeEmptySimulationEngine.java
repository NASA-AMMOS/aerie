package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

/**
 * specialization of the mock time simulation engine dedicated to MockEmptyStateContainer
 *
 * useful as shorthand in tests
 */
public class MockTimeEmptySimulationEngine
        extends MockTimeSimulationEngine<MockEmptyStateContainer> {
    /**
     *
     * creates a new mock simulation engine that is non-functional except to provide time
     * stamps to requesting code under test
     *
     * @param mockTime the initial mock simulation time to report to requestors
     */
    public MockTimeEmptySimulationEngine(Instant mockTime) {
        super(mockTime);
    }

}
