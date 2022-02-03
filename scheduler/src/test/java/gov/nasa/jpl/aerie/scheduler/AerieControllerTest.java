package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class AerieControllerTest {
  private final PlanningHorizon horizon = new PlanningHorizon(new Time(0), new Time(100000));
  private final MissionModelWrapper missionModel = MerlinSightTestUtility.getMerlinSightMissionModel(horizon);
  private final String URL_AERIE = MerlinSightTestUtility.LOCAL_AERIE;
  private final int MISSION_MODEL_ID = MerlinSightTestUtility.latest;

  /**
   * This test is here as a demonstration of how the AerieController can be used
   * Disabled because you need a local aerie
   */
  @Disabled
  @Test
  public void test(){
    //Create an empty plan locally and distantly
    var horizon = new PlanningHorizon(new Time(0), new Time(10000));
    AerieController controller = new AerieController(URL_AERIE,
                                                     MISSION_MODEL_ID,
                                                     false,
                                                     horizon,
                                                     missionModel);
    Plan localPlan = new PlanInMemory(missionModel);
    controller.initEmptyPlan(localPlan, horizon.getStartAerie(), horizon.getEndAerie(),null);
    //necessary step to be able to visualize the plan
    controller.createSimulation(localPlan);
    //Create an activity instance and add it to the distant plan.
    // Note that it is not added to the local one, scheduler must ensure the consistency between local and distant plan.
    var actType = new ActivityType("HP3TemP");
    //adding the activity type to the mission model wrapper is necessary.
    //Activity must have the same name as in the mission model
    missionModel.add(actType);
    var actinstance = new ActivityInstance(actType , Duration.of(1, Duration.MINUTE),
                                           Duration.of(1,  Duration.MINUTE));
    actinstance.addParameter("setNewSSATime", SerializedValue.of(true));
    controller.sendActivityInstance(localPlan, actinstance);

    Plan fetchedPlan = controller.fetchPlan(controller.getPlanId(localPlan));
    MerlinSightTestUtility.printPlan(fetchedPlan);
    assert(fetchedPlan.getActivitiesByTime().get(0).equalsInProperties(actinstance));
  }
}
