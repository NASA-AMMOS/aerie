package gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes;

import java.net.URI;

@org.junit.jupiter.api.Tag("integration")
public class RemotePlanRepositoryTest extends PlanRepositoryContractTest {
  private static final URI MONGO_URI = URI.create("mongodb://localhost:27017");
  private static final String MONGO_DATABASE = "plan-service";
  private static final String MONGO_PLAN_COLLECTION = "plans";
  private static final String MONGO_ACTIVITY_COLLECTION = "activities";

  private static final RemotePlanRepository remoteRepository = new RemotePlanRepository(MONGO_URI, MONGO_DATABASE, MONGO_PLAN_COLLECTION, MONGO_ACTIVITY_COLLECTION);

  @Override
  protected void resetRepository() {
    RemotePlanRepositoryTest.remoteRepository.clear();
    this.planRepository = RemotePlanRepositoryTest.remoteRepository;
  }
}
