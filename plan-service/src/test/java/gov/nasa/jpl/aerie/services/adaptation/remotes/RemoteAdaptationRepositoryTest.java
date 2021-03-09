package gov.nasa.jpl.aerie.services.adaptation.remotes;

import com.mongodb.client.MongoClients;
import org.junit.Ignore;
import org.junit.jupiter.api.Tag;

import java.net.URI;

@Ignore
@Tag("integration")
public final class RemoteAdaptationRepositoryTest extends AdaptationRepositoryContractTest {
    private static final URI MONGO_URI = URI.create("mongodb://localhost:27019");
    private static final String MONGO_DATABASE = "adaptation-service";
    private static final String MONGO_ADAPTATION_COLLECTION = "adaptations";

    private static final RemoteAdaptationRepository remoteRepository = new RemoteAdaptationRepository(
        MongoClients
            .create(MONGO_URI.toString())
            .getDatabase(MONGO_DATABASE),
        MONGO_ADAPTATION_COLLECTION);

    @Override
    protected void resetRepository() {
        RemoteAdaptationRepositoryTest.remoteRepository.clear();
        this.adaptationRepository = RemoteAdaptationRepositoryTest.remoteRepository;
    }
}
