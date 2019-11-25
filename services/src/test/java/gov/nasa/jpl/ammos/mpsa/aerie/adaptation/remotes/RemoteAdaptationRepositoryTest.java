package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.remotes;

import java.net.URI;

@org.junit.jupiter.api.Tag("integration")
public final class RemoteAdaptationRepositoryTest extends AdaptationRepositoryContractTest {
    private static final URI MONGO_URI = URI.create("mongodb://localhost:27019");
    private static final String MONGO_DATABASE = "adaptation-service";
    private static final String MONGO_ADAPTATION_COLLECTION = "adaptations";

    private static final RemoteAdaptationRepository remoteRepository = new RemoteAdaptationRepository(MONGO_URI, MONGO_DATABASE, MONGO_ADAPTATION_COLLECTION);

    @Override
    protected void resetRepository() {
        RemoteAdaptationRepositoryTest.remoteRepository.clear();
        this.adaptationRepository = RemoteAdaptationRepositoryTest.remoteRepository;
    }
}
