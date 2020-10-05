package gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchPlanException;
import org.bson.types.ObjectId;
import org.junit.Ignore;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@Ignore
@Tag("integration")
public final class RemotePlanRepositoryTest extends PlanRepositoryContractTest {
  private static final URI MONGO_URI = URI.create("mongodb://localhost:27017");
  private static final String MONGO_DATABASE = "plan-service";
  private static final String MONGO_PLAN_COLLECTION = "plans";
  private static final String MONGO_ACTIVITY_COLLECTION = "activities";

  private static final MongoDatabase mongoDatabase =
      MongoClients.create(MONGO_URI.toString()).getDatabase(MONGO_DATABASE);
  private static final RemotePlanRepository remoteRepository =
      new RemotePlanRepository(mongoDatabase, MONGO_PLAN_COLLECTION, MONGO_ACTIVITY_COLLECTION);

  @Override
  protected void resetRepository() {
    RemotePlanRepositoryTest.remoteRepository.clear();
    this.planRepository = RemotePlanRepositoryTest.remoteRepository;
  }

  @Test
  public void invalidKeyShouldThrow() {
    // GIVEN
    final String invalidObjectId = "abc";
    assertThat(ObjectId.isValid(invalidObjectId)).isFalse();

    // WHEN
    final Throwable thrown = catchThrowable(() -> this.planRepository.getPlan(invalidObjectId));

    // THEN
    assertThat(thrown).isInstanceOf(NoSuchPlanException.class);
    assertThat(((NoSuchPlanException)thrown).getInvalidPlanId()).isEqualTo(invalidObjectId);
  }
}
