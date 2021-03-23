package gov.nasa.jpl.aerie.merlin.server.mocks;

import gov.nasa.jpl.aerie.merlin.server.remotes.AdaptationRepositoryContractTest;

public final class MockAdaptationRepositoryTest extends AdaptationRepositoryContractTest {
    @Override
    protected void resetRepository() {
        this.adaptationRepository = new MockAdaptationRepository();
    }
}
