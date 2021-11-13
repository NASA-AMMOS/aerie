package gov.nasa.jpl.aerie.merlin.server.mocks;

import gov.nasa.jpl.aerie.merlin.server.remotes.MissionModelRepositoryContractTest;
import org.junit.jupiter.api.Disabled;

@Disabled
public final class InMemoryMissionModelRepositoryTest extends MissionModelRepositoryContractTest {
    @Override
    protected void resetRepository() {
        this.adaptationRepository = new InMemoryMissionModelRepository();
    }
}
