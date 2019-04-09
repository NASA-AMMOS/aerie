package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.builders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.mocks.FruitModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Parameter;
import java.util.List;
import java.util.UUID;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import org.junit.Test;

public class ActivityTypeBuilderTests {

  @Test
  public void testActivityTypeBuilderCanSetName() {
    String name = "BiteBanana";
    ActivityTypeBuilder builder = new ActivityTypeBuilder().withName(name);
    ActivityType activityType = builder.getActivityType();
    assertSame(activityType.getName(), name);
  }

  @Test
  public void testActivityTypeBuilderCanSetId() {
    String id = UUID.randomUUID().toString();
    ActivityTypeBuilder builder = new ActivityTypeBuilder().withId(id);
    ActivityType activityType = builder.getActivityType();
    assertEquals(activityType.getId().toString(), id);
  }

  @Test
  public void testActivityTypeBuilderCanSetModel() {
    FruitModel model = new FruitModel();
    ActivityTypeBuilder builder = new ActivityTypeBuilder().withModel(model);
    ActivityType activityType = builder.getActivityType();
    assertSame(activityType.getModel(), model);
  }

  @Test
  public void testActivityTypeBuilderCanAddARelationship() {
    ActivityTypeBuilder builder1 = new ActivityTypeBuilder().withName("peel");
    ActivityTypeBuilder builder2 = new ActivityTypeBuilder().addRelationship(builder1);
    List<ActivityType> activityTypes = builder2.getRelationships();
    assertEquals(activityTypes.size(), 1);
    assertEquals(activityTypes.get(0).getName(), "peel");
  }

  @Test
  public void testActivityTypeBuilderCanAddAParameter() {
    ActivityTypeBuilder activityTypeBuilder = new ActivityTypeBuilder();
    activityTypeBuilder.createParameter().withName("size");
    List<Parameter> parameters = activityTypeBuilder.getParameters();
    assertEquals(parameters.size(), 1);
    assertEquals(parameters.get(0).getName(), "size");
  }
}
