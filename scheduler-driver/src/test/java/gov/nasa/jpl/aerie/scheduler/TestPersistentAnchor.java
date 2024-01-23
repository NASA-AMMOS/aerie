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
import gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions.TimeExpressionRelative;
import gov.nasa.jpl.aerie.scheduler.goals.CoexistenceGoal;
import gov.nasa.jpl.aerie.scheduler.model.*;
import gov.nasa.jpl.aerie.scheduler.simulation.SimulationFacade;
import gov.nasa.jpl.aerie.scheduler.solver.PrioritySolver;
import org.apache.commons.lang3.function.TriFunction;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
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

  @Test
  public void testCase0() throws SchedulingInterruptedException{
    TestData testData = createTestCase(false, false, false, false, 0, 20, TimeAnchor.START, null, true, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  @Test
  public void testCase1() throws SchedulingInterruptedException{
    TestData testData = createTestCase(false, false, false, true, 0, 20, TimeAnchor.START, null, true, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  @Test
  public void testCase2() throws SchedulingInterruptedException{
    TestData testData = createTestCase(false, false, true, false, 0, 20, TimeAnchor.START, null, true, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  @Test
  public void testCase3() throws SchedulingInterruptedException{
    TestData testData = createTestCase(false, false, true, true, 0, 20, TimeAnchor.START, null, true, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(12, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  @Test
  public void testCase4() throws SchedulingInterruptedException{
    TestData testData = createTestCase(false, true, false, false, 0, 20, TimeAnchor.START, null, true, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  @Test
  public void testCase5() throws SchedulingInterruptedException{
    TestData testData = createTestCase(false, true, false, true, 0, 20, TimeAnchor.START, null, true, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  @Test
  public void testCase6() throws SchedulingInterruptedException{
    TestData testData = createTestCase(false, true, true, false, 0, 20, TimeAnchor.START, null, true, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  @Test
  public void testCase7() throws SchedulingInterruptedException{
    TestData testData = createTestCase(false, true, true, true, 0, 20, TimeAnchor.START, null, true, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(12, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  @Test
  public void testCase8() throws SchedulingInterruptedException{
    TestData testData = createTestCase(true, false, false, false, 0, 20, TimeAnchor.START, null, true, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  @Test
  public void testCase9() throws SchedulingInterruptedException{
    TestData testData = createTestCase(true, false, false, true, 0, 20, TimeAnchor.START, null, true, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  @Test
  public void testCase10() throws SchedulingInterruptedException{
    TestData testData = createTestCase(true, false, true, false, 0, 20, TimeAnchor.START, null, true, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  @Test
  public void testCase11() throws SchedulingInterruptedException{
    TestData testData = createTestCase(true, false, true, true, 0, 20, TimeAnchor.START, null, true, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  @Test
  public void testCase12() throws SchedulingInterruptedException{
    TestData testData = createTestCase(true, true, false, false, 0, 20, TimeAnchor.START, null, true, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  @Test
  public void testCase13() throws SchedulingInterruptedException{
    TestData testData = createTestCase(true, true, false, true, 0, 20, TimeAnchor.START, null, true, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  @Test
  public void testCase14() throws SchedulingInterruptedException{
    TestData testData = createTestCase(true, true, true, false, 0, 20, TimeAnchor.START, null, true, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  @Test
  public void testCase15() throws SchedulingInterruptedException{
    TestData testData = createTestCase(false, true, true, true, 0, 20, TimeAnchor.START, null, true, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  @Test
  public void testCase13TimeAnchorEnd() throws SchedulingInterruptedException{
    TestData testData = createTestCase(true, true, false, true, 0, 20, TimeAnchor.END, null, false, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  @Test
  public void testCase15TimeAnchorEnd() throws SchedulingInterruptedException{
    TestData testData = createTestCase(true, true, true, true, 0, 20, TimeAnchor.END, null, false, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  @Test
  public void testCase4DontFitStart() throws SchedulingInterruptedException{
    TestData testData = createTestCase(false, true, false, false, 1, 20, TimeAnchor.START, null, true, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(5, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  @Test
  public void testCase13DontFitStart() throws SchedulingInterruptedException{
    TestData testData = createTestCase(true, true, false, true, 1, 20, TimeAnchor.START, null, true, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  @Test
  public void testCase4DontFitEnd() throws SchedulingInterruptedException{
    TestData testData = createTestCase(false, true, false, false, 0, 14, TimeAnchor.START, null, true, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(5, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  @Test
  public void testCase13DontFitEnd() throws SchedulingInterruptedException{
    TestData testData = createTestCase(true, true, false, true, 0, 14, TimeAnchor.START, null, true, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  @Test
  public void testCase4TimeOffsetAfterEndFit() throws SchedulingInterruptedException{
    long dur = 2;
    TestData testData = createTestCase(false, true, false, false, 0, 22, null, TimeExpressionRelative.offsetByAfterEnd(Duration.of(dur, Duration.HOUR)), false, dur);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  @Test
  public void testCase5TimeOffsetAfterEndFit() throws SchedulingInterruptedException{
    long dur = 2;
    TestData testData = createTestCase(false, true, false, true, 0, 22, null, TimeExpressionRelative.offsetByAfterEnd(Duration.of(dur, Duration.HOUR)), false, dur);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  @Test
  public void testCase15TimeOffsetAfterEndFit() throws SchedulingInterruptedException{
    long dur = 2;
    TestData testData = createTestCase(true, true, true, true, 0, 22, null, TimeExpressionRelative.offsetByAfterEnd(Duration.of(dur, Duration.HOUR)), false, dur);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  @Test
  public void testCase4TimeOffsetAfterEndDontFit() throws SchedulingInterruptedException{
    long dur = 3;
    TestData testData = createTestCase(false, true, false, false, 0, 22, null, TimeExpressionRelative.offsetByAfterEnd(Duration.of(dur, Duration.HOUR)), false, dur);
    assertTrue(testData.plan.isPresent());
    assertEquals(5, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  @Test
  public void testCase5TimeOffsetAfterEndDontFit() throws SchedulingInterruptedException{
    long dur = 3;
    TestData testData = createTestCase(false, true, false, true, 0, 22, null, TimeExpressionRelative.offsetByAfterEnd(Duration.of(dur, Duration.HOUR)), false, dur);
    assertTrue(testData.plan.isPresent());
    assertEquals(8, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }

  @Test
  public void testCase15TimeOffsetAfterEndDontFit() throws SchedulingInterruptedException{
    long dur = 3;
    TestData testData = createTestCase(true, true, true, true, 0, 22, null, TimeExpressionRelative.offsetByAfterEnd(Duration.of(dur, Duration.HOUR)), false, dur);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(allAnchoringActivitiesAnchored(testData));
    assertTrue(allNonAnchoringActivitiesAreNotAnchored(testData));
  }


  public TestData createTestCase(boolean allowReuseExistingActivity, boolean allowActivityUpdate, boolean missingActAssociationsWithAnchor, boolean missingActAssociationsWithoutAnchor, int startPeriodHours, int endPeriodHours, TimeAnchor timeAnchor, TimeExpressionRelative timeExpression, boolean anchoredToStart, long offset)
  throws SchedulingInterruptedException
  {
    ArrayList<SchedulingActivityDirective> actsToBeAnchored = new ArrayList<>();
    ArrayList<SchedulingActivityDirective> templateActsAnchoring = new ArrayList<>();
    ArrayList<SchedulingActivityDirective> templateActsNotAnchoring = new ArrayList<>();

    final var bananaMissionModel = SimulationUtility.getBananaMissionModel();
    final var planningHorizon = new PlanningHorizon(TestUtility.timeFromEpochHours(0), TestUtility.timeFromEpochHours(20));

    final var simulationFacade = new SimulationFacade(
        planningHorizon,
        bananaMissionModel,
        SimulationUtility.getBananaSchedulerModel(),
        () -> false);
    final var problem = new Problem(
        bananaMissionModel,
        planningHorizon,
        simulationFacade,
        SimulationUtility.getBananaSchedulerModel()
    );

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

    // Nominal anchor is right at the start or end
    Duration dur = Duration.of(0, Duration.HOURS);
    // If TimeAnchor or TimeExpressionRelative is related to the end of Goal directive, then need to add goal directive duration, which is always 5
    if(!anchoredToStart){
      dur = dur.plus(5, Duration.HOURS);
    }
    // If in addition the goal uses an offset, it needs to be added
    if(timeAnchor == null){
      dur = dur.plus(offset, Duration.HOURS);
    }

    if(missingActAssociationsWithAnchor){
      // Activities with anchors
      SchedulingActivityDirective act4 = SchedulingActivityDirective.of(actTypeB, dur, Duration.of(5, Duration.HOURS), Map.of(
          "quantity", SerializedValue.of(1)),null, act1.id(), anchoredToStart);
      partialPlan.add(act4);
      templateActsAnchoring.add(act4);

      SchedulingActivityDirective act5 = SchedulingActivityDirective.of(actTypeB, dur, Duration.of(5, Duration.HOURS), Map.of(
          "quantity", SerializedValue.of(1)),null, act2.id(), anchoredToStart);
      partialPlan.add(act5);
      templateActsAnchoring.add(act5);

      SchedulingActivityDirective act6 = SchedulingActivityDirective.of(actTypeB, dur, Duration.of(5, Duration.HOURS), Map.of(
          "quantity", SerializedValue.of(1)),null, act3.id(), anchoredToStart);
      partialPlan.add(act6);
      templateActsAnchoring.add(act6);
    }

    if(missingActAssociationsWithoutAnchor){
      // Activities without anchors
      SchedulingActivityDirective act7 = SchedulingActivityDirective.of(actTypeB, planningHorizon.getStartAerie().plus(dur), Duration.of(5, Duration.HOURS), Map.of(
          "quantity", SerializedValue.of(1)),null, true);
      partialPlan.add(act7);
      templateActsNotAnchoring.add(act7);

      SchedulingActivityDirective act8 = SchedulingActivityDirective.of(actTypeB, planningHorizon.getStartAerie().plus(Duration.of(5, Duration.HOURS)).plus(dur), Duration.of(5, Duration.HOURS), Map.of(
          "quantity", SerializedValue.of(1)),null, true);
      partialPlan.add(act8);
      templateActsNotAnchoring.add(act8);

      SchedulingActivityDirective act9 = SchedulingActivityDirective.of(actTypeB, planningHorizon.getStartAerie().plus(Duration.of(10, Duration.HOURS)).plus(dur), Duration.of(5, Duration.HOURS), Map.of(
          "quantity", SerializedValue.of(1)),null, true);
      partialPlan.add(act9);
      templateActsNotAnchoring.add(act9);
    }

    //  pass this plan as initialPlan to Problem object
    problem.setInitialPlan(partialPlan);
    //want to create another activity for each of the already present activities
    //  foreach with activityexpression
    ActivityExpression framework = new ActivityExpression.Builder()
        .ofType(actTypeA)
        .build();

    Interval period = Interval.betweenClosedOpen(Duration.of(startPeriodHours, Duration.HOURS), Duration.of(endPeriodHours, Duration.HOURS));
    CoexistenceGoal goal;
    if(timeAnchor != null){
      goal = new CoexistenceGoal.Builder()
          .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(period, true)))
          .forEach(spansOfConstraintExpression(framework))
          .thereExistsOne(new ActivityExpression.Builder()
                              .ofType(actTypeB)
                              .withArgument("quantity", SerializedValue.of(1))
                              .build())
          .startsAt(timeAnchor)
          .aliasForAnchors("Grow and Pick Bananas")
          .createPersistentAnchor(allowReuseExistingActivity)
          .allowActivityUpdate(allowActivityUpdate)
          .withinPlanHorizon(planningHorizon)
          .build();
    }
    else{
      goal = new CoexistenceGoal.Builder()
          .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(period, true)))
          .forEach(spansOfConstraintExpression(framework))
          .thereExistsOne(new ActivityExpression.Builder()
                              .ofType(actTypeB)
                              .withArgument("quantity", SerializedValue.of(1))
                              .build())
          .startsAt(timeExpression)
          .aliasForAnchors("Grow and Pick Bananas")
          .createPersistentAnchor(allowReuseExistingActivity)
          .allowActivityUpdate(allowActivityUpdate)
          .withinPlanHorizon(planningHorizon)
          .build();
    }
    problem.setGoals(List.of(goal));

    final var solver = new PrioritySolver(problem);
    var plan = solver.getNextSolution();
    for(SchedulingActivityDirective a : plan.get().getActivitiesByTime()){
      logger.debug(a.startOffset().toString() + ", " + a.duration().toString());
    }
    return new TestData(plan, actsToBeAnchored, templateActsAnchoring, templateActsNotAnchoring);
  }
}
