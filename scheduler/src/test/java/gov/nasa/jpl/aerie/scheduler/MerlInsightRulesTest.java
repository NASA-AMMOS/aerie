package gov.nasa.jpl.aerie.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class MerlInsightRulesTest {

  @BeforeEach
  void setUp(){
    planningHorizon = new PlanningHorizon(new Time(0), new Time(10000));
    rules = new MerlInsightRules(new MissionModelWrapper(TestUtility.getMerlinSightMissionModel(), planningHorizon));
    missionModelWrapper = rules.getMissionModel();
    missionModelWrapper.getSimulationFacade().simulatePlan(makeEmptyPlan());
    controller = new AerieController(TestUtility.LOCAL_AERIE, TestUtility.latest, false, planningHorizon, missionModelWrapper);
    smallProblem = new Problem(missionModelWrapper);
  }

  private PlanningHorizon planningHorizon;
  private MissionModelWrapper missionModelWrapper;
  private MerlInsightRules rules;
  private AerieController controller;
  private Problem smallProblem;

  /** constructs an empty plan with the test model/horizon **/
  private PlanInMemory makeEmptyPlan() {
    return new PlanInMemory(missionModelWrapper);
  }

  public void schedule(){
    var solver = new PrioritySolver(new HuginnConfiguration(), smallProblem);
    solver.checkSimBeforeInsertingActInPlan();
    var plan = solver.getNextSolution();
    solver.printEvaluation();
    TestUtility.printPlan(plan.get());
    if(controller.isLocalAerieUp()) {
      controller.initEmptyPlan(plan.get(), planningHorizon.getStartAerie(), planningHorizon.getEndAerie(), null);
      controller.createSimulation(plan.get());
      controller.sendPlan(plan.get(), planningHorizon.getStartAerie(), planningHorizon.getEndAerie(), null);
    } else{
      System.out.println("Not sending plan because there is no local instance of Aerie present at " + TestUtility.LOCAL_AERIE);
    }
  }

  @Test
  public void firstRule() {
    smallProblem.addAll(rules.getFirstRuleGoals());
    schedule();
  }
  @Test
  public void secondRule() {
    smallProblem.addAll(rules.getSecondRuleGoals());
    schedule();
  }
  @Test
  public void thirdRule() {
    smallProblem.addAll(rules.getThirdRuleGoals());
    schedule();
  }

  @Disabled
  @Test
  public void deleteAllPlans(){
    controller.deleteAllPlans();
  }
}
