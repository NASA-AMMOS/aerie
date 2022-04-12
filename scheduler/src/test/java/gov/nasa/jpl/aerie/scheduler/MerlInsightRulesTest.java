package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.scheduler.goals.Goal;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.PlanInMemory;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.model.Problem;
import gov.nasa.jpl.aerie.scheduler.model.Time;
import gov.nasa.jpl.aerie.scheduler.solver.PrioritySolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

public class MerlInsightRulesTest {

  @BeforeEach
  void setUp(){
    planningHorizon = new PlanningHorizon(new Time(0), new Time(48 * 3600));
    MissionModel<?> aerieLanderMissionModel = MerlinSightTestUtility.getMerlinSightMissionModel();
    final var aerieLanderSchedulerModel = MerlinSightTestUtility.getMerlinSightSchedulerModel();
    rules = new MerlInsightRules(aerieLanderMissionModel, planningHorizon, aerieLanderSchedulerModel);
    plan = makeEmptyPlan();
    smallProblem = new Problem(aerieLanderMissionModel, planningHorizon, rules.getSimulationFacade(), aerieLanderSchedulerModel);
  }

  private PlanningHorizon planningHorizon;
  private MerlInsightRules rules;
  private Problem smallProblem;
  private Plan plan;
  /** constructs an empty plan with the test model/horizon **/
  private PlanInMemory makeEmptyPlan() {
    return new PlanInMemory();
  }

  public void schedule(){
    smallProblem.setInitialPlan(plan);
    rules.getGlobalConstraints().forEach(smallProblem::add);
    var solver = new PrioritySolver(smallProblem);
    solver.checkSimBeforeInsertingActInPlan();
    plan = solver.getNextSolution().get();
    solver.printEvaluation();
    MerlinSightTestUtility.printPlan(plan);
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
}
