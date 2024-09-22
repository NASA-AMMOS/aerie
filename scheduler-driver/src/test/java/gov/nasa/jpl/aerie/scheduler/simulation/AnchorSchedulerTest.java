package gov.nasa.jpl.aerie.scheduler.simulation;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.driver.DirectiveTypeRegistry;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.OneStepTask;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivityId;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResultsInterface;
import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.DirectiveType;
import gov.nasa.jpl.aerie.merlin.protocol.model.InputType;
import gov.nasa.jpl.aerie.merlin.protocol.model.ModelType;
import gov.nasa.jpl.aerie.merlin.protocol.model.OutputType;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InSpan;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.scheduler.SchedulingInterruptedException;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AnchorSchedulerTest {
  private final Duration tenDays = Duration.duration(10 * 60 * 60 * 24, Duration.SECONDS);
  private final static Duration oneMinute = Duration.of(60, Duration.SECONDS);
  private final Map<String, SerializedValue> arguments = Map.of("unusedArg", SerializedValue.of("test-param"));

  @Nested
  public final class AnchorsSimulationDriverTests {
    private final SerializedActivity serializedDelayDirective = new SerializedActivity("DelayActivityDirective", arguments);
    private final SerializedActivity serializedDecompositionDirective = new SerializedActivity("DecomposingActivityDirective", arguments);
    private final SerializedValue computedAttributes = new SerializedValue.MapValue(Map.of());
    private final Instant planStart = Instant.EPOCH;


    public SimulationResultsInterface simulateActivities(final Map<ActivityDirectiveId, ActivityDirective> schedule){
      //note that vanilla SimDriver currently has no way to stop after all acts (need CheckpointSimDriver for that)
      return SimulationDriver.simulate(
          AnchorTestModel,
          schedule,
          planStart,
          tenDays,
          planStart,
          tenDays,
          ()->false);
    }

    /**
     * Asserts equality based on the following fields of SimulationResults:
     *  - startTime
     *  - simulatedActivities
     *  - unfinishedActivities (asserted to be empty in actual)
     *  - topics
     *  Any resource profiles and events are not checked.
     *  <p>
     *  the duration span of the results themselves are not checked since eg only CheckpointSimDriver can currently stop
     *  when all activities are finished. do note that the simulated activity durations are checked though.
     */
    private static void assertEqualsSimulationResults(SimulationResultsInterface expected, SimulationResultsInterface actual){
      assertEquals(expected.getStartTime(), actual.getStartTime());
      //do not require that results objects have the same duration since only CheckpointSimDriver accepts stop criteria
      assertEquals(expected.getSimulatedActivities().entrySet().size(), actual.getSimulatedActivities().size());
      for(final var entry : expected.getSimulatedActivities().entrySet()){
        final var key = entry.getKey();
        final var expectedValue = entry.getValue();
        final var actualValue = actual.getSimulatedActivities().get(key);
        assertNotNull(actualValue);
        assertEquals(expectedValue, actualValue);
      }
      assertTrue(actual.getUnfinishedActivities().isEmpty());
      assertEquals(expected.getTopics().size(), actual.getTopics().size());
      for(int i = 0; i < expected.getTopics().size(); ++i){
        assertEquals(expected.getTopics().get(i), actual.getTopics().get(i));
      }
    }

    private void constructFullComplete5AryTree(int maxLevel, int currentLevel, long parentNode, Map<ActivityDirectiveId, ActivityDirective> activitiesToSimulate, Map<SimulatedActivityId, SimulatedActivity> simulatedActivities){
      if(currentLevel > maxLevel) return;
      for(int i = 1; i <= 5; i++) {
        long curElement = parentNode*5+i;
        activitiesToSimulate.put(
            new ActivityDirectiveId(curElement),
            new ActivityDirective(Duration.ZERO, serializedDelayDirective, new ActivityDirectiveId(parentNode), false));
        simulatedActivities.put(
            new SimulatedActivityId(curElement),
            new SimulatedActivity(
                serializedDelayDirective.getTypeName(),
                Map.of(),
                Instant.EPOCH.plus(currentLevel, ChronoUnit.MINUTES),
                oneMinute,
                null,
                List.of(),
                Optional.of(new ActivityDirectiveId(curElement)), computedAttributes));
        constructFullComplete5AryTree(maxLevel, currentLevel+1, curElement, activitiesToSimulate, simulatedActivities);
      }
    }

    private static void assertEqualsAsideFromChildren(SimulatedActivity expected, SimulatedActivity actual){
      assertEquals(expected.type(), actual.type());
      assertEquals(expected.arguments(), actual.arguments());
      assertEquals(expected.start(), actual.start());
      assertEquals(expected.duration(), actual.duration());
      assertEquals(expected.parentId(), actual.parentId());
      assertEquals(expected.directiveId(), actual.directiveId());
      assertEquals(expected.computedAttributes(), actual.computedAttributes());
    }

    @Test
    @DisplayName("Activities depending on no activities simulate at the correct time")
    public void activitiesAnchoredToPlan() throws SchedulingInterruptedException {
      final var minusOneMinute = Duration.of(-60, Duration.SECONDS);
      final var resolveToPlanStartAnchors = new HashMap<ActivityDirectiveId, ActivityDirective>(415);
      final Map<SimulatedActivityId, SimulatedActivity> simulatedActivities = new HashMap<>(415);

      // Anchored to Plan Start (only positive is allowed)
      for (long l = 0; l < 5; l++) {
        final var activityDirectiveId = new ActivityDirectiveId(l);
        resolveToPlanStartAnchors.put(
            activityDirectiveId,
            new ActivityDirective(Duration.of(l, Duration.SECONDS), serializedDelayDirective, null, true));
        simulatedActivities.put(new SimulatedActivityId(l), new SimulatedActivity(
            serializedDelayDirective.getTypeName(),
            Map.of(),
            Instant.EPOCH.plus(l, ChronoUnit.SECONDS),
            oneMinute,
            null,
            List.of(),
            Optional.of(activityDirectiveId),
            computedAttributes
        ));
      }
      // Anchored to Plan End (only negative will be simulated)
      for (long l = 10; l < 15; l++) {
        final var activityDirectiveId = new ActivityDirectiveId(l);
        resolveToPlanStartAnchors.put(
            activityDirectiveId,
            new ActivityDirective(Duration.of(-l, Duration.MINUTES), serializedDelayDirective, null, false)); // Minutes so they finish by simulation end
        simulatedActivities.put(new SimulatedActivityId(l), new SimulatedActivity(
            serializedDelayDirective.getTypeName(),
            Map.of(),
            Instant.EPOCH.plus(10, ChronoUnit.DAYS).minus(l, ChronoUnit.MINUTES),
            oneMinute,
            null,
            List.of(),
            Optional.of(activityDirectiveId),
            computedAttributes
        ));
      }

      // Chained to plan start
      resolveToPlanStartAnchors.put(
          new ActivityDirectiveId(15),
          new ActivityDirective(Duration.ZERO, serializedDelayDirective, new ActivityDirectiveId(0), true));
      simulatedActivities.put(new SimulatedActivityId(15), new SimulatedActivity(
          serializedDelayDirective.getTypeName(),
          Map.of(),
          Instant.EPOCH,
          oneMinute,
          null,
          List.of(),
          Optional.of(new ActivityDirectiveId(15)),
          computedAttributes
      ));

      for (long l = 16; l < 415; l++) {
        final var activityDirectiveId = new ActivityDirectiveId(l);
        if ((l & 1) == 0) { // If even
          resolveToPlanStartAnchors.put(
              activityDirectiveId,
              new ActivityDirective(oneMinute, serializedDelayDirective, new ActivityDirectiveId(l - 1), true));
          simulatedActivities.put(new SimulatedActivityId(l), new SimulatedActivity(
              serializedDelayDirective.getTypeName(),
              Map.of(),
              Instant.EPOCH.plus(1, ChronoUnit.MINUTES),
              oneMinute,
              null,
              List.of(),
              Optional.of(activityDirectiveId),
              computedAttributes
          ));
        } else {
          resolveToPlanStartAnchors.put(
              activityDirectiveId,
              new ActivityDirective(minusOneMinute, serializedDelayDirective, new ActivityDirectiveId(l - 1), true));
          simulatedActivities.put(new SimulatedActivityId(l), new SimulatedActivity(
              serializedDelayDirective.getTypeName(),
              Map.of(),
              Instant.EPOCH,
              oneMinute,
              null,
              List.of(),
              Optional.of(activityDirectiveId),
              computedAttributes
          ));
        }
      }

      // Assert Simulation results
      final var expectedSimResults = new SimulationResults(
          Map.of(), //real
          Map.of(), //discrete
          simulatedActivities,
          Map.of(), //unfinished
          planStart,
          tenDays.minus(Duration.of(9, Duration.MINUTE)), // simulation duration
          modelTopicList,
          new TreeMap<>() //events
      );

      final var actualSimResults = simulateActivities(resolveToPlanStartAnchors);

      assertEqualsSimulationResults(expectedSimResults, actualSimResults);
    }

    @Test
    @DisplayName("Activities depending on another activities simulate at the correct time")
    public void activitiesAnchoredToOtherActivities() throws SchedulingInterruptedException {
      final var allEndTimeAnchors = new HashMap<ActivityDirectiveId, ActivityDirective>(400);
      final var endTimeAnchorEveryFifth = new HashMap<ActivityDirectiveId, ActivityDirective>(400);
      final Map<SimulatedActivityId, SimulatedActivity> simulatedActivities = new HashMap<>(800);
      final var activitiesToSimulate = new HashMap<ActivityDirectiveId, ActivityDirective>(800);

      allEndTimeAnchors.put(
          new ActivityDirectiveId(0),
          new ActivityDirective(oneMinute, serializedDelayDirective, null, true));
      endTimeAnchorEveryFifth.put(
          new ActivityDirectiveId(400),
          new ActivityDirective(oneMinute, serializedDelayDirective, null, true));
      simulatedActivities.put(
          new SimulatedActivityId(0),
          new SimulatedActivity(
              serializedDelayDirective.getTypeName(),
              Map.of(),
              Instant.EPOCH.plus(1, ChronoUnit.MINUTES),
              oneMinute,
              null,
              List.of(),
              Optional.of(new ActivityDirectiveId(0)),
              computedAttributes));
      simulatedActivities.put(
          new SimulatedActivityId(400),
          new SimulatedActivity(
              serializedDelayDirective.getTypeName(),
              Map.of(),
              Instant.EPOCH.plus(1, ChronoUnit.MINUTES),
              oneMinute,
              null,
              List.of(),
              Optional.of(new ActivityDirectiveId(400)),
              computedAttributes));

      for (long l = 1, k = 401, c = 1; l < 400; l++, k++) {
        final var activityDirectiveIdAETA = new ActivityDirectiveId(l);
        final var activityDirectiveIdEveryFifth = new ActivityDirectiveId(k);

        allEndTimeAnchors.put(
            activityDirectiveIdAETA,
            new ActivityDirective(
                oneMinute,
                serializedDelayDirective,
                new ActivityDirectiveId(l - 1),
                false));
        simulatedActivities.put(
            new SimulatedActivityId(l),
            new SimulatedActivity(
                serializedDelayDirective.getTypeName(),
                Map.of(),
                Instant.EPOCH.plus((2*l)+1, ChronoUnit.MINUTES),
                oneMinute,
                null,
                List.of(),
                Optional.of(activityDirectiveIdAETA),
                computedAttributes));

        if (k % 5 == 0) {
          endTimeAnchorEveryFifth.put(
              activityDirectiveIdEveryFifth,
              new ActivityDirective(
                  oneMinute,
                  serializedDelayDirective,
                  new ActivityDirectiveId(k - 1),
                  false));
          c++;
        } else {
          endTimeAnchorEveryFifth.put(
              activityDirectiveIdEveryFifth,
              new ActivityDirective(
                  oneMinute,
                  serializedDelayDirective,
                  new ActivityDirectiveId(k - 1),
                  true));
        }
        simulatedActivities.put(
            new SimulatedActivityId(k),
            new SimulatedActivity(
                serializedDelayDirective.getTypeName(),
                Map.of(),
                Instant.EPOCH.plus(l+c, ChronoUnit.MINUTES),
                oneMinute,
                null,
                List.of(),
                Optional.of(activityDirectiveIdEveryFifth),
                computedAttributes));
      }

      activitiesToSimulate.putAll(allEndTimeAnchors);
      activitiesToSimulate.putAll(endTimeAnchorEveryFifth);

      // Assert Simulation results
      final var expectedSimResults = new SimulationResults(
          Map.of(), //real
          Map.of(), //discrete
          simulatedActivities,
          Map.of(), //unfinished
          planStart,
          Duration.of(800, Duration.MINUTES), // duration. 800 because 400 * 2 (AETA is the temporally longer chain), NOT because of number of activities
          modelTopicList,
          new TreeMap<>() //events
      );

      final var actualSimResults = simulateActivities(activitiesToSimulate);

      assertEqualsSimulationResults(expectedSimResults, actualSimResults);
    }

    @Test
    @DisplayName("Reference to anchored activities are correctly maintained by the driver")
    public void activitiesAnchoredToOtherActivitiesSimple() throws SchedulingInterruptedException {
      final var activitiesToSimulate = new HashMap<ActivityDirectiveId, ActivityDirective>(2);
      activitiesToSimulate.put(
          new ActivityDirectiveId(0),
          new ActivityDirective(oneMinute, serializedDelayDirective, null, true));
      activitiesToSimulate.put(
          new ActivityDirectiveId(1),
          new ActivityDirective(oneMinute, serializedDelayDirective, new ActivityDirectiveId(0), false));
      final var simulationResults = simulateActivities(activitiesToSimulate);
      final var durationOfAnchoredActivity = SimulationFacadeUtils.getActivityDuration(new ActivityDirectiveId(1), simulationResults);
      assertTrue(durationOfAnchoredActivity.isPresent());
    }

    @Test
    @DisplayName("Decomposition and anchors do not interfere with each other")
    public void decomposingActivitiesAndAnchors() throws SchedulingInterruptedException{
      // Given positions Left, Center, Right in an anchor chain, where each position can either contain a Non-Decomposition (ND) activity or a Decomposition (D) activity,
      // and the connection between Center and Left and Right and Center can be either Start (<-s-) or End (<-e-),
      // and two NDs cannot be adjacent to each other, there are 20 permutations.

      // In order to have fewer activities and more complex subtrees, the first and second elements of a set of sequences will be reused when possible
      final var activitiesToSimulate = new HashMap<ActivityDirectiveId, ActivityDirective>(23);
      // NOTE: This list is intentionally keyed on ActivityDirectiveId, not on SimulatedActivityId.
      // Additionally, because we do not know the order the child activities will generate in, DecompositionDirectives will have a List.of() rather than the correct value
      final var topLevelSimulatedActivities = new HashMap<ActivityDirectiveId, SimulatedActivity>(23);
      final var threeMinutes = Duration.of(3, Duration.MINUTES);

      // ND <-s- D <-s- D
      activitiesToSimulate.put(new ActivityDirectiveId(1), new ActivityDirective(Duration.ZERO, serializedDelayDirective, null, true));
      activitiesToSimulate.put(new ActivityDirectiveId(2), new ActivityDirective(Duration.ZERO, serializedDecompositionDirective, new ActivityDirectiveId(1), true));
      activitiesToSimulate.put(new ActivityDirectiveId(3), new ActivityDirective(Duration.ZERO, serializedDecompositionDirective, new ActivityDirectiveId(2), true));
      topLevelSimulatedActivities.put(
          new ActivityDirectiveId(1),
          new SimulatedActivity(serializedDelayDirective.getTypeName(), Map.of(), Instant.EPOCH, oneMinute, null, List.of(), Optional.of(new ActivityDirectiveId(1)), computedAttributes));
      topLevelSimulatedActivities.put(
          new ActivityDirectiveId(2),
          new SimulatedActivity(serializedDecompositionDirective.getTypeName(), Map.of(), Instant.EPOCH, threeMinutes, null, List.of(), Optional.of(new ActivityDirectiveId(2)), computedAttributes));
      topLevelSimulatedActivities.put(
          new ActivityDirectiveId(3),
          new SimulatedActivity(serializedDecompositionDirective.getTypeName(), Map.of(), Instant.EPOCH, threeMinutes, null, List.of(), Optional.of(new ActivityDirectiveId(3)), computedAttributes));

      // ND <-s- D <-e- D
      activitiesToSimulate.put(new ActivityDirectiveId(4), new ActivityDirective(Duration.ZERO, serializedDecompositionDirective, new ActivityDirectiveId(2), false));
      topLevelSimulatedActivities.put(
          new ActivityDirectiveId(4),
          new SimulatedActivity(serializedDecompositionDirective.getTypeName(), Map.of(), Instant.EPOCH.plus(3, ChronoUnit.MINUTES), threeMinutes, null, List.of(), Optional.of(new ActivityDirectiveId(4)), computedAttributes));

      // ND <-s- D <-s- ND
      activitiesToSimulate.put(new ActivityDirectiveId(5), new ActivityDirective(Duration.ZERO, serializedDelayDirective, new ActivityDirectiveId(2), true));
      topLevelSimulatedActivities.put(
          new ActivityDirectiveId(5),
          new SimulatedActivity(serializedDelayDirective.getTypeName(), Map.of(), Instant.EPOCH, oneMinute, null, List.of(), Optional.of(new ActivityDirectiveId(5)), computedAttributes));

      // ND <-s- D <-e- ND
      activitiesToSimulate.put(new ActivityDirectiveId(6), new ActivityDirective(Duration.ZERO, serializedDelayDirective, new ActivityDirectiveId(2), false));
      topLevelSimulatedActivities.put(
          new ActivityDirectiveId(6),
          new SimulatedActivity(serializedDelayDirective.getTypeName(), Map.of(), Instant.EPOCH.plus(3, ChronoUnit.MINUTES), oneMinute, null, List.of(), Optional.of(new ActivityDirectiveId(6)), computedAttributes));

      // ND <-e- D <-s- D
      activitiesToSimulate.put(new ActivityDirectiveId(7), new ActivityDirective(Duration.ZERO, serializedDecompositionDirective, new ActivityDirectiveId(1), false));
      activitiesToSimulate.put(new ActivityDirectiveId(8), new ActivityDirective(Duration.ZERO, serializedDecompositionDirective, new ActivityDirectiveId(7), true));
      topLevelSimulatedActivities.put(
          new ActivityDirectiveId(7),
          new SimulatedActivity(serializedDecompositionDirective.getTypeName(), Map.of(), Instant.EPOCH.plus(1, ChronoUnit.MINUTES), threeMinutes, null, List.of(), Optional.of(new ActivityDirectiveId(7)), computedAttributes));
      topLevelSimulatedActivities.put(
          new ActivityDirectiveId(8),
          new SimulatedActivity(serializedDecompositionDirective.getTypeName(), Map.of(), Instant.EPOCH.plus(1, ChronoUnit.MINUTES), threeMinutes, null, List.of(), Optional.of(new ActivityDirectiveId(8)), computedAttributes));

      // ND <-e- D <-e- D
      activitiesToSimulate.put(new ActivityDirectiveId(9), new ActivityDirective(Duration.ZERO, serializedDecompositionDirective, new ActivityDirectiveId(7), false));
      topLevelSimulatedActivities.put(
          new ActivityDirectiveId(9),
          new SimulatedActivity(serializedDecompositionDirective.getTypeName(), Map.of(), Instant.EPOCH.plus(4, ChronoUnit.MINUTES), threeMinutes, null, List.of(), Optional.of(new ActivityDirectiveId(9)), computedAttributes));

      // ND <-e- D <-s- ND
      activitiesToSimulate.put(new ActivityDirectiveId(10), new ActivityDirective(Duration.ZERO, serializedDelayDirective, new ActivityDirectiveId(7), true));
      topLevelSimulatedActivities.put(
          new ActivityDirectiveId(10),
          new SimulatedActivity(serializedDelayDirective.getTypeName(), Map.of(), Instant.EPOCH.plus(1, ChronoUnit.MINUTES), oneMinute, null, List.of(), Optional.of(new ActivityDirectiveId(10)), computedAttributes));

      // ND <-e- D <-e- ND
      activitiesToSimulate.put(new ActivityDirectiveId(11), new ActivityDirective(Duration.ZERO, serializedDelayDirective, new ActivityDirectiveId(7), false));
      topLevelSimulatedActivities.put(
          new ActivityDirectiveId(11),
          new SimulatedActivity(serializedDelayDirective.getTypeName(), Map.of(), Instant.EPOCH.plus(4, ChronoUnit.MINUTES), oneMinute, null, List.of(), Optional.of(new ActivityDirectiveId(11)), computedAttributes));

      // D <-s- D <-s- D
      activitiesToSimulate.put(new ActivityDirectiveId(12), new ActivityDirective(Duration.ZERO, serializedDecompositionDirective, new ActivityDirectiveId(3), true));
      topLevelSimulatedActivities.put(
          new ActivityDirectiveId(12),
          new SimulatedActivity(serializedDecompositionDirective.getTypeName(), Map.of(), Instant.EPOCH, threeMinutes, null, List.of(), Optional.of(new ActivityDirectiveId(12)), computedAttributes));

      // D <-s- D <-e- D
      activitiesToSimulate.put(new ActivityDirectiveId(13), new ActivityDirective(Duration.ZERO, serializedDecompositionDirective, new ActivityDirectiveId(3), false));
      topLevelSimulatedActivities.put(
          new ActivityDirectiveId(13),
          new SimulatedActivity(serializedDecompositionDirective.getTypeName(), Map.of(), Instant.EPOCH.plus(3, ChronoUnit.MINUTES), threeMinutes, null, List.of(), Optional.of(new ActivityDirectiveId(13)), computedAttributes));

      // D <-s- D <-s- ND
      activitiesToSimulate.put(new ActivityDirectiveId(14), new ActivityDirective(Duration.ZERO, serializedDelayDirective, new ActivityDirectiveId(3), true));
      topLevelSimulatedActivities.put(
          new ActivityDirectiveId(14),
          new SimulatedActivity(serializedDelayDirective.getTypeName(), Map.of(), Instant.EPOCH, oneMinute, null, List.of(), Optional.of(new ActivityDirectiveId(14)), computedAttributes));

      // D <-s- D <-e- ND
      activitiesToSimulate.put(new ActivityDirectiveId(15), new ActivityDirective(Duration.ZERO, serializedDelayDirective, new ActivityDirectiveId(3), false));
      topLevelSimulatedActivities.put(
          new ActivityDirectiveId(15),
          new SimulatedActivity(serializedDelayDirective.getTypeName(), Map.of(), Instant.EPOCH.plus(3, ChronoUnit.MINUTES), oneMinute, null, List.of(), Optional.of(new ActivityDirectiveId(15)), computedAttributes));

      // D <-e- D <-s- D
      activitiesToSimulate.put(new ActivityDirectiveId(16), new ActivityDirective(Duration.ZERO, serializedDecompositionDirective, new ActivityDirectiveId(4), true));
      topLevelSimulatedActivities.put(
          new ActivityDirectiveId(16),
          new SimulatedActivity(serializedDecompositionDirective.getTypeName(), Map.of(), Instant.EPOCH.plus(3, ChronoUnit.MINUTES), threeMinutes, null, List.of(), Optional.of(new ActivityDirectiveId(16)), computedAttributes));

      // D <-e- D <-e- D
      activitiesToSimulate.put(new ActivityDirectiveId(17), new ActivityDirective(Duration.ZERO, serializedDecompositionDirective, new ActivityDirectiveId(4), false));
      topLevelSimulatedActivities.put(
          new ActivityDirectiveId(17),
          new SimulatedActivity(serializedDecompositionDirective.getTypeName(), Map.of(), Instant.EPOCH.plus(6, ChronoUnit.MINUTES), threeMinutes, null, List.of(), Optional.of(new ActivityDirectiveId(17)), computedAttributes));

      // D <-e- D <-s- ND
      activitiesToSimulate.put(new ActivityDirectiveId(18), new ActivityDirective(Duration.ZERO, serializedDelayDirective, new ActivityDirectiveId(4), true));
      topLevelSimulatedActivities.put(
          new ActivityDirectiveId(18),
          new SimulatedActivity(serializedDelayDirective.getTypeName(), Map.of(), Instant.EPOCH.plus(3, ChronoUnit.MINUTES), oneMinute, null, List.of(), Optional.of(new ActivityDirectiveId(18)), computedAttributes));

      // D <-e- D <-e- ND
      activitiesToSimulate.put(new ActivityDirectiveId(19), new ActivityDirective(Duration.ZERO, serializedDelayDirective, new ActivityDirectiveId(4), false));
      topLevelSimulatedActivities.put(
          new ActivityDirectiveId(19),
          new SimulatedActivity(serializedDelayDirective.getTypeName(), Map.of(), Instant.EPOCH.plus(6, ChronoUnit.MINUTES), oneMinute, null, List.of(), Optional.of(new ActivityDirectiveId(19)), computedAttributes));

      // D <-s- ND <-s- D
      activitiesToSimulate.put(new ActivityDirectiveId(20), new ActivityDirective(Duration.ZERO, serializedDecompositionDirective, new ActivityDirectiveId(14), true));
      topLevelSimulatedActivities.put(
          new ActivityDirectiveId(20),
          new SimulatedActivity(serializedDecompositionDirective.getTypeName(), Map.of(), Instant.EPOCH, threeMinutes, null, List.of(), Optional.of(new ActivityDirectiveId(20)), computedAttributes));

      // D <-s- ND <-e- D
      activitiesToSimulate.put(new ActivityDirectiveId(21), new ActivityDirective(Duration.ZERO, serializedDecompositionDirective, new ActivityDirectiveId(14), false));
      topLevelSimulatedActivities.put(
          new ActivityDirectiveId(21),
          new SimulatedActivity(serializedDecompositionDirective.getTypeName(), Map.of(), Instant.EPOCH.plus(1, ChronoUnit.MINUTES), threeMinutes, null, List.of(), Optional.of(new ActivityDirectiveId(21)), computedAttributes));

      // D <-e- ND <-s- D
      activitiesToSimulate.put(new ActivityDirectiveId(22), new ActivityDirective(Duration.ZERO, serializedDecompositionDirective, new ActivityDirectiveId(15), true));
      topLevelSimulatedActivities.put(
          new ActivityDirectiveId(22),
          new SimulatedActivity(serializedDecompositionDirective.getTypeName(), Map.of(), Instant.EPOCH.plus(3, ChronoUnit.MINUTES), threeMinutes, null, List.of(), Optional.of(new ActivityDirectiveId(22)), computedAttributes));

      // D <-e- ND <-e- D
      activitiesToSimulate.put(new ActivityDirectiveId(23), new ActivityDirective(Duration.ZERO, serializedDecompositionDirective, new ActivityDirectiveId(15), false));
      topLevelSimulatedActivities.put(
          new ActivityDirectiveId(23),
          new SimulatedActivity(serializedDecompositionDirective.getTypeName(), Map.of(), Instant.EPOCH.plus(4, ChronoUnit.MINUTES), threeMinutes, null, List.of(), Optional.of(new ActivityDirectiveId(23)), computedAttributes));

      final var actualSimResults = simulateActivities(activitiesToSimulate);

      assertEquals(planStart, actualSimResults.getStartTime());
      assertTrue(actualSimResults.getUnfinishedActivities().isEmpty());
      assertEquals(modelTopicList.size(), actualSimResults.getTopics().size());
      for(int i = 0; i < modelTopicList.size(); ++i){
        assertEquals(modelTopicList.get(i), actualSimResults.getTopics().get(i));
      }

      final var childSimulatedActivities = new HashMap<SimulatedActivityId, SimulatedActivity>(28);
      final var otherSimulatedActivities = new HashMap<SimulatedActivityId, SimulatedActivity>(23);
      assertEquals(51, actualSimResults.getSimulatedActivities().size()); // 23 + 2*(14 Decomposing activities)

      for(final var entry : actualSimResults.getSimulatedActivities().entrySet()) {
        if(entry.getValue().parentId()==null){
          otherSimulatedActivities.put(entry.getKey(), entry.getValue());
        }
        else {
          childSimulatedActivities.put(entry.getKey(), entry.getValue());
        }
      }
      assertEquals(23, otherSimulatedActivities.size());
      assertEquals(28, childSimulatedActivities.size());

      for(final var entry : otherSimulatedActivities.entrySet()){
        assertTrue(entry.getValue().directiveId().isPresent());
        final ActivityDirectiveId topLevelKey = entry.getValue().directiveId().get();
        assertEqualsAsideFromChildren(topLevelSimulatedActivities.get(topLevelKey), entry.getValue());
        // For decompositions, examine the children
        if(entry.getValue().type().equals(serializedDecompositionDirective.getTypeName())){
          assertEquals(2, entry.getValue().childIds().size());
          final var firstChild = childSimulatedActivities.remove(entry.getValue().childIds().get(0));
          final var secondChild = childSimulatedActivities.remove(entry.getValue().childIds().get(1));

          // Assert the children look as expected and one starts and the parent's start time, and one starts two minutes later
          assertNotNull(firstChild);
          assertNotNull(secondChild);
          assertTrue(firstChild.childIds().isEmpty());
          assertTrue(secondChild.childIds().isEmpty());

          if(firstChild.start().isBefore(secondChild.start())){
            assertEqualsAsideFromChildren(
                new SimulatedActivity(
                    serializedDelayDirective.getTypeName(),
                    Map.of(),
                    entry.getValue().start(),
                    oneMinute,
                    entry.getKey(),
                    List.of(),
                    Optional.empty(),
                    computedAttributes),
                firstChild);
            assertEqualsAsideFromChildren(
                new SimulatedActivity(
                    serializedDelayDirective.getTypeName(),
                    Map.of(),
                    entry.getValue().start().plus(2, ChronoUnit.MINUTES),
                    oneMinute,
                    entry.getKey(),
                    List.of(),
                    Optional.empty(),
                    computedAttributes),
                secondChild);
          } else {
            assertEqualsAsideFromChildren(
                new SimulatedActivity(
                    serializedDelayDirective.getTypeName(),
                    Map.of(),
                    entry.getValue().start().plus(2, ChronoUnit.MINUTES),
                    oneMinute,
                    entry.getKey(),
                    List.of(),
                    Optional.empty(),
                    computedAttributes),
                firstChild);
            assertEqualsAsideFromChildren(
                new SimulatedActivity(
                    serializedDelayDirective.getTypeName(),
                    Map.of(),
                    entry.getValue().start(),
                    oneMinute,
                    entry.getKey(),
                    List.of(),
                    Optional.empty(),
                    computedAttributes),
                secondChild);
          }
        }
      }

      // We have examined all the children
      assertTrue(childSimulatedActivities.isEmpty());
    }

    @Test
    @DisplayName("Activities arranged in a wide anchor tree simulate at the correct time")
    public void naryTreeAnchorChain() throws SchedulingInterruptedException{
      // Full and complete 5-ary tree,  6 levels deep
      // Number of activity directives = 5^0 + 5^1 + 5^2 + 5^3 + 5^4 + 5^5 = 3906

      final var activitiesToSimulate = new HashMap<ActivityDirectiveId, ActivityDirective>(3906);
      final var simulatedActivities = new HashMap<SimulatedActivityId, SimulatedActivity>(3906);

      activitiesToSimulate.put(
          new ActivityDirectiveId(0),
          new ActivityDirective(Duration.ZERO, serializedDelayDirective, null, true));
      simulatedActivities.put(
          new SimulatedActivityId(0),
          new SimulatedActivity(
              serializedDelayDirective.getTypeName(),
              Map.of(),
              Instant.EPOCH,
              oneMinute,
              null,
              List.of(),
              Optional.of(new ActivityDirectiveId(0)),
              computedAttributes));

      constructFullComplete5AryTree(5, 1, 0, activitiesToSimulate, simulatedActivities);

      // Assert Simulation results
      final var expectedSimResults = new SimulationResults(
          Map.of(), //real
          Map.of(), //discrete
          simulatedActivities,
          Map.of(), //unfinished
          planStart,
          Duration.of(6, Duration.MINUTES), // duration. wide tree, but max branch length is 6
          modelTopicList,
          new TreeMap<>() //events
      );
      final var actualSimResults = simulateActivities(activitiesToSimulate);

      assertEquals(3906, expectedSimResults.getSimulatedActivities().size());
      assertEqualsSimulationResults(expectedSimResults, actualSimResults);
    }
  }

  //region Mission Model
  private static final List<Triple<Integer, String, ValueSchema>> modelTopicList = Arrays.asList(
      Triple.of(0, "ActivityType.Input.DelayActivityDirective", new ValueSchema.StructSchema(Map.of())),
      Triple.of(1, "ActivityType.Output.DelayActivityDirective", new ValueSchema.StructSchema(Map.of())),
      Triple.of(2, "ActivityType.Input.DecomposingActivityDirective", new ValueSchema.StructSchema(Map.of())),
      Triple.of(3, "ActivityType.Output.DecomposingActivityDirective", new ValueSchema.StructSchema(Map.of())));

  private static final Topic<Object> delayedActivityDirectiveInputTopic = new Topic<>();
  private static final Topic<Object> delayedActivityDirectiveOutputTopic = new Topic<>();
  private static final DirectiveType<Object, Object, Object> delayedActivityDirective = new DirectiveType<>() {
    @Override
    public InputType<Object> getInputType() {
      return testModelInputType;
    }

    @Override
    public OutputType<Object> getOutputType() {
      return testModelOutputType;
    }

    @Override
    public TaskFactory<Object> getTaskFactory(final Object o, final Object o2) {
      return executor -> new OneStepTask<>($ -> {
        $.startActivity(this, delayedActivityDirectiveInputTopic);
        return TaskStatus.delayed(oneMinute, new OneStepTask<>($$ -> {
          $$.endActivity(Unit.UNIT, delayedActivityDirectiveOutputTopic);
          return TaskStatus.completed(Unit.UNIT);
        }));
      });
    }
  };

  private static final Topic<Object> decomposingActivityDirectiveInputTopic = new Topic<>();
  private static final Topic<Object> decomposingActivityDirectiveOutputTopic = new Topic<>();
  private static final DirectiveType<Object, Object, Object> decomposingActivityDirective = new DirectiveType<>() {
    @Override
    public InputType<Object> getInputType() {
      return testModelInputType;
    }

    @Override
    public OutputType<Object> getOutputType() {
      return testModelOutputType;
    }

    @Override
    public TaskFactory<Object> getTaskFactory(final Object o, final Object o2) {
      return executor -> new OneStepTask<>(scheduler -> {
        scheduler.startActivity(this, decomposingActivityDirectiveInputTopic);
        return TaskStatus.delayed(
            Duration.ZERO,
            new OneStepTask<>($ -> {
              try {
                $.spawn(InSpan.Fresh, delayedActivityDirective.getTaskFactory(null, null));
              } catch (final InstantiationException ex) {
                throw new Error("Unexpected state: activity instantiation of DelayedActivityDirective failed with: %s".formatted(
                    ex.toString()));
              }
              return TaskStatus.delayed(Duration.of(120, Duration.SECOND), new OneStepTask<>($$ -> {
                try {
                  $$.spawn(InSpan.Fresh, delayedActivityDirective.getTaskFactory(null, null));
                } catch (final InstantiationException ex) {
                  throw new Error(
                      "Unexpected state: activity instantiation of DelayedActivityDirective failed with: %s".formatted(
                          ex.toString()));
                }
                $$.endActivity(Unit.UNIT, decomposingActivityDirectiveOutputTopic);
                return TaskStatus.completed(Unit.UNIT);
              }));
            }));
      });
    }
  };

  private static final InputType<Object> testModelInputType = new InputType<>() {
    @Override
    public List<Parameter> getParameters() {
      return List.of();
    }

    @Override
    public List<String> getRequiredParameters() {
      return List.of();
    }

    @Override
    public Object instantiate(final Map arguments) {
      return new Object();
    }

    @Override
    public Map<String, SerializedValue> getArguments(final Object value) {
      return Map.of();
    }

    @Override
    public List<ValidationNotice> getValidationFailures(final Object value) {
      return List.of();
    }
  };

  private static final OutputType<Object> testModelOutputType = new OutputType<>() {
    @Override
    public ValueSchema getSchema() {
      return ValueSchema.ofStruct(Map.of());
    }

    @Override
    public SerializedValue serialize(final Object value) {
      return SerializedValue.of(Map.of());
    }
  };

  private static LinkedHashMap<Topic<?>, MissionModel.SerializableTopic<?>> _topics = new LinkedHashMap<>();
  {
    _topics.put(delayedActivityDirectiveInputTopic,
                new MissionModel.SerializableTopic<>(
                    "ActivityType.Input.DelayActivityDirective",
                    delayedActivityDirectiveInputTopic,
                    testModelOutputType));
    _topics.put(delayedActivityDirectiveOutputTopic,
                new MissionModel.SerializableTopic<>(
                    "ActivityType.Output.DelayActivityDirective",
                    delayedActivityDirectiveOutputTopic,
                    testModelOutputType));
    _topics.put(decomposingActivityDirectiveInputTopic,
                new MissionModel.SerializableTopic<>(
                    "ActivityType.Input.DecomposingActivityDirective",
                    decomposingActivityDirectiveInputTopic,
                    testModelOutputType));
    _topics.put(decomposingActivityDirectiveOutputTopic,
                new MissionModel.SerializableTopic<>(
                    "ActivityType.Output.DecomposingActivityDirective",
                    decomposingActivityDirectiveOutputTopic,
                    testModelOutputType));
  }

  private static final MissionModel<Object> AnchorTestModel = new MissionModel<>(
      new Object(),
      new LiveCells(null),
      Map.of(),
      _topics,
      Map.of(),
      DirectiveTypeRegistry.extract(
          new ModelType<>() {

            @Override
            public Map<String, ? extends DirectiveType<Object, ?, ?>> getDirectiveTypes() {
              return Map.of(
                  "DelayActivityDirective",
                  delayedActivityDirective,
                  "DecomposingActivityDirective",
                  decomposingActivityDirective);
            }

            @Override
            public InputType<Object> getConfigurationType() {
              return testModelInputType;
            }

            @Override
            public Object instantiate(
                final Instant planStart,
                final Object configuration,
                final Initializer builder)
            {
              return new Object();
            }
          }
      )
  );
  //endregion
}
