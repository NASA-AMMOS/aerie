package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.remotes.AdaptationRepositoryContractTest;

public final class MockAdaptationRepositoryTest extends AdaptationRepositoryContractTest {
    @Override
    protected void resetRepository() {
        this.adaptationRepository = new MockAdaptationRepository();
    }
}
