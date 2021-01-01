package gov.nasa.jpl.ammos.mpsa.aerie.services.adaptation.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.services.adaptation.remotes.AdaptationRepositoryContractTest;

public final class MockAdaptationRepositoryTest extends AdaptationRepositoryContractTest {
    @Override
    protected void resetRepository() {
        this.adaptationRepository = new MockAdaptationRepository();
    }
}
