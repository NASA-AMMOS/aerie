package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Spans;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.ActivitySpan;
import gov.nasa.jpl.aerie.constraints.tree.Expression;
import gov.nasa.jpl.aerie.constraints.tree.ForEachActivitySpans;
import gov.nasa.jpl.aerie.constraints.tree.WindowsWrapperExpression;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions.TimeAnchor;
import gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions.TimeExpression;
import gov.nasa.jpl.aerie.scheduler.goals.CoexistenceGoal;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.PlanInMemory;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.model.Problem;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirectiveId;
import gov.nasa.jpl.aerie.scheduler.simulation.SimulationFacade;
import gov.nasa.jpl.aerie.scheduler.solver.PrioritySolver;
import org.apache.commons.lang3.function.TriFunction;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
Considering the four boolean variables: createPersistentAnchor,	allowActivityUpdate,	missingActAssociationsWithAnchor,	missingActAssociationsWithoutAnchor
that define all possible anchoring cases (see CoexistenceGoal.getConflicts), the test cases below cover all possible combinations
 */
public class TestPersistentAnchor {

  public record TestData(
      Optional<Plan> plan,
      ArrayList<SchedulingActivityDirective> actsToBeAnchored,
      ArrayList<SchedulingActivityDirective> templateActsAnchoring,
      ArrayList<SchedulingActivityDirective> templateActsNotAnchoring
  ) {}

  private static final Logger logger = LoggerFactory.getLogger(TestPersistentAnchor.class);

  private static Expression<Spans> spansOfConstraintExpression(
      final ActivityExpression ae)
  {
    return new ForEachActivitySpans(
        new TriFunction<>() {
          @Override
          public Boolean apply(
              final ActivityInstance activityInstance,
              final SimulationResults simResults,
              final EvaluationEnvironment environment)
          {
            final var startTime = activityInstance.interval.start;
            if (!activityInstance.type.equals(ae.type().getName())) return false;
            for (final var arg : ae
                .arguments()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry
                    .getValue()
                    .evaluate(
                        simResults,
                        Interval.at(startTime),
                        environment)
                    .valueAt(startTime)
                    .orElseThrow()))
                .entrySet()) {
              if (!arg.getValue().equals(activityInstance.parameters.get(arg.getKey()))) return false;
            }
            return true;
          }

          @Override
          public String toString() {
            return "(filter by ActivityExpression)";
          }
        },
        "alias" + ae.type(),
        new ActivitySpan("alias" + ae.type())
    );
  }

  public boolean allAnchorsIncluded(TestData testData) {
    if(testData.actsToBeAnchored == null || testData.actsToBeAnchored.isEmpty())
      return true;
    if(testData.plan.isEmpty())
      return false;

    Set<SchedulingActivityDirectiveId> planActivityAnchors = testData.plan.get().getAnchorIds();
    for(SchedulingActivityDirective act : testData.actsToBeAnchored){
      if(!planActivityAnchors.contains(act.anchorId()))
        return false;
    }
    return true;
}

  /**
   * Test that all directives added manually to the plan in the test (not by the scheduler)
   * with the purpose of being anchored do actually get an anchor
   * @param testData
   * @return
   */
  public boolean allAnchoringActivitiesAnchored(TestData testData) {
    if(testData.templateActsAnchoring == null || testData.templateActsAnchoring.isEmpty())
      return true;
    if(testData.plan.isEmpty())
      return false;

    Set<SchedulingActivityDirectiveId> anchorIds = testData.actsToBeAnchored.stream()
                             .map(SchedulingActivityDirective::id)
                             .collect(Collectors.toSet());

    Map<SchedulingActivityDirectiveId, SchedulingActivityDirective> mapIdToActivity = testData.plan.get().getActivitiesById();
    for (SchedulingActivityDirective act: testData.templateActsAnchoring){
      SchedulingActivityDirective directive = mapIdToActivity.get(act.id());
      if (directive.anchorId() == null || !anchorIds.contains(directive.anchorId()))
        return false;
    }
    return true;
  }

  /**
   * Test that all directives added manually to the plan in the test (not by the scheduler)
   * that cannot be anchored (e.g. because it is not allowed to modify them) don't actually get an anchor
   * @param testData
   * @return
   */
  public boolean allNonAnchoringActivitiesAreNotAnchored(TestData testData) {
    if(testData.templateActsNotAnchoring == null || testData.templateActsNotAnchoring.isEmpty())
      return true;
    if(testData.plan.isEmpty())
      return false;

    Map<SchedulingActivityDirectiveId, SchedulingActivityDirective> mapIdToActivity = testData.plan.get().getActivitiesById();
    for (SchedulingActivityDirective act: testData.templateActsNotAnchoring){
      SchedulingActivityDirective directive = mapIdToActivity.get(act.id());
      if (directive.anchorId() != null)
        return false;
    }
    return true;
  }

  /*
  Test 1-a
  This case covers the scenarios with truth values for variables createPersistentAnchor,	allowActivityUpdate,	missingActAssociationsWithAnchor,	missingActAssociationsWithoutAnchor: "x x 0 0",
  that is, those cases in which the plan does not have any activity that can be associated to the goal and needs to create new ones.
  In that case, a MissingActivityTemplateConflict will be created to resolve the conflict
  Template activities start at the start of the "for each" directive
   */
  @Test
  public void testPlanWithoutAssociableActivitiesA(){
    TestData testData = testPlanWithoutAssociableActivitiesA(true, true);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));

    testData = testPlanWithoutAssociableActivitiesA(true, false);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));

    testData = testPlanWithoutAssociableActivitiesA(false, true);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));

    testData = testPlanWithoutAssociableActivitiesA(false, false);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  public TestData testPlanWithoutAssociableActivitiesA(boolean createPersistentAnchor, boolean allowActivityUpdate){
    ArrayList<SchedulingActivityDirective> anchoredActs = new ArrayList<SchedulingActivityDirective>();
    Interval period = Interval.betweenClosedOpen(Duration.of(0, Duration.HOURS), Duration.of(20, Duration.HOURS));

    final var bananaMissionModel = SimulationUtility.getBananaMissionModel();
    final var planningHorizon = new PlanningHorizon(TestUtility.timeFromEpochHours(0), TestUtility.timeFromEpochHours(20));
    Problem problem = new Problem(bananaMissionModel, planningHorizon, new SimulationFacade(
        planningHorizon,
        bananaMissionModel, SimulationUtility.getBananaSchedulerModel()), SimulationUtility.getBananaSchedulerModel());

    //have some activity already present
    //  create a PlanInMemory, add ActivityInstances
    PlanInMemory partialPlan = new PlanInMemory();
    final var actTypeA = problem.getActivityType("GrowBanana");
    final var actTypeB = problem.getActivityType("PickBanana");

    SchedulingActivityDirective act1 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie(), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act1);
    anchoredActs.add(act1);

    SchedulingActivityDirective act2 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie().plus(Duration.of(5, Duration.HOURS)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act2);
    anchoredActs.add(act2);

    SchedulingActivityDirective act3 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie().plus(Duration.of(10, Duration.HOURS)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act3);
    anchoredActs.add(act3);

    //  pass this plan as initialPlan to Problem object
    problem.setInitialPlan(partialPlan);
    //want to create another activity for each of the already present activities
    //  foreach with activityexpression
    ActivityExpression framework = new ActivityExpression.Builder()
        .ofType(actTypeA)
        .build();

    //and cut off in the middle of one of the already present activities (period ends at 18)
    CoexistenceGoal goal = new CoexistenceGoal.Builder()
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(period, true)))
        .forEach(spansOfConstraintExpression(framework))
        .thereExistsOne(new ActivityExpression.Builder()
                            .ofType(actTypeB)
                            .withArgument("quantity", SerializedValue.of(1))
                            .build())
        .startsAt(TimeAnchor.START)
        .aliasForAnchors("Grow and Pick Bananas")
        .createPersistentAnchor(createPersistentAnchor)
        .allowActivityUpdate(allowActivityUpdate)
        .withinPlanHorizon(planningHorizon)
        .build();

    problem.setGoals(List.of(goal));

    final var solver = new PrioritySolver(problem);
    var plan = solver.getNextSolution();
    for(SchedulingActivityDirective a : plan.get().getActivitiesByTime()){
      logger.debug(a.startOffset().toString() + ", " + a.duration().toString());
    }
    return new TestData(plan, anchoredActs, null, null);
  }

  /*
  Test 1-b
  This case covers the scenarios with truth values for variables createPersistentAnchor,	allowActivityUpdate,	missingActAssociationsWithAnchor,	missingActAssociationsWithoutAnchor: "x x 0 0",
  that is, those cases in which the plan does not have any activity that can be associated to the goal and needs to create new ones.
  In that case, a MissingActivityTemplateConflict will be created to resolve the conflict
  Template activities start at the end of for each activity plus 5 minutes
   */
  @Test
  public void testPlanWithoutAssociableActivitiesB(){
    TestData testData = testPlanWithoutAssociableActivitiesB(true, true);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));

    testData = testPlanWithoutAssociableActivitiesB(true, false);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));

    testData = testPlanWithoutAssociableActivitiesB(false, true);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));

    testData = testPlanWithoutAssociableActivitiesB(false, false);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  public TestData testPlanWithoutAssociableActivitiesB(boolean createPersistentAnchor, boolean allowActivityUpdate){
    ArrayList<SchedulingActivityDirective> anchoredActs = new ArrayList<SchedulingActivityDirective>();
    Interval period = Interval.betweenClosedOpen(Duration.of(0, Duration.HOURS), Duration.of(20, Duration.HOURS));

    final var bananaMissionModel = SimulationUtility.getBananaMissionModel();
    final var planningHorizon = new PlanningHorizon(TestUtility.timeFromEpochHours(0), TestUtility.timeFromEpochHours(20));
    Problem problem = new Problem(bananaMissionModel, planningHorizon, new SimulationFacade(
        planningHorizon,
        bananaMissionModel, SimulationUtility.getBananaSchedulerModel()), SimulationUtility.getBananaSchedulerModel());

    //have some activity already present
    //  create a PlanInMemory, add ActivityInstances
    PlanInMemory partialPlan = new PlanInMemory();
    final var actTypeA = problem.getActivityType("GrowBanana");
    final var actTypeB = problem.getActivityType("PickBanana");

    SchedulingActivityDirective act1 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie(), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act1);
    anchoredActs.add(act1);

    SchedulingActivityDirective act2 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie().plus(Duration.of(5, Duration.HOURS)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act2);
    anchoredActs.add(act2);

    SchedulingActivityDirective act3 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie().plus(Duration.of(10, Duration.HOURS)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act3);
    anchoredActs.add(act3);

    //  pass this plan as initialPlan to Problem object
    problem.setInitialPlan(partialPlan);
    //want to create another activity for each of the already present activities
    //  foreach with activityexpression
    ActivityExpression framework = new ActivityExpression.Builder()
        .ofType(actTypeA)
        .build();

    //and cut off in the middle of one of the already present activities (period ends at 18)
    CoexistenceGoal goal = new CoexistenceGoal.Builder()
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(period, true)))
        .forEach(spansOfConstraintExpression(framework))
        .thereExistsOne(new ActivityExpression.Builder()
                            .ofType(actTypeB)
                            .withArgument("quantity", SerializedValue.of(1))
                            .build())
        .startsAt(TimeExpression.offsetByAfterEnd(Duration.of(5, Duration.MINUTE)))
        .aliasForAnchors("Grow and Pick Bananas")
        .createPersistentAnchor(createPersistentAnchor)
        .allowActivityUpdate(allowActivityUpdate)
        .withinPlanHorizon(planningHorizon)
        .build();

    problem.setGoals(List.of(goal));

    final var solver = new PrioritySolver(problem);
    var plan = solver.getNextSolution();
    for(SchedulingActivityDirective a : plan.get().getActivitiesByTime()){
      logger.debug(a.startOffset().toString() + ", " + a.duration().toString());
    }
    return new TestData(plan, anchoredActs, null, null);
  }

  /*
  Test 2-a
  This case covers the scenarios with truth values for variables createPersistentAnchor,	allowActivityUpdate,	missingActAssociationsWithAnchor,	missingActAssociationsWithoutAnchor: "x 0 0 1",
  that is, those cases in which the goal doesn't allow to update existing activities but the plan only have activities that can be associated if updated with the appropriate anchor
  In that case, a MissingActivityTemplateConflict will be created to resolve the conflict by creating new activities
  Template activities start at the start of the "for each" directive
   */
  @Test
  public void testPlanNoUpdateAssociableActivitiesWithoutAnchorA(){
    TestData testData = testPlanNoUpdateAssociableActivitiesWithoutAnchorA(true, false);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));

    testData = testPlanNoUpdateAssociableActivitiesWithoutAnchorA(false, false);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  public TestData testPlanNoUpdateAssociableActivitiesWithoutAnchorA(boolean createPersistentAnchor, boolean allowActivityUpdate){
    ArrayList<SchedulingActivityDirective> actsToBeAnchored = new ArrayList<>();
    ArrayList<SchedulingActivityDirective> templateActsAnchoring = new ArrayList<>();
    ArrayList<SchedulingActivityDirective> templateActsNotAnchoring = new ArrayList<>();

    Interval period = Interval.betweenClosedOpen(Duration.of(0, Duration.HOURS), Duration.of(20, Duration.HOURS));

    final var bananaMissionModel = SimulationUtility.getBananaMissionModel();
    final var planningHorizon = new PlanningHorizon(TestUtility.timeFromEpochHours(0), TestUtility.timeFromEpochHours(20));
    Problem problem = new Problem(bananaMissionModel, planningHorizon, new SimulationFacade(
        planningHorizon,
        bananaMissionModel, SimulationUtility.getBananaSchedulerModel()), SimulationUtility.getBananaSchedulerModel());

    //have some activity already present
    //  create a PlanInMemory, add ActivityInstances
    PlanInMemory partialPlan = new PlanInMemory();
    final var actTypeA = problem.getActivityType("GrowBanana");

    SchedulingActivityDirective act1 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie(), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act1);
    actsToBeAnchored.add(act1);

    SchedulingActivityDirective act2 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie().plus(Duration.of(5, Duration.HOURS)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act2);
    actsToBeAnchored.add(act2);

    SchedulingActivityDirective act3 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie().plus(Duration.of(10, Duration.HOURS)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act3);
    actsToBeAnchored.add(act3);


    final var actTypeB = problem.getActivityType("PickBanana");
    SchedulingActivityDirective act4 = SchedulingActivityDirective.of(actTypeB, planningHorizon.getStartAerie(), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, null, true);
    partialPlan.add(act4);
    templateActsNotAnchoring.add(act4);

    SchedulingActivityDirective act5 = SchedulingActivityDirective.of(actTypeB, planningHorizon.getStartAerie().plus(Duration.of(5, Duration.HOURS)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, null, true);
    partialPlan.add(act5);
    templateActsNotAnchoring.add(act5);

    SchedulingActivityDirective act6 = SchedulingActivityDirective.of(actTypeB, planningHorizon.getStartAerie().plus(Duration.of(10, Duration.HOURS)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, null, true);
    partialPlan.add(act6);
    templateActsNotAnchoring.add(act6);

    //  pass this plan as initialPlan to Problem object
    problem.setInitialPlan(partialPlan);
    //want to create another activity for each of the already present activities
    //  foreach with activityexpression
    ActivityExpression framework = new ActivityExpression.Builder()
        .ofType(actTypeA)
        .build();

    //and cut off in the middle of one of the already present activities (period ends at 18)
    CoexistenceGoal goal = new CoexistenceGoal.Builder()
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(period, true)))
        .forEach(spansOfConstraintExpression(framework))
        .thereExistsOne(new ActivityExpression.Builder()
                            .ofType(actTypeB)
                            .withArgument("quantity", SerializedValue.of(1))
                            .build())
        .startsAt(TimeAnchor.START)
        .aliasForAnchors("Grow and Pick Bananas")
        .createPersistentAnchor(createPersistentAnchor)
        .allowActivityUpdate(allowActivityUpdate)
        .withinPlanHorizon(planningHorizon)
        .build();

    problem.setGoals(List.of(goal));

    final var solver = new PrioritySolver(problem);
    var plan = solver.getNextSolution();
    for(SchedulingActivityDirective a : plan.get().getActivitiesByTime()){
      logger.debug(a.startOffset().toString() + ", " + a.duration().toString());
    }
    return new TestData(plan, actsToBeAnchored, templateActsAnchoring, templateActsNotAnchoring);
  }

  /*
  Test 2-b
  This case covers the scenarios with truth values for variables createPersistentAnchor,	allowActivityUpdate,	missingActAssociationsWithAnchor,	missingActAssociationsWithoutAnchor: "x 0 0 1",
  that is, those cases in which the goal doesn't allow to update existing activities but the plan only have activities that can be associated if updated with the appropriate anchor
  In that case, a MissingActivityTemplateConflict will be created to resolve the conflict by creating new activities
  Template activities start at the end of for each activity plus 5 minutes
   */
  @Test
  public void testPlanNoUpdateAssociableActivitiesWithoutAnchorB(){
    TestData testData = testPlanNoUpdateAssociableActivitiesWithoutAnchorB(true, false);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));

    testData = testPlanNoUpdateAssociableActivitiesWithoutAnchorB(false, false);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  public TestData testPlanNoUpdateAssociableActivitiesWithoutAnchorB(boolean createPersistentAnchor, boolean allowActivityUpdate){
    ArrayList<SchedulingActivityDirective> actsToBeAnchored = new ArrayList<>();
    ArrayList<SchedulingActivityDirective> templateActsAnchoring = new ArrayList<>();
    ArrayList<SchedulingActivityDirective> templateActsNotAnchoring = new ArrayList<>();

    Interval period = Interval.betweenClosedOpen(Duration.of(0, Duration.HOURS), Duration.of(20, Duration.HOURS));

    final var bananaMissionModel = SimulationUtility.getBananaMissionModel();
    final var planningHorizon = new PlanningHorizon(TestUtility.timeFromEpochHours(0), TestUtility.timeFromEpochHours(20));
    Problem problem = new Problem(bananaMissionModel, planningHorizon, new SimulationFacade(
        planningHorizon,
        bananaMissionModel, SimulationUtility.getBananaSchedulerModel()), SimulationUtility.getBananaSchedulerModel());

    //have some activity already present
    //  create a PlanInMemory, add ActivityInstances
    PlanInMemory partialPlan = new PlanInMemory();
    final var actTypeA = problem.getActivityType("GrowBanana");

    SchedulingActivityDirective act1 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie(), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act1);
    actsToBeAnchored.add(act1);

    SchedulingActivityDirective act2 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie().plus(Duration.of(5, Duration.HOURS)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act2);
    actsToBeAnchored.add(act2);

    SchedulingActivityDirective act3 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie().plus(Duration.of(10, Duration.HOURS)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act3);
    actsToBeAnchored.add(act3);


    final var actTypeB = problem.getActivityType("PickBanana");
    SchedulingActivityDirective act4 = SchedulingActivityDirective.of(actTypeB, planningHorizon.getStartAerie().plus(Duration.of(0, Duration.HOURS)).plus(Duration.of(3, Duration.HOURS)).plus(Duration.of(5, Duration.MINUTES)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, null, true);
    partialPlan.add(act4);
    templateActsNotAnchoring.add(act4);

    SchedulingActivityDirective act5 = SchedulingActivityDirective.of(actTypeB, planningHorizon.getStartAerie().plus(Duration.of(5, Duration.HOURS)).plus(Duration.of(3, Duration.HOURS)).plus(Duration.of(5, Duration.MINUTES)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, null, true);
    partialPlan.add(act5);
    templateActsNotAnchoring.add(act5);

    SchedulingActivityDirective act6 = SchedulingActivityDirective.of(actTypeB, planningHorizon.getStartAerie().plus(Duration.of(10, Duration.HOURS)).plus(Duration.of(3, Duration.HOURS)).plus(Duration.of(5, Duration.MINUTES)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, null, true);
    partialPlan.add(act6);
    templateActsNotAnchoring.add(act6);

    //  pass this plan as initialPlan to Problem object
    problem.setInitialPlan(partialPlan);
    //want to create another activity for each of the already present activities
    //  foreach with activityexpression
    ActivityExpression framework = new ActivityExpression.Builder()
        .ofType(actTypeA)
        .build();

    //and cut off in the middle of one of the already present activities (period ends at 18)
    CoexistenceGoal goal = new CoexistenceGoal.Builder()
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(period, true)))
        .forEach(spansOfConstraintExpression(framework))
        .thereExistsOne(new ActivityExpression.Builder()
                            .ofType(actTypeB)
                            .withArgument("quantity", SerializedValue.of(1))
                            .build())
        .startsAt(TimeExpression.offsetByAfterEnd(Duration.of(5, Duration.MINUTE)))
        .aliasForAnchors("Grow and Pick Bananas")
        .createPersistentAnchor(createPersistentAnchor)
        .allowActivityUpdate(allowActivityUpdate)
        .withinPlanHorizon(planningHorizon)
        .build();

    problem.setGoals(List.of(goal));

    final var solver = new PrioritySolver(problem);
    var plan = solver.getNextSolution();
    for(SchedulingActivityDirective a : plan.get().getActivitiesByTime()){
      logger.debug(a.startOffset().toString() + ", " + a.duration().toString());
    }
    return new TestData(plan, actsToBeAnchored, templateActsAnchoring, templateActsNotAnchoring);
  }


  /*
  Test 3-a
  This case covers the scenarios with truth values for variables createPersistentAnchor,	allowActivityUpdate,	missingActAssociationsWithAnchor,	missingActAssociationsWithoutAnchor: "x 1 0 1",
  that is, those cases in which the goal ALLOWS to update existing activities and the plan only have activities that can be associated if updated with the appropriate anchor
  In that case, the three activities PickBanana will be modified (by anchoring them to the respective GrowBanana) and associated with the goal
  Template activities start at the start of the "for each" directive
  */
  @Test
  public void testPlanUpdateWithAssociableActivitiesWithoutAnchorA(){
    TestData testData = testPlanUpdateWithAssociableActivitiesWithoutAnchorA(true, true);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));

    testData = testPlanUpdateWithAssociableActivitiesWithoutAnchorA(false, true);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  public TestData testPlanUpdateWithAssociableActivitiesWithoutAnchorA(boolean createPersistentAnchor, boolean allowActivityUpdate){
    ArrayList<SchedulingActivityDirective> actsToBeAnchored = new ArrayList<>();
    ArrayList<SchedulingActivityDirective> templateActsAnchoring = new ArrayList<>();
    ArrayList<SchedulingActivityDirective> templateActsNotAnchoring = new ArrayList<>();

    Interval period = Interval.betweenClosedOpen(Duration.of(0, Duration.HOURS), Duration.of(20, Duration.HOURS));

    final var bananaMissionModel = SimulationUtility.getBananaMissionModel();
    final var planningHorizon = new PlanningHorizon(TestUtility.timeFromEpochHours(0), TestUtility.timeFromEpochHours(20));
    Problem problem = new Problem(bananaMissionModel, planningHorizon, new SimulationFacade(
        planningHorizon,
        bananaMissionModel, SimulationUtility.getBananaSchedulerModel()), SimulationUtility.getBananaSchedulerModel());

    //have some activity already present
    //  create a PlanInMemory, add ActivityInstances
    PlanInMemory partialPlan = new PlanInMemory();
    final var actTypeA = problem.getActivityType("GrowBanana");
    SchedulingActivityDirective act1 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie(), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
        partialPlan.add(act1);
    actsToBeAnchored.add(act1);

    SchedulingActivityDirective act2 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie().plus(Duration.of(5, Duration.HOURS)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
        partialPlan.add(act2);
    actsToBeAnchored.add(act2);

    SchedulingActivityDirective act3 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie().plus(Duration.of(10, Duration.HOURS)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
        partialPlan.add(act3);
    actsToBeAnchored.add(act3);


    final var actTypeB = problem.getActivityType("PickBanana");
    SchedulingActivityDirective act4 = SchedulingActivityDirective.of(actTypeB, planningHorizon.getStartAerie(), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, null, true);
    partialPlan.add(act4);
    templateActsAnchoring.add(act4);

    SchedulingActivityDirective act5 = SchedulingActivityDirective.of(actTypeB, planningHorizon.getStartAerie().plus(Duration.of(5, Duration.HOURS)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, null, true);
    partialPlan.add(act5);
    templateActsAnchoring.add(act5);

    SchedulingActivityDirective act6 = SchedulingActivityDirective.of(actTypeB, planningHorizon.getStartAerie().plus(Duration.of(10, Duration.HOURS)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, null, true);
    partialPlan.add(act6);
    templateActsAnchoring.add(act6);


    //  pass this plan as initialPlan to Problem object
    problem.setInitialPlan(partialPlan);
    //want to create another activity for each of the already present activities
    //  foreach with activityexpression
    ActivityExpression framework = new ActivityExpression.Builder()
        .ofType(actTypeA)
        .build();

    //and cut off in the middle of one of the already present activities (period ends at 18)
    CoexistenceGoal goal = new CoexistenceGoal.Builder()
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(period, true)))
        .forEach(spansOfConstraintExpression(framework))
        .thereExistsOne(new ActivityExpression.Builder()
                            .ofType(actTypeB)
                            .withArgument("quantity", SerializedValue.of(1))
                            .build())
        .startsAt(TimeAnchor.START)
        .aliasForAnchors("Grow and Pick Bananas")
        .createPersistentAnchor(createPersistentAnchor)
        .allowActivityUpdate(allowActivityUpdate)
        .withinPlanHorizon(planningHorizon)
        .build();

    problem.setGoals(List.of(goal));

    final var solver = new PrioritySolver(problem);
    var plan = solver.getNextSolution();
    for(SchedulingActivityDirective a : plan.get().getActivitiesByTime()){
      logger.debug(a.startOffset().toString() + ", " + a.duration().toString());
    }
    return new TestData(plan, actsToBeAnchored, templateActsAnchoring, templateActsNotAnchoring);
  }


  /*
  Test 3-b
  This case covers the scenarios with truth values for variables createPersistentAnchor,	allowActivityUpdate,	missingActAssociationsWithAnchor,	missingActAssociationsWithoutAnchor: "x 1 0 1",
  that is, those cases in which the goal ALLOWS to update existing activities but the plan only have activities that can be associated if updated with the appropriate anchor
  In that case, the three activities PickBanana will be modified (by anchoring them to the respective GrowBanana) and associated with the goal
  Template activities start at the end of for each activity plus 5 minutes
  */
  @Test
  public void testPlanUpdateWithAssociableActivitiesWithoutAnchorB(){
    TestData testData = testPlanUpdateWithAssociableActivitiesWithoutAnchorB(true, true);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));

    testData = testPlanUpdateWithAssociableActivitiesWithoutAnchorB(false, true);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  public TestData testPlanUpdateWithAssociableActivitiesWithoutAnchorB(boolean createPersistentAnchor, boolean allowActivityUpdate){
    ArrayList<SchedulingActivityDirective> actsToBeAnchored = new ArrayList<>();
    ArrayList<SchedulingActivityDirective> templateActsAnchoring = new ArrayList<>();
    ArrayList<SchedulingActivityDirective> templateActsNotAnchoring = new ArrayList<>();

    Interval period = Interval.betweenClosedOpen(Duration.of(0, Duration.HOURS), Duration.of(20, Duration.HOURS));

    final var bananaMissionModel = SimulationUtility.getBananaMissionModel();
    final var planningHorizon = new PlanningHorizon(TestUtility.timeFromEpochHours(0), TestUtility.timeFromEpochHours(20));
    Problem problem = new Problem(bananaMissionModel, planningHorizon, new SimulationFacade(
        planningHorizon,
        bananaMissionModel, SimulationUtility.getBananaSchedulerModel()), SimulationUtility.getBananaSchedulerModel());

    //have some activity already present
    //  create a PlanInMemory, add ActivityInstances
    PlanInMemory partialPlan = new PlanInMemory();
    final var actTypeA = problem.getActivityType("GrowBanana");
    SchedulingActivityDirective act1 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie(), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act1);
    actsToBeAnchored.add(act1);

    SchedulingActivityDirective act2 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie().plus(Duration.of(5, Duration.HOURS)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act2);
    actsToBeAnchored.add(act2);

    SchedulingActivityDirective act3 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie().plus(Duration.of(10, Duration.HOURS)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act3);
    actsToBeAnchored.add(act3);


    final var actTypeB = problem.getActivityType("PickBanana");
    SchedulingActivityDirective act4 = SchedulingActivityDirective.of(actTypeB, planningHorizon.getStartAerie().plus(Duration.of(0, Duration.HOURS)).plus(Duration.of(3, Duration.HOURS)).plus(Duration.of(5, Duration.MINUTES)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, null, true);
    partialPlan.add(act4);
    templateActsAnchoring.add(act4);

    SchedulingActivityDirective act5 = SchedulingActivityDirective.of(actTypeB, planningHorizon.getStartAerie().plus(Duration.of(5, Duration.HOURS)).plus(Duration.of(3, Duration.HOURS)).plus(Duration.of(5, Duration.MINUTES)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, null, true);
    partialPlan.add(act5);
    templateActsAnchoring.add(act5);

    SchedulingActivityDirective act6 = SchedulingActivityDirective.of(actTypeB, planningHorizon.getStartAerie().plus(Duration.of(10, Duration.HOURS)).plus(Duration.of(3, Duration.HOURS)).plus(Duration.of(5, Duration.MINUTES)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, null, true);
    partialPlan.add(act6);
    templateActsAnchoring.add(act6);


    //  pass this plan as initialPlan to Problem object
    problem.setInitialPlan(partialPlan);
    //want to create another activity for each of the already present activities
    //  foreach with activityexpression
    ActivityExpression framework = new ActivityExpression.Builder()
        .ofType(actTypeA)
        .build();

    //and cut off in the middle of one of the already present activities (period ends at 18)
    CoexistenceGoal goal = new CoexistenceGoal.Builder()
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(period, true)))
        .forEach(spansOfConstraintExpression(framework))
        .thereExistsOne(new ActivityExpression.Builder()
                            .ofType(actTypeB)
                            .withArgument("quantity", SerializedValue.of(1))
                            .build())
        .startsAt(TimeExpression.offsetByAfterEnd(Duration.of(5, Duration.MINUTE)))
        .aliasForAnchors("Grow and Pick Bananas")
        .createPersistentAnchor(createPersistentAnchor)
        .allowActivityUpdate(allowActivityUpdate)
        .withinPlanHorizon(planningHorizon)
        .build();

    problem.setGoals(List.of(goal));

    final var solver = new PrioritySolver(problem);
    var plan = solver.getNextSolution();
    for(SchedulingActivityDirective a : plan.get().getActivitiesByTime()){
      logger.debug(a.startOffset().toString() + ", " + a.duration().toString());
    }
    return new TestData(plan, actsToBeAnchored, templateActsAnchoring, templateActsNotAnchoring);
  }


  /*
  Test 4-a
  This case covers the scenarios with truth values for variables createPersistentAnchor,	allowActivityUpdate,	missingActAssociationsWithAnchor,	missingActAssociationsWithoutAnchor: "x x 1 0",
  that is, those cases in which the goal ALLOWS to update existing activities and the plan already have activities that can be associated without modifications
  In that case, the three activities PickBanana will be associated with the goal
  Template activities start at the start of the "for each" directive
  */
  @Test
  public void testPlanUpdateWithAssociableActivitiesWithAnchorA() {
    TestData testData = testPlanUpdateWithAssociableActivitiesWithAnchorA(true, true);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));

    testData = testPlanUpdateWithAssociableActivitiesWithAnchorA(true, false);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));

    testData = testPlanUpdateWithAssociableActivitiesWithAnchorA(false, true);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));

    testData = testPlanUpdateWithAssociableActivitiesWithAnchorA(false, false);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  public TestData testPlanUpdateWithAssociableActivitiesWithAnchorA(boolean createPersistentAnchor, boolean allowActivityUpdate){
    ArrayList<SchedulingActivityDirective> actsToBeAnchored = new ArrayList<>();
    ArrayList<SchedulingActivityDirective> templateActsAnchoring = new ArrayList<>();
    ArrayList<SchedulingActivityDirective> templateActsNotAnchoring = new ArrayList<>();

    Interval period = Interval.betweenClosedOpen(Duration.of(0, Duration.HOURS), Duration.of(20, Duration.HOURS));

    final var bananaMissionModel = SimulationUtility.getBananaMissionModel();
    final var planningHorizon = new PlanningHorizon(TestUtility.timeFromEpochHours(0), TestUtility.timeFromEpochHours(20));
    Problem problem = new Problem(bananaMissionModel, planningHorizon, new SimulationFacade(
        planningHorizon,
        bananaMissionModel, SimulationUtility.getBananaSchedulerModel()), SimulationUtility.getBananaSchedulerModel());

    //have some activity already present
    //  create a PlanInMemory, add ActivityInstances
    PlanInMemory partialPlan = new PlanInMemory();
    final var actTypeA = problem.getActivityType("GrowBanana");
    SchedulingActivityDirective act1 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie(), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act1);
    actsToBeAnchored.add(act1);

    SchedulingActivityDirective act2 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie().plus(Duration.of(5, Duration.HOURS)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act2);
    actsToBeAnchored.add(act2);

    SchedulingActivityDirective act3 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie().plus(Duration.of(10, Duration.HOURS)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act3);
    actsToBeAnchored.add(act3);


    final var actTypeB = problem.getActivityType("PickBanana");
    SchedulingActivityDirective act4 = SchedulingActivityDirective.of(actTypeB, Duration.of(0, Duration.HOURS), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, act1.id(), true);
    partialPlan.add(act4);
    templateActsAnchoring.add(act4);

    SchedulingActivityDirective act5 = SchedulingActivityDirective.of(actTypeB, Duration.of(0, Duration.HOURS), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, act2.id(), true);
    partialPlan.add(act5);
    templateActsAnchoring.add(act5);

    SchedulingActivityDirective act6 = SchedulingActivityDirective.of(actTypeB, Duration.of(0, Duration.HOURS), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, act3.id(), true);
    partialPlan.add(act6);
    templateActsAnchoring.add(act6);

    //  pass this plan as initialPlan to Problem object
    problem.setInitialPlan(partialPlan);
    //want to create another activity for each of the already present activities
    //  foreach with activityexpression
    ActivityExpression framework = new ActivityExpression.Builder()
        .ofType(actTypeA)
        .build();

    //and cut off in the middle of one of the already present activities (period ends at 18)
    CoexistenceGoal goal = new CoexistenceGoal.Builder()
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(period, true)))
        .forEach(spansOfConstraintExpression(framework))
        .thereExistsOne(new ActivityExpression.Builder()
                            .ofType(actTypeB)
                            .withArgument("quantity", SerializedValue.of(1))
                            .build())
        .startsAt(TimeAnchor.START)
        .aliasForAnchors("Grow and Pick Bananas")
        .createPersistentAnchor(createPersistentAnchor)
        .allowActivityUpdate(allowActivityUpdate)
        .withinPlanHorizon(planningHorizon)
        .build();

    problem.setGoals(List.of(goal));

    final var solver = new PrioritySolver(problem);
    var plan = solver.getNextSolution();
    for(SchedulingActivityDirective a : plan.get().getActivitiesByTime()){
      logger.debug(a.startOffset().toString() + ", " + a.duration().toString());
    }
    return new TestData(plan, actsToBeAnchored, templateActsAnchoring, templateActsNotAnchoring);
  }


  /*
Test 4-b
This case covers the scenarios with truth values for variables createPersistentAnchor,	allowActivityUpdate,	missingActAssociationsWithAnchor,	missingActAssociationsWithoutAnchor: "x x 1 0",
that is, those cases in which the goal ALLOWS to update existing activities and the plan already have activities that can be associated without modifications
In that case, the three activities PickBanana will be associated with the goal
Template activities start at the end of the "for each" directive
*/
  @Test
  public void testPlanUpdateWithAssociableActivitiesWithAnchorB() {
    TestData testData = testPlanUpdateWithAssociableActivitiesWithAnchorB(true, true);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));

    testData = testPlanUpdateWithAssociableActivitiesWithAnchorB(true, false);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));

    testData = testPlanUpdateWithAssociableActivitiesWithAnchorB(false, true);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));

    testData = testPlanUpdateWithAssociableActivitiesWithAnchorB(false, false);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  public TestData testPlanUpdateWithAssociableActivitiesWithAnchorB(boolean createPersistentAnchor, boolean allowActivityUpdate){
    ArrayList<SchedulingActivityDirective> actsToBeAnchored = new ArrayList<>();
    ArrayList<SchedulingActivityDirective> templateActsAnchoring = new ArrayList<>();
    ArrayList<SchedulingActivityDirective> templateActsNotAnchoring = new ArrayList<>();

    Interval period = Interval.betweenClosedOpen(Duration.of(0, Duration.HOURS), Duration.of(20, Duration.HOURS));

    final var bananaMissionModel = SimulationUtility.getBananaMissionModel();
    final var planningHorizon = new PlanningHorizon(TestUtility.timeFromEpochHours(0), TestUtility.timeFromEpochHours(20));
    Problem problem = new Problem(bananaMissionModel, planningHorizon, new SimulationFacade(
        planningHorizon,
        bananaMissionModel, SimulationUtility.getBananaSchedulerModel()), SimulationUtility.getBananaSchedulerModel());

    //have some activity already present
    //  create a PlanInMemory, add ActivityInstances
    PlanInMemory partialPlan = new PlanInMemory();
    final var actTypeA = problem.getActivityType("GrowBanana");
    SchedulingActivityDirective act1 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie(), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act1);
    actsToBeAnchored.add(act1);

    SchedulingActivityDirective act2 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie().plus(Duration.of(5, Duration.HOURS)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act2);
    actsToBeAnchored.add(act2);

    SchedulingActivityDirective act3 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie().plus(Duration.of(10, Duration.HOURS)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act3);
    actsToBeAnchored.add(act3);

    final var actTypeB = problem.getActivityType("PickBanana");
    SchedulingActivityDirective act4 = SchedulingActivityDirective.of(actTypeB, Duration.of(0, Duration.HOURS).plus(Duration.of(5, Duration.MINUTES)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, act1.id(), false);
    partialPlan.add(act4);
    templateActsAnchoring.add(act4);

    SchedulingActivityDirective act5 = SchedulingActivityDirective.of(actTypeB, Duration.of(0, Duration.HOURS).plus(Duration.of(5, Duration.MINUTES)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, act2.id(), false);
    partialPlan.add(act5);
    templateActsAnchoring.add(act5);

    SchedulingActivityDirective act6 = SchedulingActivityDirective.of(actTypeB, Duration.of(0, Duration.HOURS).plus(Duration.of(5, Duration.MINUTES)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, act3.id(), false);
    partialPlan.add(act6);
    templateActsAnchoring.add(act6);

    //  pass this plan as initialPlan to Problem object
    problem.setInitialPlan(partialPlan);
    //want to create another activity for each of the already present activities
    //  foreach with activityexpression
    ActivityExpression framework = new ActivityExpression.Builder()
        .ofType(actTypeA)
        .build();

    //and cut off in the middle of one of the already present activities (period ends at 18)
    CoexistenceGoal goal = new CoexistenceGoal.Builder()
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(period, true)))
        .forEach(spansOfConstraintExpression(framework))
        .thereExistsOne(new ActivityExpression.Builder()
                            .ofType(actTypeB)
                            .withArgument("quantity", SerializedValue.of(1))
                            .build())
        .startsAt(TimeExpression.offsetByAfterEnd(Duration.of(5, Duration.MINUTE)))
        .aliasForAnchors("Grow and Pick Bananas")
        .createPersistentAnchor(createPersistentAnchor)
        .allowActivityUpdate(allowActivityUpdate)
        .withinPlanHorizon(planningHorizon)
        .build();

    problem.setGoals(List.of(goal));

    final var solver = new PrioritySolver(problem);
    var plan = solver.getNextSolution();
    for(SchedulingActivityDirective a : plan.get().getActivitiesByTime()){
      logger.debug(a.startOffset().toString() + ", " + a.duration().toString());
    }
    return new TestData(plan, actsToBeAnchored, templateActsAnchoring, templateActsNotAnchoring);
  }


  /*
  Test 5-a
  This case covers the scenarios with truth values for variables createPersistentAnchor,	allowActivityUpdate,	missingActAssociationsWithAnchor,	missingActAssociationsWithoutAnchor: "x x 1 1",
  that is, those cases in which the goal ALLOWS to update existing activities and the plan has both activities that can be associated without modifications  and activities that require to update the anchorId
  In that case, only the activities already contained the anchorId will be considered
  Template activities start at the start of the "for each" directive
   */
  @Test
  public void testPlanUpdateAssociableActivitiesWithAndWithoutAnchorA(){
    TestData testData = testPlanUpdateAssociableActivitiesWithAndWithoutAnchorA(true, true);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));

    testData = testPlanUpdateAssociableActivitiesWithAndWithoutAnchorA(true, false);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));

    testData = testPlanUpdateAssociableActivitiesWithAndWithoutAnchorA(false, true);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));

    testData = testPlanUpdateAssociableActivitiesWithAndWithoutAnchorA(false, false);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  public TestData testPlanUpdateAssociableActivitiesWithAndWithoutAnchorA(boolean createPersistentAnchor, boolean allowActivityUpdate){
    ArrayList<SchedulingActivityDirective> actsToBeAnchored = new ArrayList<>();
    ArrayList<SchedulingActivityDirective> templateActsAnchoring = new ArrayList<>();
    ArrayList<SchedulingActivityDirective> templateActsNotAnchoring = new ArrayList<>();

    Interval period = Interval.betweenClosedOpen(Duration.of(0, Duration.HOURS), Duration.of(20, Duration.HOURS));

    final var bananaMissionModel = SimulationUtility.getBananaMissionModel();
    final var planningHorizon = new PlanningHorizon(TestUtility.timeFromEpochHours(0), TestUtility.timeFromEpochHours(20));
    Problem problem = new Problem(bananaMissionModel, planningHorizon, new SimulationFacade(
        planningHorizon,
        bananaMissionModel, SimulationUtility.getBananaSchedulerModel()), SimulationUtility.getBananaSchedulerModel());

    //have some activity already present
    //  create a PlanInMemory, add ActivityInstances
    PlanInMemory partialPlan = new PlanInMemory();
    final var actTypeA = problem.getActivityType("GrowBanana");
    SchedulingActivityDirective act1 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie(), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act1);
    actsToBeAnchored.add(act1);

    SchedulingActivityDirective act2 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie().plus(Duration.of(5, Duration.HOURS)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act2);
    actsToBeAnchored.add(act2);

    SchedulingActivityDirective act3 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie().plus(Duration.of(10, Duration.HOURS)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act3);
    actsToBeAnchored.add(act3);

    final var actTypeB = problem.getActivityType("PickBanana");
    // Activities with anchors
    SchedulingActivityDirective act4 = SchedulingActivityDirective.of(actTypeB, Duration.of(0, Duration.HOURS), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, act1.id(), true);
    partialPlan.add(act4);
    templateActsAnchoring.add(act4);

    SchedulingActivityDirective act5 = SchedulingActivityDirective.of(actTypeB, Duration.of(0, Duration.HOURS), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, act2.id(), true);
    partialPlan.add(act5);
    templateActsAnchoring.add(act5);

    SchedulingActivityDirective act6 = SchedulingActivityDirective.of(actTypeB, Duration.of(0, Duration.HOURS), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, act3.id(), true);
    partialPlan.add(act6);
    templateActsAnchoring.add(act6);

    // Activities without anchors
    SchedulingActivityDirective act7 = SchedulingActivityDirective.of(actTypeB, planningHorizon.getStartAerie(), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, true);
    partialPlan.add(act7);
    templateActsNotAnchoring.add(act7);

    SchedulingActivityDirective act8 = SchedulingActivityDirective.of(actTypeB, planningHorizon.getStartAerie().plus(Duration.of(5, Duration.HOURS)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, true);
    partialPlan.add(act8);
    templateActsNotAnchoring.add(act8);

    SchedulingActivityDirective act9 = SchedulingActivityDirective.of(actTypeB, planningHorizon.getStartAerie().plus(Duration.of(10, Duration.HOURS)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, true);
    partialPlan.add(act9);
    templateActsNotAnchoring.add(act9);

    //  pass this plan as initialPlan to Problem object
    problem.setInitialPlan(partialPlan);
    //want to create another activity for each of the already present activities
    //  foreach with activityexpression
    ActivityExpression framework = new ActivityExpression.Builder()
        .ofType(actTypeA)
        .build();

    //and cut off in the middle of one of the already present activities (period ends at 18)
    CoexistenceGoal goal = new CoexistenceGoal.Builder()
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(period, true)))
        .forEach(spansOfConstraintExpression(framework))
        .thereExistsOne(new ActivityExpression.Builder()
                            .ofType(actTypeB)
                            .withArgument("quantity", SerializedValue.of(1))
                            .build())
        .startsAt(TimeAnchor.START)
        .aliasForAnchors("Grow and Pick Bananas")
        .createPersistentAnchor(createPersistentAnchor)
        .allowActivityUpdate(allowActivityUpdate)
        .withinPlanHorizon(planningHorizon)
        .build();

    problem.setGoals(List.of(goal));

    final var solver = new PrioritySolver(problem);
    var plan = solver.getNextSolution();
    for(SchedulingActivityDirective a : plan.get().getActivitiesByTime()){
      logger.debug(a.startOffset().toString() + ", " + a.duration().toString());
    }
    return new TestData(plan, actsToBeAnchored, templateActsAnchoring, templateActsNotAnchoring);
  }


  /*
  Test 5-b
  This case covers the scenarios with truth values for variables createPersistentAnchor,	allowActivityUpdate,	missingActAssociationsWithAnchor,	missingActAssociationsWithoutAnchor: "x x 1 1",
  that is, those cases in which the goal ALLOWS to update existing activities and the plan has both activities that can be associated without modifications  and activities that require to update the anchorId
  In that case, only the activities already contained the anchorId will be considered
  Template activities start at the end of for each activity plus 5 minutes
   */
  @Test
  public void testPlanUpdateAssociableActivitiesWithAndWithoutAnchorB(){
    TestData testData = testPlanUpdateAssociableActivitiesWithAndWithoutAnchorB(true, true);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));

    testData = testPlanUpdateAssociableActivitiesWithAndWithoutAnchorB(true, false);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));

    testData = testPlanUpdateAssociableActivitiesWithAndWithoutAnchorB(false, true);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));

    testData = testPlanUpdateAssociableActivitiesWithAndWithoutAnchorB(false, false);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  public TestData testPlanUpdateAssociableActivitiesWithAndWithoutAnchorB(boolean createPersistentAnchor, boolean allowActivityUpdate){
    ArrayList<SchedulingActivityDirective> actsToBeAnchored = new ArrayList<>();
    ArrayList<SchedulingActivityDirective> templateActsAnchoring = new ArrayList<>();
    ArrayList<SchedulingActivityDirective> templateActsNotAnchoring = new ArrayList<>();

    Interval period = Interval.betweenClosedOpen(Duration.of(0, Duration.HOURS), Duration.of(20, Duration.HOURS));

    final var bananaMissionModel = SimulationUtility.getBananaMissionModel();
    final var planningHorizon = new PlanningHorizon(TestUtility.timeFromEpochHours(0), TestUtility.timeFromEpochHours(20));
    Problem problem = new Problem(bananaMissionModel,
                                  planningHorizon,
                                  new SimulationFacade(planningHorizon, bananaMissionModel, SimulationUtility.getBananaSchedulerModel()),
                                  SimulationUtility.getBananaSchedulerModel());
    //have some activity already present
    //  create a PlanInMemory, add ActivityInstances
    PlanInMemory partialPlan = new PlanInMemory();
    final var actTypeA = problem.getActivityType("GrowBanana");
    SchedulingActivityDirective act1 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie(), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act1);
    actsToBeAnchored.add(act1);

    SchedulingActivityDirective act2 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie().plus(Duration.of(5, Duration.HOURS)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act2);
    actsToBeAnchored.add(act2);

    SchedulingActivityDirective act3 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie().plus(Duration.of(10, Duration.HOURS)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act3);
    actsToBeAnchored.add(act3);


    final var actTypeB = problem.getActivityType("PickBanana");
    // Activities with anchors
    SchedulingActivityDirective act4 = SchedulingActivityDirective.of(actTypeB, Duration.of(0, Duration.HOURS).plus(Duration.of(5, Duration.MINUTES)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, act1.id(), false);
    partialPlan.add(act4);
    templateActsAnchoring.add(act4);

    SchedulingActivityDirective act5 = SchedulingActivityDirective.of(actTypeB, Duration.of(0, Duration.HOURS).plus(Duration.of(5, Duration.MINUTES)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, act2.id(), false);
    partialPlan.add(act5);
    templateActsAnchoring.add(act5);

    SchedulingActivityDirective act6 = SchedulingActivityDirective.of(actTypeB, Duration.of(0, Duration.HOURS).plus(Duration.of(5, Duration.MINUTES)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, act3.id(), false);
    partialPlan.add(act6);
    templateActsAnchoring.add(act6);

    // Activities without anchors
    SchedulingActivityDirective act7 = SchedulingActivityDirective.of(actTypeB, planningHorizon.getStartAerie().plus(Duration.of(0, Duration.HOURS)).plus(Duration.of(3, Duration.HOURS)).plus(Duration.of(5, Duration.MINUTES)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, false);
    partialPlan.add(act7);
    templateActsNotAnchoring.add(act7);

    SchedulingActivityDirective act8 = SchedulingActivityDirective.of(actTypeB, planningHorizon.getStartAerie().plus(Duration.of(5, Duration.HOURS)).plus(Duration.of(3, Duration.HOURS)).plus(Duration.of(5, Duration.MINUTES)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, false);
    partialPlan.add(act8);
    templateActsNotAnchoring.add(act8);

    SchedulingActivityDirective act9 = SchedulingActivityDirective.of(actTypeB, planningHorizon.getStartAerie().plus(Duration.of(10, Duration.HOURS)).plus(Duration.of(3, Duration.HOURS)).plus(Duration.of(5, Duration.MINUTES)), Duration.of(3, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1)),null, false);
    partialPlan.add(act9);
    templateActsNotAnchoring.add(act9);

    //  pass this plan as initialPlan to Problem object
    problem.setInitialPlan(partialPlan);
    //want to create another activity for each of the already present activities
    //  foreach with activityexpression
    ActivityExpression framework = new ActivityExpression.Builder()
        .ofType(actTypeA)
        .build();

    //and cut off in the middle of one of the already present activities (period ends at 18)
    CoexistenceGoal goal = new CoexistenceGoal.Builder()
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(period, true)))
        .forEach(spansOfConstraintExpression(framework))
        .thereExistsOne(new ActivityExpression.Builder()
                            .ofType(actTypeB)
                            .withArgument("quantity", SerializedValue.of(1))
                            .build())
        .startsAt(TimeExpression.offsetByAfterEnd(Duration.of(5, Duration.MINUTE)))
        .aliasForAnchors("Grow and Pick Bananas")
        .createPersistentAnchor(createPersistentAnchor)
        .allowActivityUpdate(allowActivityUpdate)
        .withinPlanHorizon(planningHorizon)
        .build();

    problem.setGoals(List.of(goal));

    final var solver = new PrioritySolver(problem);
    var plan = solver.getNextSolution();
    for(SchedulingActivityDirective a : plan.get().getActivitiesByTime()){
      logger.debug(a.startOffset().toString() + ", " + a.duration().toString());
    }
    return new TestData(plan, actsToBeAnchored, templateActsAnchoring, templateActsNotAnchoring);
  }
}
