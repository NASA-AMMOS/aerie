package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.protocol.model.SchedulerModel;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class AerieControllerTest {
  private final PlanningHorizon horizon = new PlanningHorizon(new Time(0), new Time(100000));
  private final MissionModel<?> missionModel = MerlinSightTestUtility.getMerlinSightMissionModel();
  private final SchedulerModel schedulerModel = MerlinSightTestUtility.getMerlinSightSchedulerModel();
  private final String URL_AERIE = MerlinSightTestUtility.LOCAL_AERIE;
  private final int MISSION_MODEL_ID = MerlinSightTestUtility.latest;
  private final Problem problem = new Problem(missionModel, horizon, schedulerModel);
  /**
   * This test is here as a demonstration of how the AerieController can be used
   * Disabled because you need a local aerie
   */
  @Disabled
  @Test
  public void test(){
    //Create an empty plan locally and distantly
    var horizon = new PlanningHorizon(new Time(0), new Time(10000));
    AerieController controller = new AerieController(MerlinSightTestUtility.LOCAL_AERIE,
                                                     MerlinSightTestUtility.MISSION_MODEL_ID,
                                                     false,
                                                     horizon,
                                                     problem.getActivityTypes());
    Plan localPlan = new PlanInMemory();
    controller.initEmptyPlan(localPlan, horizon.getStartAerie(), horizon.getEndAerie(),null);
    //necessary step to be able to visualize the plan
    controller.createSimulation(localPlan);
    //Create an activity instance and add it to the distant plan.
    // Note that it is not added to the local one, scheduler must ensure the consistency between local and distant plan.
    var actinstance = new ActivityInstance(problem.getActivityType("HP3TemP") , Duration.of(1, Duration.MINUTE),
                                           Duration.of(1,  Duration.MINUTE));
    actinstance.addArgument("setNewSSATime", SerializedValue.of(true));
    controller.sendActivityInstance(localPlan, actinstance);

    Plan fetchedPlan = controller.fetchPlan(controller.getPlanId(localPlan));
    MerlinSightTestUtility.printPlan(fetchedPlan);
    assert(fetchedPlan.getActivitiesByTime().get(0).equalsInProperties(actinstance));
  }
}
