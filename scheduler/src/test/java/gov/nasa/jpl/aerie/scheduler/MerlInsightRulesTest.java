package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

public class MerlInsightRulesTest {

  @BeforeEach
  void setUp(){
    planningHorizon = new PlanningHorizon(new Time(0), new Time(48*3600));
    MissionModel<?> aerieLanderMissionModel = MerlinSightTestUtility.getMerlinSightMissionModel();
    final var aerieLanderSchedulerModel = MerlinSightTestUtility.getMerlinSightSchedulerModel();
    rules = new MerlInsightRules(aerieLanderMissionModel, planningHorizon, aerieLanderSchedulerModel);
    rules.getSimulationFacade().simulatePlan(makeEmptyPlan());
    plan = makeEmptyPlan();
    controller = new AerieController(MerlinSightTestUtility.LOCAL_AERIE, MerlinSightTestUtility.latest, false, planningHorizon, rules.getActivityTypes());
    smallProblem = new Problem(aerieLanderMissionModel, planningHorizon, aerieLanderSchedulerModel);
  }

  private PlanningHorizon planningHorizon;
  private MerlInsightRules rules;
  private AerieController controller;
  private Problem smallProblem;
  private Plan plan;
  /** constructs an empty plan with the test model/horizon **/
  private PlanInMemory makeEmptyPlan() {
    return new PlanInMemory();
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
    smallProblem.setGoals(new ArrayList<>(rules.getFirstRuleGoals().values()));
    schedule();
  }
  @Test
  public void secondRule() {
    smallProblem.setGoals(new ArrayList<>(rules.getSecondRuleGoals().values()));
    schedule();
  }

  @Test
  public void thirdRule() {
    var goals = new ArrayList<Goal>();
    goals.add(rules.generateDSNVisibilityAllocationGoal().getValue());
    goals.addAll(rules.getThirdRuleGoals().values());
    smallProblem.setGoals(goals);
    schedule();
  }

  @Disabled
  @Test
  public void deleteAllPlans(){
    controller.deleteAllPlans();
  }
}
