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
  public boolean checkAnchoredActivities(TestData testData, boolean allowReuseExistingActivity, boolean missingActAssociationsWithAnchor) {
    if(testData.actsWithAnchor == null || testData.actsWithAnchor.isEmpty())
      return true;
    if(testData.plan.isEmpty())
      return false;

    Set<SchedulingActivityDirectiveId> anchorIds = testData.actsToBeAnchored.stream()
                                                                            .map(SchedulingActivityDirective::id)
                                                                            .collect(Collectors.toSet());

    Map<SchedulingActivityDirectiveId, SchedulingActivityDirective> mapIdToActivity = testData.plan.get().getActivitiesById();

    if (allowReuseExistingActivity && missingActAssociationsWithAnchor){
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

  /* Test cases in which the goal is created with TimeExpression startAt
   */
  @Test
  public void testCase0AnchorStartAt() throws SchedulingInterruptedException{

    TestData testData = createTestCaseStartsAt(false, false, false, false, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase1AnchorStartAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(false, false, false, true, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase2AnchorStartAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(false, false, true, false, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, true));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase3AnchorStartAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(false, false, true, true, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(12, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, true));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase4AnchorStartAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(false, true, false, false, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase5AnchorStartAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(false, true, false, true, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase6AnchorStartAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(false, true, true, false, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, true));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase7AnchorStartAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(false, true, true, true, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(12, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, true));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase8AnchorStartAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(true, false, false, false, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase9AnchorStartAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(true, false, false, true, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase10AnchorStartAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(true, false, true, false, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, true));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase11AnchorStartAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(true, false, true, true, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, true));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase12AnchorStartAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(true, true, false, false, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase13AnchorStartAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(true, true, false, true, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase14AnchorStartAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(true, true, true, false, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, true));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase15AnchorStartAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(true, true, true, true, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, true));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase13AnchorStartAtEnd() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(true, true, false, true, 2, 0, 20, TimeAnchor.END, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase15AnchorStartAtEnd() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(true, true, true, true, 2, 0, 20, TimeAnchor.END, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, true));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase4AnchorStartAtStartDontFit() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(false, true, false, false, 2, 3, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(5, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase13AnchorStartAtStartDontFit() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(true, true, false, true, 2, 3, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase4AnchorStartAtEndDontFit() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(false, true, false, false, 2, 0, 10, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(5, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase13AnchorStartAtEndDontFit() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(true, true, false, true, 2, 0, 10, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, false));
    assertTrue(checkUnanchoredActivities(testData));;
  }

  @Test
  public void testCase4AnchorStartAtTimeOffsetAfterEndFit() throws SchedulingInterruptedException{
    long durOffset = 1;
    TestData testData = createTestCaseStartsAt(false, true, false, false, 2, 0, 20, null, TimeExpressionRelative.offsetByAfterEnd(Duration.of(durOffset, Duration.HOUR)), durOffset);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase5AnchorStartAtTimeOffsetAfterEndFit() throws SchedulingInterruptedException{
    long durOffset = 1;
    TestData testData = createTestCaseStartsAt(false, true, false, true, 2, 0, 20, null, TimeExpressionRelative.offsetByAfterEnd(Duration.of(durOffset, Duration.HOUR)), durOffset);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase15AnchorStartAtTimeOffsetAfterEndFit() throws SchedulingInterruptedException{
    long durOffset = 1;
    TestData testData = createTestCaseStartsAt(true, true, true, true, 2, 0, 20, null, TimeExpressionRelative.offsetByAfterEnd(Duration.of(durOffset, Duration.HOUR)), durOffset);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, true));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase4AnchorStartAtTimeOffsetAfterEndDontFit() throws SchedulingInterruptedException{
    long durOffset = 9;
    TestData testData = createTestCaseStartsAt(false, true, false, false, 2, 0, 20, null, TimeExpressionRelative.offsetByAfterEnd(Duration.of(durOffset, Duration.HOUR)), durOffset);
    assertTrue(testData.plan.isPresent());
    assertEquals(5, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, false));
    assertTrue(checkUnanchoredActivities(testData));
  }



  /* Test cases in which the goal is created with TimeExpression endsAt
   */
  @Test
  public void testCase0AnchorEndAt() throws SchedulingInterruptedException{

    TestData testData = createTestCaseEndsAt(false, false, false, false, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase1AnchorEndAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseEndsAt(false, false, false, true, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase2AnchorEndAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseEndsAt(false, false, true, false, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, true));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase3AnchorEndAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseEndsAt(false, false, true, true, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(12, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, true));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase4AnchorEndAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(false, true, false, false, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase5AnchorEndAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(false, true, false, true, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase6AnchorEndAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(false, true, true, false, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, true));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase7AnchorEndAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(false, true, true, true, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(12, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, true));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase8AnchorEndAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(true, false, false, false, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase9AnchorEndAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(true, false, false, true, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase10AnchorEndAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(true, false, true, false, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, true));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase11AnchorEndAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(true, false, true, true, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, true));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase12AnchorEndAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(true, true, false, false, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase13AnchorEndAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(true, true, false, true, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase14AnchorEndAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(true, true, true, false, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, true));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase15AnchorEndAt() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(true, true, true, true, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, true));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase13AnchorEndAtEnd() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(true, true, false, true, 2, 0, 20, TimeAnchor.END, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(6, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase15AnchorEndAtEnd() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(true, true, true, true, 2, 0, 20, TimeAnchor.END, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(9, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, true, true));
    assertTrue(checkUnanchoredActivities(testData));
  }


  @Test
  public void testCase4AnchorEndAtEndDontFit() throws SchedulingInterruptedException{
    TestData testData = createTestCaseStartsAt(false, true, false, false, 2, 0, 20, TimeAnchor.START, null, 0);
    assertTrue(testData.plan.isPresent());
    assertEquals(5, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, false));
    assertTrue(checkUnanchoredActivities(testData));
  }

  @Test
  public void testCase4AnchorEndAtTimeOffsetAfterEndFit() throws SchedulingInterruptedException{
    long durOffset = 7;
    TestData testData = createTestCaseStartsAt(false, true, false, false, 2, 0, 20, null, TimeExpressionRelative.offsetByAfterEnd(Duration.of(durOffset, Duration.HOUR)), durOffset);
    assertTrue(testData.plan.isPresent());
    assertEquals(5, testData.plan.get().getActivitiesById().size());
    assertTrue(allAnchorsIncluded(testData));
    assertTrue(checkAnchoredActivities(testData, false, false));
    assertTrue(checkUnanchoredActivities(testData));
  }


  /*
  Test Case constructor
   */

  public TestData createTestCaseStartsAt(boolean allowReuseExistingActivity, boolean allowActivityUpdate, boolean missingActAssociationsWithAnchor, boolean missingActAssociationsWithoutAnchor, int activityDurationHours, int goalStartPeriodHours, int goalEndPeriodHours, TimeAnchor timeAnchor, TimeExpressionRelative timeExpression, long relativeOffsetHours)
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
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act1);
    actsToBeAnchored.add(act1);

    SchedulingActivityDirective act2 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie().plus(Duration.of(5, Duration.HOURS)), Duration.of(activityDurationHours, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act2);
    actsToBeAnchored.add(act2);

    SchedulingActivityDirective act3 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie().plus(Duration.of(10, Duration.HOURS)), Duration.of(activityDurationHours, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
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

      if (allowReuseExistingActivity && allowActivityUpdate && !missingActAssociationsWithAnchor) {
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

    templateNewActsAnchored = new ArrayList<>(plan.get().getActivities());
    templateNewActsAnchored.removeAll(partialPlan.getActivities());


    for(SchedulingActivityDirective a : plan.get().getActivitiesByTime()){
      logger.debug(a.startOffset().toString() + ", " + a.duration().toString());
    }
    return new TestData(plan, actsToBeAnchored, templateActsAlreadyAnchor, templateActsWithoutAnchorAnchored, templateActsWithoutAnchorNotAnchored, templateNewActsAnchored);
  }

  public TestData createTestCaseEndsAt(boolean allowReuseExistingActivity, boolean allowActivityUpdate, boolean missingActAssociationsWithAnchor, boolean missingActAssociationsWithoutAnchor, int activityDurationHours, int goalStartPeriodHours, int goalEndPeriodHours, TimeAnchor timeAnchor, TimeExpressionRelative timeExpression, long relativeOffsetHours)
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
    SchedulingActivityDirective act1 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie().plus(Duration.of(5, Duration.HOURS)), Duration.of(activityDurationHours, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act1);
    actsToBeAnchored.add(act1);

    SchedulingActivityDirective act2 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie().plus(Duration.of(10, Duration.HOURS)), Duration.of(activityDurationHours, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
    ), null, null, true);
    partialPlan.add(act2);
    actsToBeAnchored.add(act2);

    SchedulingActivityDirective act3 = SchedulingActivityDirective.of(actTypeA, planningHorizon.getStartAerie().plus(Duration.of(15, Duration.HOURS)), Duration.of(activityDurationHours, Duration.HOURS), Map.of(
        "quantity", SerializedValue.of(1),
        "growingDuration", SerializedValue.of(Duration.HOUR.times(3).in(Duration.HOURS))
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
      SchedulingActivityDirective act7 = SchedulingActivityDirective.of(actTypeB, planningHorizon.getStartAerie().plus(Duration.of(5, Duration.HOURS)).plus(offsetWithDuration), Duration.of(activityDurationHours, Duration.HOURS), Map.of(
          "quantity", SerializedValue.of(1)),null, true);
      partialPlan.add(act7);

      SchedulingActivityDirective act8 = SchedulingActivityDirective.of(actTypeB, planningHorizon.getStartAerie().plus(Duration.of(10, Duration.HOURS)).plus(offsetWithDuration), Duration.of(activityDurationHours, Duration.HOURS), Map.of(
          "quantity", SerializedValue.of(1)),null, true);
      partialPlan.add(act8);

      SchedulingActivityDirective act9 = SchedulingActivityDirective.of(actTypeB, planningHorizon.getStartAerie().plus(Duration.of(15, Duration.HOURS)).plus(offsetWithDuration), Duration.of(activityDurationHours, Duration.HOURS), Map.of(
          "quantity", SerializedValue.of(1)),null, true);
      partialPlan.add(act9);

      if (allowReuseExistingActivity && allowActivityUpdate && !missingActAssociationsWithAnchor) {
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
          .createPersistentAnchor(allowReuseExistingActivity)
          .allowActivityUpdate(allowActivityUpdate)
          .withinPlanHorizon(planningHorizon)
          .build();
    }
    else{
      //todo:replace pickbanana by a controllable activity
      goal = new CoexistenceGoal.Builder()
          .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(period, true)))
          .forEach(spansOfConstraintExpression(framework))
          .thereExistsOne(new ActivityExpression.Builder()
                              .ofType(actTypeB)
                              .withArgument("quantity", SerializedValue.of(1))
                              .withArgument("duration", SimulationUtility.getBananaSchedulerModel().serializeDuration(Duration.of(activityDurationHours, Duration.HOURS)))
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

    templateNewActsAnchored = new ArrayList<>(plan.get().getActivities());
    templateNewActsAnchored.removeAll(partialPlan.getActivities());


    for(SchedulingActivityDirective a : plan.get().getActivitiesByTime()){
      logger.debug(a.startOffset().toString() + ", " + a.duration().toString());
    }
    return new TestData(plan, actsToBeAnchored, templateActsAlreadyAnchor, templateActsWithoutAnchorAnchored, templateActsWithoutAnchorNotAnchored, templateNewActsAnchored);
  }
}
