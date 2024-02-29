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

public class TestPersistentAnchor {

  public record TestData(
      Optional<Plan> plan,
      ArrayList<SchedulingActivityDirective> actsToBeAnchored,
      ArrayList<SchedulingActivityDirective> actsWithAnchor,
      ArrayList<SchedulingActivityDirective> actsWithoutAnchorAnchored,
      ArrayList<SchedulingActivityDirective> actsWithoutAnchorNotAnchored,
      ArrayList<SchedulingActivityDirective> actsNewAnchored
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
  public boolean checkAnchoredActivities(TestData testData, boolean allowCreationAnchors, boolean missingActAssociationsWithAnchor) {
    if(testData.actsWithAnchor == null || testData.actsWithAnchor.isEmpty())
      return true;
    if(testData.plan.isEmpty())
      return false;

    Set<SchedulingActivityDirectiveId> anchorIds = testData.actsToBeAnchored.stream()
                                                                            .map(SchedulingActivityDirective::id)
                                                                            .collect(Collectors.toSet());

    Map<SchedulingActivityDirectiveId, SchedulingActivityDirective> mapIdToActivity = testData.plan.get().getActivitiesById();

    if (allowCreationAnchors || missingActAssociationsWithAnchor){
      for (SchedulingActivityDirective act: testData.actsWithAnchor){
        SchedulingActivityDirective directive = mapIdToActivity.get(act.id());
        if (directive.anchorId() == null || !anchorIds.contains(directive.anchorId()))
          return false;
        anchorIds.remove(directive.anchorId());
      }
    }

    for (SchedulingActivityDirective act: testData.actsNewAnchored){
      SchedulingActivityDirective directive = mapIdToActivity.get(act.id());
      if (directive.anchorId() == null || !anchorIds.contains(directive.anchorId()))
        return false;
      anchorIds.remove(directive.anchorId());
    }
    return anchorIds.isEmpty();
  }


  /**
   * Test that all directives added manually to the plan in the test (not by the scheduler)
   * that cannot be anchored (e.g. because it is not allowed to modify them) don't actually get an anchor
   * @param testData
   * @return
   */
  public boolean checkUnanchoredActivities(TestData testData) {
    if(testData.actsWithoutAnchorNotAnchored == null || testData.actsWithoutAnchorNotAnchored.isEmpty())
      return true;
    if(testData.plan.isEmpty())
      return false;

    Map<SchedulingActivityDirectiveId, SchedulingActivityDirective> mapIdToActivity = testData.plan.get().getActivitiesById();
    for (SchedulingActivityDirective act: testData.actsWithoutAnchorNotAnchored){
      SchedulingActivityDirective directive = mapIdToActivity.get(act.id());
      if (directive.anchorId() != null)
        return false;
    }
    return true;
  }


  /*
  Test cases for temporal relation StartsAt Start, with anchor at start and end
   */

  // Anchor Disabled
  @Test
  public void testCaseStartAtStartDisable00() throws SchedulingInterruptedException{

    TestData testData = createTestCaseStartsAt(PersistentTimeAnchor.DISABLED, false, false, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCaseStartAtStartAnchorAtDisable01() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(PersistentTimeAnchor.DISABLED, false, true, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCaseStartAtStartAnchorAtDisable10() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(PersistentTimeAnchor.DISABLED, true, false, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, true));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCaseStartAtStartAnchorAtDisable11() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(PersistentTimeAnchor.DISABLED, true, true, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, true));
    assertTrue(checkUnanchoredActivities(testData));
  }

  // Anchor at START
  @Test
  public void testCaseStartAtStartAnchorAtStart00() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(PersistentTimeAnchor.START, false, false, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCaseStartAtStartAnchorAtStart01() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(PersistentTimeAnchor.START, false, true, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCaseStartAtStartAnchorAtStart10() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(PersistentTimeAnchor.START, true, false, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, true));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCaseStartAtStartAnchorAtStart11() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(PersistentTimeAnchor.START, true, true, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, true));
    assertTrue(checkUnanchoredActivities(testData));
  }

  //Anchor at END
  @Test
  public void testCaseStartAtStartAnchorAtEnd00() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(PersistentTimeAnchor.END, false, false, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCaseStartAtStartAnchorAtEnd01() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(PersistentTimeAnchor.END, false, true, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCaseStartAtStartAnchorAtEnd10() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(PersistentTimeAnchor.END, true, false, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, true));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCaseStartAtStartAnchorAtEnd11() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(PersistentTimeAnchor.END, true, true, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, true));
    assertTrue(checkUnanchoredActivities(testData));
  }


  /*
  Test cases for temporal relation StartsAt End, with anchor at start and end
   */

  // Anchor Disabled
  @Test
  public void testCaseStartAtEndAnchorAtDisable00() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(PersistentTimeAnchor.DISABLED, false, false, 2, 0, 20, TimeAnchor.END, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCaseStartAtDisable01() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(PersistentTimeAnchor.DISABLED, false, true, 2, 0, 20, TimeAnchor.END, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCaseStartAtDisable10() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(PersistentTimeAnchor.DISABLED, true, false, 2, 0, 20, TimeAnchor.END, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, true));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCaseStartAtDisable11() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(PersistentTimeAnchor.DISABLED, true, true, 2, 0, 20, TimeAnchor.END, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, true));
    assertTrue(checkUnanchoredActivities(testData));
  }

  // Anchor to START
  @Test
  public void testCaseStartAtStartAnchorAt00() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(PersistentTimeAnchor.START, false, false, 2, 0, 20, TimeAnchor.END, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCaseStartAtStartAnchorAt01() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(PersistentTimeAnchor.START, false, true, 2, 0, 20, TimeAnchor.END, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCaseStartAtStartAnchorAt10() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(PersistentTimeAnchor.START, true, false, 2, 0, 20, TimeAnchor.END, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, true));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCaseStartAtStartAnchorAt11() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(PersistentTimeAnchor.START, true, true, 2, 0, 20, TimeAnchor.END, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, true));
    assertTrue(checkUnanchoredActivities(testData));
  }

  //Anchor at END
  @Test
  public void testCaseStartAtEndAnchorAt00() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(PersistentTimeAnchor.END, false, false, 2, 0, 20, TimeAnchor.END, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCaseStartAtEndAnchorAt01() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(PersistentTimeAnchor.END, false, true, 2, 0, 20, TimeAnchor.END, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCaseStartAtEndAnchorAt10() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(PersistentTimeAnchor.END, true, false, 2, 0, 20, TimeAnchor.END, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, true));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCaseStartAtEndAnchorAt11() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(PersistentTimeAnchor.END, true, true, 2, 0, 20, TimeAnchor.END, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, true));
    assertTrue(checkUnanchoredActivities(testData));
  }







/*

  @Test
  public void testCase4AnchorStartAtTimeOffsetAfterEndFit() throws SchedulingInterruptedException{
    long durOffset = 1;
    TestData testData = createTestCaseStartsAt(PersistentTimeAnchor.END, false, false, 2, 0, 20, null, TimeExpressionRelative.offsetByAfterEnd(Duration.of(durOffset, Duration.HOUR)), durOffset);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase5AnchorStartAtTimeOffsetAfterEndFit() throws SchedulingInterruptedException{
    long durOffset = 1;
    TestData testData = createTestCaseStartsAt(true, false, true, 2, 0, 20, null, TimeExpressionRelative.offsetByAfterEnd(Duration.of(durOffset, Duration.HOUR)), durOffset);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase7AnchorStartAtTimeOffsetAfterEndFit() throws SchedulingInterruptedException{
    long durOffset = 1;
    TestData testData = createTestCaseStartsAt(true, true, true, 2, 0, 20, null, TimeExpressionRelative.offsetByAfterEnd(Duration.of(durOffset, Duration.HOUR)), durOffset);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, true));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase4AnchorStartAtTimeOffsetAfterEndDontFit() throws SchedulingInterruptedException{
    long durOffset = 9;
    TestData testData = createTestCaseStartsAt(true, false, false, 2, 0, 20, null, TimeExpressionRelative.offsetByAfterEnd(Duration.of(durOffset, Duration.HOUR)), durOffset);
    assertTrue(testData.plan.isPresent());
    assertEquals(5, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, false));
    assertTrue(checkUnanchoredActivities(testData));
  }*/

  /* Test cases in which the goal is created with TimeExpression endsAt
   */

  public TestData createTestCaseStartsAt(PersistentTimeAnchor createPersistentAnchor,  boolean missingActAssociationsWithAnchor, boolean missingActAssociationsWithoutAnchor, int activityDurationHours, int goalStartPeriodHours, int goalEndPeriodHours, TimeAnchor timeAnchor, TimeExpressionRelative timeExpression, long relativeOffsetHours)
  throws SchedulingInterruptedException
  {
    ArrayList<SchedulingActivityDirective> actsToBeAnchored = new ArrayList<>();
    ArrayList<SchedulingActivityDirective> templateActsAlreadyAnchor = new ArrayList<>();
    ArrayList<SchedulingActivityDirective> templateActsWithoutAnchorAnchored = new ArrayList<>();
    ArrayList<SchedulingActivityDirective> templateActsWithoutAnchorNotAnchored = new ArrayList<>();
    ArrayList<SchedulingActivityDirective> templateNewActsAnchored;

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
    SchedulingActivityDirective act1 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie(), Duration.of(activityDurationHours, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(activityDurationHours).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act1);
    actsToBeAnchored.add(act1);

    SchedulingActivityDirective act2 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie().plus(Duration.of(5, Duration.HOURS)), Duration.of(activityDurationHours, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(activityDurationHours).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act2);
    actsToBeAnchored.add(act2);

    SchedulingActivityDirective act3 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie().plus(Duration.of(10, Duration.HOURS)), Duration.of(activityDurationHours, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(activityDurationHours).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act3);
    actsToBeAnchored.add(act3);

    final var actTypeB = problem.getActivityType("PickBanana");

    boolean anchoredToStart;
    if(timeAnchor != null)
      anchoredToStart = timeAnchor.equals(TimeAnchor.START);
    else
      anchoredToStart = timeExpression.getAnchor().equals(TimeAnchor.START);

    // Nominal anchor is right at the start or end
    Duration relativeOffset = Duration.of(relativeOffsetHours, Duration.HOURS);
    Duration offsetWithDuration = Duration.of(0, Duration.HOURS);
    // If TimeAnchor or TimeExpressionRelative is related to the end of Goal directive, then need to add goal directive duration
    if(!anchoredToStart){
      offsetWithDuration = offsetWithDuration.plus(activityDurationHours, Duration.HOURS);
    }
    // If in addition the goal uses an offset, it needs to be added
    if(timeAnchor == null){
      offsetWithDuration = offsetWithDuration.plus(relativeOffsetHours, Duration.HOURS);
    }

    if(missingActAssociationsWithAnchor){
      // Activities with anchors
      SchedulingActivityDirective act4 = SchedulingActivityDirective.of(actTypeB, relativeOffset, Duration.of(activityDurationHours, Duration.HOURS), Map.of(
          "quantity", SerializedValue.of(1)),null, act1.id(), anchoredToStart);
      partialPlan.add(act4);
      templateActsAlreadyAnchor.add(act4);

      SchedulingActivityDirective act5 = SchedulingActivityDirective.of(actTypeB, relativeOffset, Duration.of(activityDurationHours, Duration.HOURS), Map.of(
          "quantity", SerializedValue.of(1)),null, act2.id(), anchoredToStart);
      partialPlan.add(act5);
      templateActsAlreadyAnchor.add(act5);

      SchedulingActivityDirective act6 = SchedulingActivityDirective.of(actTypeB, relativeOffset, Duration.of(activityDurationHours, Duration.HOURS), Map.of(
          "quantity", SerializedValue.of(1)),null, act3.id(), anchoredToStart);
      partialPlan.add(act6);
      templateActsAlreadyAnchor.add(act6);
    }

    if(missingActAssociationsWithoutAnchor){
      // Activities without anchors
      SchedulingActivityDirective act7 = SchedulingActivityDirective.of(actTypeB, planningHorizon.getStartAerie().plus(offsetWithDuration), Duration.of(activityDurationHours, Duration.HOURS), Map.of(
          "quantity", SerializedValue.of(1)),null, true);
      partialPlan.add(act7);

      SchedulingActivityDirective act8 = SchedulingActivityDirective.of(actTypeB, planningHorizon.getStartAerie().plus(Duration.of(5, Duration.HOURS)).plus(offsetWithDuration), Duration.of(activityDurationHours, Duration.HOURS), Map.of(
          "quantity", SerializedValue.of(1)),null, true);
      partialPlan.add(act8);

      SchedulingActivityDirective act9 = SchedulingActivityDirective.of(actTypeB, planningHorizon.getStartAerie().plus(Duration.of(10, Duration.HOURS)).plus(offsetWithDuration), Duration.of(activityDurationHours, Duration.HOURS), Map.of(
          "quantity", SerializedValue.of(1)),null, true);
      partialPlan.add(act9);

      if (!createPersistentAnchor.equals(PersistentTimeAnchor.DISABLED) && !missingActAssociationsWithAnchor) {
        templateActsWithoutAnchorAnchored.add(act7);
        templateActsWithoutAnchorAnchored.add(act8);
        templateActsWithoutAnchorAnchored.add(act9);
      }
      else{
        templateActsWithoutAnchorNotAnchored.add(act7);
        templateActsWithoutAnchorNotAnchored.add(act8);
        templateActsWithoutAnchorNotAnchored.add(act9);
      }
    }

    //  pass this plan as initialPlan to Problem object
    problem.setInitialPlan(partialPlan);
    //want to create another activity for each of the already present activities
    //  foreach with activityexpression
    ActivityExpression framework = new ActivityExpression.Builder()
        .ofType(actTypeA)
        .build();

    Interval period = Interval.betweenClosedOpen(Duration.of(goalStartPeriodHours, Duration.HOURS), Duration.of(goalEndPeriodHours, Duration.HOURS));
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
          .createPersistentAnchor(createPersistentAnchor)
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
          .createPersistentAnchor(createPersistentAnchor)
          .withinPlanHorizon(planningHorizon)
          .build();
    }
    problem.setGoals(List.of(goal));

    final var solver = new PrioritySolver(problem);
    var plan = solver.getNextSolution();

    templateNewActsAnchored = new ArrayList<>(plan.get().getActivities());
    templateNewActsAnchored.removeAll(partialPlan.getActivities());


    for(SchedulingActivityDirective a : plan.get().getActivitiesByTime()){
      logger.debug(a.startOffset().toString() + ", " + a.duration().toString());
    }
    return new TestData(plan, actsToBeAnchored, templateActsAlreadyAnchor, templateActsWithoutAnchorAnchored, templateActsWithoutAnchorNotAnchored, templateNewActsAnchored);
  }
}
