package gov.nasa.jpl.aerie.services.plan.mocks;

import gov.nasa.jpl.aerie.services.plan.remotes.AdaptationRepositoryContractTest;

public final class MockAdaptationRepositoryTest extends AdaptationRepositoryContractTest {
    @Override
    protected void resetRepository() {
        this.adaptationRepository = new MockAdaptationRepository();
    }
}
