package gov.nasa.jpl.aerie.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class MerlInsightRulesTest {

  @BeforeEach
  void setUp(){
    planningHorizon = new PlanningHorizon(new Time(0), new Time(24*3600));
    rules = new MerlInsightRules(MerlinSightTestUtility.getMerlinSightMissionModel(planningHorizon));
    missionModelWrapper = rules.getMissionModel();
    missionModelWrapper.getSimulationFacade().simulatePlan(makeEmptyPlan());
    plan = makeEmptyPlan();
    controller = new AerieController(MerlinSightTestUtility.LOCAL_AERIE, MerlinSightTestUtility.latest, false, planningHorizon, missionModelWrapper);
    smallProblem = new Problem(missionModelWrapper);
  }

  private PlanningHorizon planningHorizon;
  private MissionModelWrapper missionModelWrapper;
  private MerlInsightRules rules;
  private AerieController controller;
  private Problem smallProblem;
  private Plan plan;
  /** constructs an empty plan with the test model/horizon **/
  private PlanInMemory makeEmptyPlan() {
    return new PlanInMemory(missionModelWrapper);
  }

  public void schedule(){
    smallProblem.setInitialPlan(plan);
    var solver = new PrioritySolver(new HuginnConfiguration(), smallProblem);
    solver.checkSimBeforeInsertingActInPlan();
    plan = solver.getNextSolution().get();
    solver.printEvaluation();
    MerlinSightTestUtility.printPlan(plan);
    if(controller.isLocalAerieUp()) {
      controller.initEmptyPlan(plan, planningHorizon.getStartAerie(), planningHorizon.getEndAerie(), null);
      controller.createSimulation(plan);
      controller.sendPlan(plan, planningHorizon.getStartAerie(), planningHorizon.getEndAerie(), null);
    } else{
      System.out.println("Not sending plan because there is no local instance of Aerie present at " + MerlinSightTestUtility.LOCAL_AERIE);
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
    smallProblem.add(rules.generateDSNVisibilityAllocationGoal());
    smallProblem.addAll(rules.getThirdRuleGoals());
    schedule();
  }

  @Disabled
  @Test
  public void deleteAllPlans(){
    controller.deleteAllPlans();
  }
}
