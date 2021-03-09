package gov.nasa.jpl.aerie.services.adaptation.mocks;

import gov.nasa.jpl.aerie.services.adaptation.remotes.AdaptationRepositoryContractTest;

public final class MockAdaptationRepositoryTest extends AdaptationRepositoryContractTest {
    @Override
    protected void resetRepository() {
        this.adaptationRepository = new MockAdaptationRepository();
    }
}
