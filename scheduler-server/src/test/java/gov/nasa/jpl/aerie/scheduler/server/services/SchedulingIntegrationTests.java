package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.DurationValueMapper;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelLoader;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.TimeUtility;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.server.config.PlanOutputMode;
import gov.nasa.jpl.aerie.scheduler.server.models.GoalId;
import gov.nasa.jpl.aerie.scheduler.server.models.GoalRecord;
import gov.nasa.jpl.aerie.scheduler.server.models.PlanId;
import gov.nasa.jpl.aerie.scheduler.server.models.Specification;
import gov.nasa.jpl.aerie.scheduler.server.models.SpecificationId;
import gov.nasa.jpl.aerie.scheduler.server.models.Timestamp;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SchedulingIntegrationTests {

  private record MissionModelDescription(String name, Map<String, SerializedValue> config, Path libPath) {}

  private record SchedulingGoal(GoalId goalId, String definition, boolean enabled) {};

  private static final MissionModelDescription BANANANATION = new MissionModelDescription(
      "bananantion",
      Map.of("initialDataPath", SerializedValue.of("/etc/hosts")),
      Path.of(System.getenv("AERIE_ROOT"), "examples", "banananation", "build", "libs")
  );
  private SchedulingDSLCompilationService schedulingDSLCompiler;

  @BeforeAll
  void setup() throws IOException {
    this.schedulingDSLCompiler = new SchedulingDSLCompilationService(new TypescriptCodeGenerationService());
  }

  @Test
  void testEmptyPlanEmptySpecification() {
    final var results = runScheduler(BANANANATION, List.of(), List.of());
    assertEquals(Map.of(), results.scheduleResults.goalResults());
  }

  @Test
  void testEmptyPlanSimpleRecurrenceGoal() {
    final var results = runScheduler(
        BANANANATION,
        List.of(),
        List.of(new SchedulingGoal(new GoalId(0L), """
          export default () => Goal.ActivityRecurrenceGoal({
            activityTemplate: ActivityTemplates.PeelBanana({
              peelDirection: "fromStem",
            }),
            interval: 24 * 60 * 60 * 1000 * 1000 // one day in microseconds
          })
          """, true)));
    assertEquals(1, results.scheduleResults.goalResults().size());
    final var goalResult = results.scheduleResults.goalResults().get(new GoalId(0L));
    assertTrue(goalResult.satisfied());
    assertEquals(4, goalResult.createdActivities().size());
    for (final var activity : goalResult.createdActivities()) {
      assertNotNull(activity);
    }
    for (final var activity : goalResult.satisfyingActivities()) {
      assertNotNull(activity);
    }
    final var updatedPlan = results.updatedPlan();
    for (final var activity : updatedPlan) {
      final var arguments = activity.args();
      assertEquals("PeelBanana", activity.type());
      assertEquals(SerializedValue.of("fromStem"), arguments.get("peelDirection"));
    }
  }

  @Test
  void testEmptyPlanDurationCardinalityGoal() {
    final var results = runScheduler(BANANANATION,
        List.of(),
        List.of(new SchedulingGoal(new GoalId(0L), """
                  export default function myGoal() {
                                    return Goal.CardinalityGoal({
                                      activityTemplate: ActivityTemplates.GrowBanana({
                                        quantity: 1,
                                        growingDuration: 1000000,
                                      }),
                                      inPeriod: {start :0, end:10000000},
                                      specification : {duration: 10 * 1000000}
                                    })
                  }
                    """, true)));
    assertEquals(1, results.scheduleResults.goalResults().size());
    final var goalResult = results.scheduleResults.goalResults().get(new GoalId(0L));

    assertTrue(goalResult.satisfied());
    assertEquals(10, goalResult.createdActivities().size());
    for (final var activity : goalResult.createdActivities()) {
      assertNotNull(activity);
    }
    for (final var activity : goalResult.satisfyingActivities()) {
      assertNotNull(activity);
    }

    final var activitiesByType = partitionByActivityType(results.updatedPlan());

    final var growBananas = activitiesByType.get("GrowBanana");
    assertEquals(10, growBananas.size());

    final var setStartTimes = new HashSet<>(Stream
                                              .of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
                                              .map(x -> Duration.of(x, Duration.SECOND)).toList());
    for (final var growBanana : growBananas) {
      assertTrue(setStartTimes.remove(growBanana.startTime()));
      assertEquals(SerializedValue.of(1), growBanana.args().get("quantity"));
    }
  }

  @Test
  void testEmptyPlanOccurrenceCardinalityGoal() {
    final var results = runScheduler(BANANANATION,
        List.of(),
        List.of(new SchedulingGoal(new GoalId(0L), """
                  export default function myGoal() {
                                    return Goal.CardinalityGoal({
                                      activityTemplate: ActivityTemplates.GrowBanana({
                                        quantity: 1,
                                        growingDuration: 1000000,
                                      }),
                                      inPeriod: {start :0, end:10000000},
                                      specification : {occurrence: 10}
                                    })
                  }
                    """, true)));

    assertEquals(1, results.scheduleResults.goalResults().size());
    final var goalResult = results.scheduleResults.goalResults().get(new GoalId(0L));

    assertTrue(goalResult.satisfied());
    assertEquals(10, goalResult.createdActivities().size());
    for (final var activity : goalResult.createdActivities()) {
      assertNotNull(activity);
    }
    for (final var activity : goalResult.satisfyingActivities()) {
      assertNotNull(activity);
    }

    final var activitiesByType = partitionByActivityType(results.updatedPlan());

    final var growBananas = activitiesByType.get("GrowBanana");
    assertEquals(10, growBananas.size());

    final var setStartTimes = new HashSet<>(Stream
                                              .of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
                                              .map(x -> Duration.of(x, Duration.SECOND)).toList());
    for (final var growBanana : growBananas) {
      assertTrue(setStartTimes.remove(growBanana.startTime()));
      assertEquals(SerializedValue.of(1), growBanana.args().get("quantity"));
    }
  }


  @Test
  void testSingleActivityPlanSimpleRecurrenceGoal() {
    final var results = runScheduler(
        BANANANATION,
        List.of(new MockMerlinService.PlannedActivityInstance("BiteBanana", Map.of("biteSize", SerializedValue.of(1)), Duration.ZERO)),
        List.of(new SchedulingGoal(new GoalId(0L), """
          export default () => Goal.ActivityRecurrenceGoal({
            activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
            interval: 24 * 60 * 60 * 1000 * 1000 // one day in microseconds
          })
          """, true)));

    assertEquals(1, results.scheduleResults.goalResults().size());
    final var goalResult = results.scheduleResults.goalResults().get(new GoalId(0L));

    assertTrue(goalResult.satisfied());
    assertEquals(4, goalResult.createdActivities().size());
    for (final var activity : goalResult.createdActivities()) {
      assertNotNull(activity);
    }
    for (final var activity : goalResult.satisfyingActivities()) {
      assertNotNull(activity);
    }

    final var activitiesByType = partitionByActivityType(results.updatedPlan());
    final var biteBananas = activitiesByType.get("BiteBanana");
    assertEquals(1, biteBananas.size());

    final var biteBanana = biteBananas.iterator().next();
    assertEquals(SerializedValue.of(1), biteBanana.args().get("biteSize"));

    final var peelBananas = activitiesByType.get("PeelBanana");
    assertEquals(4, peelBananas.size());

    for (final var peelBanana : peelBananas) {
      assertEquals(SerializedValue.of("fromStem"), peelBanana.args().get("peelDirection"));
    }
  }

  @Test
  void testSingleActivityPlanSimpleCoexistenceGoal() {
    final var growBananaDuration = Duration.of(1, Duration.HOUR);
    final var results = runScheduler(
        BANANANATION,
        List.of(new MockMerlinService.PlannedActivityInstance(
            "GrowBanana",
            Map.of(
                "quantity", SerializedValue.of(1),
                "growingDuration", new DurationValueMapper().serializeValue(growBananaDuration)),
            Duration.ZERO)),
        List.of(new SchedulingGoal(new GoalId(0L), """
          export default () => Goal.CoexistenceGoal({
            forEach: ActivityExpression.ofType(ActivityTypes.GrowBanana),
            activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
            startsAt: TimingConstraint.singleton(WindowProperty.END).plus(5 * 60 * 1000 * 1000)
          })
          """, true)));

    assertEquals(1, results.scheduleResults.goalResults().size());
    final var goalResult = results.scheduleResults.goalResults().get(new GoalId(0L));

    assertTrue(goalResult.satisfied());
    assertEquals(1, goalResult.createdActivities().size());
    for (final var activity : goalResult.createdActivities()) {
      assertNotNull(activity);
    }
    for (final var activity : goalResult.satisfyingActivities()) {
      assertNotNull(activity);
    }

    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var peelBananas = planByActivityType.get("PeelBanana");
    final var growBananas = planByActivityType.get("GrowBanana");
    assertEquals(1, peelBananas.size());
    assertEquals(1, growBananas.size());
    final var peelBanana = peelBananas.iterator().next();
    final var growBanana = growBananas.iterator().next();

    assertEquals(SerializedValue.of("fromStem"), peelBanana.args().get("peelDirection"));
    assertEquals(SerializedValue.of(1), growBanana.args().get("quantity"));

    assertEquals(growBanana.startTime().plus(growBananaDuration).plus(Duration.of(5, Duration.MINUTES)), peelBanana.startTime());
  }

  @Test
  void testSingleActivityPlanSimpleCoexistenceGoal_constrainEndTime() {
    final var growBananaDuration = Duration.of(1, Duration.HOUR);
    final var results = runScheduler(
        BANANANATION,
        List.of(new MockMerlinService.PlannedActivityInstance(
            "GrowBanana",
            Map.of(
                "quantity", SerializedValue.of(1),
                "growingDuration", new DurationValueMapper().serializeValue(growBananaDuration)),
            Duration.of(5, Duration.MINUTES))),
        List.of(new SchedulingGoal(new GoalId(0L), """
          export default () => Goal.CoexistenceGoal({
            forEach: ActivityExpression.ofType(ActivityTypes.GrowBanana),
            activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
            endsWithin: TimingConstraint.range(WindowProperty.END, Operator.PLUS, 5 * 60 * 1000 * 1000)
          })
          """, true)));

    assertEquals(1, results.scheduleResults.goalResults().size());
    final var goalResult = results.scheduleResults.goalResults().get(new GoalId(0L));

    assertTrue(goalResult.satisfied());
    assertEquals(1, goalResult.createdActivities().size());
    for (final var activity : goalResult.createdActivities()) {
      assertNotNull(activity);
    }
    for (final var activity : goalResult.satisfyingActivities()) {
      assertNotNull(activity);
    }

    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var peelBananas = planByActivityType.get("PeelBanana");
    final var growBananas = planByActivityType.get("GrowBanana");
    assertEquals(1, peelBananas.size());
    assertEquals(1, growBananas.size());
    final var peelBanana = peelBananas.iterator().next();
    final var growBanana = growBananas.iterator().next();

    assertEquals(SerializedValue.of("fromStem"), peelBanana.args().get("peelDirection"));
    assertEquals(SerializedValue.of(1), growBanana.args().get("quantity"));

    assertTrue(growBanana.startTime().plus(growBananaDuration).plus(Duration.of(5, Duration.MINUTES)).noShorterThan(peelBanana.startTime()));
    assertTrue(growBanana.startTime().plus(growBananaDuration).noLongerThan(peelBanana.startTime()));
  }

  @Test
  void testStateCoexistenceGoal_greaterThan() {
    // Initial plant count is 200 in default configuration
    // GrowBanana adds 100
    // PickBanana removes 100
    // Between the end of the GrowBanana, and the beginning of the PickBanana, the StateConstraint is satisfied
    final var growBananaDuration = Duration.of(1, Duration.HOUR);
    final var results = runScheduler(
        BANANANATION,
        List.of(new MockMerlinService.PlannedActivityInstance(
            "GrowBanana",
            Map.of(
                "quantity", SerializedValue.of(100),
                "growingDuration", new DurationValueMapper().serializeValue(growBananaDuration)),
            Duration.of(2, Duration.HOURS)),
                new MockMerlinService.PlannedActivityInstance(
                    "PickBanana",
                    Map.of("quantity", SerializedValue.of(100)),
                    Duration.of(4, Duration.HOURS))),
        List.of(new SchedulingGoal(new GoalId(0L), """
                export default (): Goal => {
                 return Goal.CoexistenceGoal({
                   activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
                   forEach: Real.Resource("/plant").greaterThan(201.0),
                   startsAt: TimingConstraint.singleton(WindowProperty.START)
                 })
               }""", true)));

    assertEquals(1, results.scheduleResults.goalResults().size());
    assertEquals(3, results.updatedPlan().size());
    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var growBanana = planByActivityType.get("GrowBanana").iterator().next();
    final var peelBanana = planByActivityType.get("PeelBanana").iterator().next();
    final var pickBanana = planByActivityType.get("PickBanana").iterator().next();
    assertTrue(peelBanana.startTime().noShorterThan(growBanana.startTime().plus(growBananaDuration)));
    assertTrue(peelBanana.startTime().noLongerThan(pickBanana.startTime()));
    assertEquals(Map.of("peelDirection", SerializedValue.of("fromStem")), peelBanana.args());
  }

  @Test
  void testLessThan() {
    // Initial plant count is 200 in default configuration
    // GrowBanana adds 100
    // PickBanana removes 100
    // Between the end of the GrowBanana, and the beginning of the PickBanana, the StateConstraint is satisfied
    final var growBananaDuration = Duration.of(1, Duration.HOUR);
    final var results = runScheduler(
        BANANANATION,
        List.of(
            new MockMerlinService.PlannedActivityInstance(
                "PickBanana",
                Map.of("quantity", SerializedValue.of(100)),
                Duration.of(2, Duration.HOURS)),
            new MockMerlinService.PlannedActivityInstance(
                    "GrowBanana",
                    Map.of(
                        "quantity", SerializedValue.of(100),
                        "growingDuration", new DurationValueMapper().serializeValue(growBananaDuration)),
                    Duration.of(4, Duration.HOURS))),
        List.of(new SchedulingGoal(new GoalId(0L), """
                export default (): Goal => {
                 return Goal.CoexistenceGoal({
                   activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
                   forEach: Real.Resource("/plant").lessThan(199.0),
                   startsAt: TimingConstraint.singleton(WindowProperty.START)
                 })
               }""", true)));

    assertEquals(1, results.scheduleResults.goalResults().size());
    assertEquals(3, results.updatedPlan().size());
    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var growBanana = planByActivityType.get("GrowBanana").iterator().next();
    final var peelBanana = planByActivityType.get("PeelBanana").iterator().next();
    final var pickBanana = planByActivityType.get("PickBanana").iterator().next();
    assertTrue(peelBanana.startTime().noShorterThan(pickBanana.startTime()), "PeelBanana was placed at %s which is before the start/end of PickBanana at %s".formatted(peelBanana.startTime(), pickBanana.startTime()));
    assertTrue(peelBanana.startTime().noLongerThan(growBanana.startTime().plus(growBananaDuration)), "PeelBanana was placed at %s which is after the end of GrowBanana at %s".formatted(peelBanana.startTime(), growBanana.startTime()));
    assertEquals(Map.of("peelDirection", SerializedValue.of("fromStem")), peelBanana.args());
  }

  @Test
  void testLinear_atChangePoints() {
    // Initial fruit count is 4.0 in default configuration
    // BiteBanana takes away 1.0
    // The constraint should be satisfied between the two BiteBananaActivities
    final var results = runScheduler(
        BANANANATION,
        List.of(
            new MockMerlinService.PlannedActivityInstance(
                "BiteBanana",
                Map.of("biteSize", SerializedValue.of(1.0)),
                Duration.of(2, Duration.HOURS)),
            new MockMerlinService.PlannedActivityInstance(
                "BiteBanana",
                Map.of("biteSize", SerializedValue.of(1.0)),
                Duration.of(4, Duration.HOURS))
        ),

        List.of(new SchedulingGoal(new GoalId(0L), """
                export default (): Goal => {
                 return Goal.CoexistenceGoal({
                   activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
                   forEach: Windows.All(
                     Real.Resource("/fruit").lessThan(4.0),
                     Real.Resource("/fruit").greaterThan(2.0)
                   ),
                   startsAt: TimingConstraint.singleton(WindowProperty.START)
                 })
               }""", true)));

    assertEquals(1, results.scheduleResults.goalResults().size());
    assertEquals(3, results.updatedPlan().size());
    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var peelBanana = planByActivityType.get("PeelBanana").iterator().next();
    assertTrue(peelBanana.startTime().noShorterThan(Duration.of(2, Duration.HOURS)), "PeelBanana was placed at %s which is before the start/end of BiteBanana at %s".formatted(peelBanana.startTime(), Duration.of(2, Duration.HOURS)));
    assertTrue(peelBanana.startTime().noLongerThan(Duration.of(4, Duration.HOURS)), "PeelBanana was placed at %s which is before the start/end of BiteBanana at %s".formatted(peelBanana.startTime(), Duration.of(4, Duration.HOURS)));
    assertEquals(Map.of("peelDirection", SerializedValue.of("fromStem")), peelBanana.args());
  }

  @Test
  void testLinear_interpolated() {
    // Initial fruit count is 4.0 in default configuration
    // GrowBanana takes it from 4.0 to 7.0 in 3 hours
    // The constraint should be satisfied between the two BiteBananaActivities
    final var results = runScheduler(
        BANANANATION,
        List.of(
            new MockMerlinService.PlannedActivityInstance(
                "GrowBanana",
                Map.of("quantity", SerializedValue.of(3.0),
                       "growingDuration", new DurationValueMapper().serializeValue(Duration.of(3, Duration.HOURS))),
                Duration.of(1, Duration.HOURS))),

        List.of(new SchedulingGoal(new GoalId(0L), """
                export default (): Goal => {
                 return Goal.CoexistenceGoal({
                   activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromTip"}),
                   forEach: Windows.All(
                     Real.Resource("/fruit").greaterThan(5.0),
                     Real.Resource("/fruit").lessThan(6.0),
                   ),
                   startsAt: TimingConstraint.singleton(WindowProperty.START)
                 })
               }""", true)));

    assertEquals(1, results.scheduleResults.goalResults().size());
    assertEquals(2, results.updatedPlan().size());
    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var peelBanana = planByActivityType.get("PeelBanana").iterator().next();
    assertTrue(peelBanana.startTime().noShorterThan(Duration.of(2, Duration.HOURS)), "PeelBanana was placed at %s which is before the start/end of GrowBanana at %s".formatted(peelBanana.startTime(), Duration.of(2, Duration.HOURS)));
    assertTrue(peelBanana.startTime().noLongerThan(Duration.of(3, Duration.HOURS)), "PeelBanana was placed at %s which is before the start/end of GrowBanana at %s".formatted(peelBanana.startTime(), Duration.of(3, Duration.HOURS)));
    assertEquals(Map.of("peelDirection", SerializedValue.of("fromTip")), peelBanana.args());
  }

  @Test
  void testEqualTo_satsified() {
    // Initial plant count is 200 in default configuration
    // PickBanana removes 100
    // GrowBanana adds 100
    // Between the end of the PickBanana, and the beginning of the GrowBanana, the StateConstraint is satisfied
    final var growBananaDuration = Duration.of(1, Duration.HOUR);
    final var results = runScheduler(
        BANANANATION,
        List.of(
            new MockMerlinService.PlannedActivityInstance(
                "PickBanana",
                Map.of("quantity", SerializedValue.of(100)),
                Duration.of(2, Duration.HOURS)),
            new MockMerlinService.PlannedActivityInstance(
                "GrowBanana",
                Map.of(
                    "quantity", SerializedValue.of(100),
                    "growingDuration", new DurationValueMapper().serializeValue(growBananaDuration)),
                Duration.of(4, Duration.HOURS))),
        List.of(new SchedulingGoal(new GoalId(0L), """
                export default (): Goal => {
                 return Goal.CoexistenceGoal({
                   activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
                   forEach: Real.Resource("/plant").equal(100.0),
                   startsAt: TimingConstraint.singleton(WindowProperty.START)
                 })
               }""", true)));

    assertEquals(1, results.scheduleResults.goalResults().size());
    assertEquals(3, results.updatedPlan().size());
    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var growBanana = planByActivityType.get("GrowBanana").iterator().next();
    final var peelBanana = planByActivityType.get("PeelBanana").iterator().next();
    final var pickBanana = planByActivityType.get("PickBanana").iterator().next();
    assertTrue(peelBanana.startTime().noShorterThan(pickBanana.startTime()), "PeelBanana was placed at %s which is before the start/end of PickBanana at %s".formatted(peelBanana.startTime(), pickBanana.startTime()));
    assertTrue(peelBanana.startTime().noLongerThan(growBanana.startTime().plus(growBananaDuration)), "PeelBanana was placed at %s which is after the end of GrowBanana at %s".formatted(peelBanana.startTime(), growBanana.startTime()));
    assertEquals(Map.of("peelDirection", SerializedValue.of("fromStem")), peelBanana.args());
  }

  @Test
  void testEqualTo_neverSatisfied() {
    // Initial plant count is 200 in default configuration
    // PickBanana removes 99
    // GrowBanana adds 100
    // The StateConstraint is never satisfied
    final var growBananaDuration = Duration.of(1, Duration.HOUR);
    final var results = runScheduler(
        BANANANATION,
        List.of(
            new MockMerlinService.PlannedActivityInstance(
                "PickBanana",
                Map.of("quantity", SerializedValue.of(99)),
                Duration.of(2, Duration.HOURS)),
            new MockMerlinService.PlannedActivityInstance(
                "GrowBanana",
                Map.of(
                    "quantity", SerializedValue.of(100),
                    "growingDuration", new DurationValueMapper().serializeValue(growBananaDuration)),
                Duration.of(4, Duration.HOURS))),
        List.of(new SchedulingGoal(new GoalId(0L), """
                export default (): Goal => {
                 return Goal.CoexistenceGoal({
                   activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
                   forEach: Real.Resource("/plant").equal(100.0),
                   startsAt: TimingConstraint.singleton(WindowProperty.START)
                 })
               }""", true)));

    assertEquals(1, results.scheduleResults.goalResults().size());
    assertEquals(2, results.updatedPlan().size());
    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var pickBanana = planByActivityType.get("PickBanana").iterator().next();
    final var growBanana = planByActivityType.get("GrowBanana").iterator().next();
    assertEquals(Duration.of(2, Duration.HOURS), pickBanana.startTime());
    assertEquals(Duration.of(4, Duration.HOURS), growBanana.startTime());
  }

  @Test
  void testNotEqualTo_satisfied() {
    // Initial plant count is 200 in default configuration
    // PickBanana removes 100
    // GrowBanana adds 100
    // The StateConstraint is satisfied between the two
    final var growBananaDuration = Duration.of(1, Duration.HOUR);
    final var results = runScheduler(
        BANANANATION,
        List.of(
            new MockMerlinService.PlannedActivityInstance(
                "PickBanana",
                Map.of("quantity", SerializedValue.of(100)),
                Duration.of(2, Duration.HOURS)),
            new MockMerlinService.PlannedActivityInstance(
                "GrowBanana",
                Map.of(
                    "quantity", SerializedValue.of(100),
                    "growingDuration", new DurationValueMapper().serializeValue(growBananaDuration)),
                Duration.of(4, Duration.HOURS))),
        List.of(new SchedulingGoal(new GoalId(0L), """
                export default (): Goal => {
                 return Goal.CoexistenceGoal({
                   activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
                   forEach: Real.Resource("/plant").notEqual(200.0),
                   startsAt: TimingConstraint.singleton(WindowProperty.START)
                 })
               }""", true)));

    assertEquals(1, results.scheduleResults.goalResults().size());
    assertEquals(3, results.updatedPlan().size());
    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var growBanana = planByActivityType.get("GrowBanana").iterator().next();
    final var peelBanana = planByActivityType.get("PeelBanana").iterator().next();
    final var pickBanana = planByActivityType.get("PickBanana").iterator().next();
    assertTrue(peelBanana.startTime().noShorterThan(pickBanana.startTime()), "PeelBanana was placed at %s which is before the start/end of PickBanana at %s".formatted(peelBanana.startTime(), pickBanana.startTime()));
    assertTrue(peelBanana.startTime().noLongerThan(growBanana.startTime().plus(growBananaDuration)), "PeelBanana was placed at %s which is after the end of GrowBanana at %s".formatted(peelBanana.startTime(), growBanana.startTime()));
    assertEquals(Map.of("peelDirection", SerializedValue.of("fromStem")), peelBanana.args());
  }

  @Test
  void testBetweenInTermsOfAll() {
    // Initial plant count is 200 in default configuration
    // PickBanana removes 100
    // GrowBanana adds 100
    // The StateConstraint is satisfied between the two
    final var growBananaDuration = Duration.of(1, Duration.HOUR);
    final var results = runScheduler(
        BANANANATION,
        List.of(
            new MockMerlinService.PlannedActivityInstance(
                "PickBanana",
                Map.of("quantity", SerializedValue.of(100)),
                Duration.of(2, Duration.HOURS)),
            new MockMerlinService.PlannedActivityInstance(
                "GrowBanana",
                Map.of(
                    "quantity", SerializedValue.of(100),
                    "growingDuration", new DurationValueMapper().serializeValue(growBananaDuration)),
                Duration.of(4, Duration.HOURS))),
        List.of(new SchedulingGoal(new GoalId(0L), """
                export default (): Goal => {
                 return Goal.CoexistenceGoal({
                   activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
                   forEach: Windows.All(
                     Real.Resource("/plant").greaterThan(50.0),
                     Real.Resource("/plant").lessThan(150.0),
                   ),
                   startsAt: TimingConstraint.singleton(WindowProperty.START)
                 })
               }""", true)));

    assertEquals(1, results.scheduleResults.goalResults().size());
    assertEquals(3, results.updatedPlan().size());
    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var growBanana = planByActivityType.get("GrowBanana").iterator().next();
    final var peelBanana = planByActivityType.get("PeelBanana").iterator().next();
    final var pickBanana = planByActivityType.get("PickBanana").iterator().next();
    assertTrue(peelBanana.startTime().noShorterThan(pickBanana.startTime()), "PeelBanana was placed at %s which is before the start/end of PickBanana at %s".formatted(peelBanana.startTime(), pickBanana.startTime()));
    assertTrue(peelBanana.startTime().noLongerThan(growBanana.startTime().plus(growBananaDuration)), "PeelBanana was placed at %s which is after the end of GrowBanana at %s".formatted(peelBanana.startTime(), growBanana.startTime()));
    assertEquals(Map.of("peelDirection", SerializedValue.of("fromStem")), peelBanana.args());
  }

  @Test
  void testWindowsAny() {
    // Initial plant count is 200 in default configuration
    // PickBanana removes 100
    // GrowBanana adds 100
    // Between the end of the PickBanana, and the beginning of the GrowBanana, the StateConstraint is satisfied
    final var growBananaDuration = Duration.of(1, Duration.HOUR);
    final var results = runScheduler(
        BANANANATION,
        List.of(
            new MockMerlinService.PlannedActivityInstance(
                "PickBanana",
                Map.of("quantity", SerializedValue.of(100)),
                Duration.of(2, Duration.HOURS)),
            new MockMerlinService.PlannedActivityInstance(
                "GrowBanana",
                Map.of(
                    "quantity", SerializedValue.of(100),
                    "growingDuration", new DurationValueMapper().serializeValue(growBananaDuration)),
                Duration.of(4, Duration.HOURS))),
        List.of(new SchedulingGoal(new GoalId(0L), """
                export default (): Goal => {
                 return Goal.CoexistenceGoal({
                   activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
                   forEach: Windows.Any(
                     Real.Resource("/plant").equal(999.0),
                     Real.Resource("/plant").equal(100.0),
                   ),
                   startsAt: TimingConstraint.singleton(WindowProperty.START)
                 })
               }""", true)));

    assertEquals(1, results.scheduleResults.goalResults().size());
    assertEquals(3, results.updatedPlan().size());
    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var growBanana = planByActivityType.get("GrowBanana").iterator().next();
    final var peelBanana = planByActivityType.get("PeelBanana").iterator().next();
    final var pickBanana = planByActivityType.get("PickBanana").iterator().next();
    assertTrue(peelBanana.startTime().noShorterThan(pickBanana.startTime()), "PeelBanana was placed at %s which is before the start/end of PickBanana at %s".formatted(peelBanana.startTime(), pickBanana.startTime()));
    assertTrue(peelBanana.startTime().noLongerThan(growBanana.startTime().plus(growBananaDuration)), "PeelBanana was placed at %s which is after the end of GrowBanana at %s".formatted(peelBanana.startTime(), growBanana.startTime()));
    assertEquals(Map.of("peelDirection", SerializedValue.of("fromStem")), peelBanana.args());
  }

  @Test
  void testWindowsTransition() {
    // Initial producer is "Chiquita"
    // ChangeProducer sets producer to "Dole"
    // The PeelBanana should be placed at the same time as the ChangeProducer
    final var results = runScheduler(
        BANANANATION,
        List.of(
            new MockMerlinService.PlannedActivityInstance(
                "ChangeProducer",
                Map.of(),
                Duration.of(2, Duration.HOURS))),
        List.of(new SchedulingGoal(new GoalId(0L), """
                export default (): Goal => {
                 return Goal.CoexistenceGoal({
                   activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
                   forEach: Discrete.Resource(Resources["/producer"]).transition("Chiquita", "Dole"),
                   startsAt: TimingConstraint.singleton(WindowProperty.START)
                 })
               }""", true)));

    assertEquals(1, results.scheduleResults.goalResults().size());
    assertEquals(2, results.updatedPlan().size());
    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var changeProducer = planByActivityType.get("ChangeProducer").iterator().next();
    final var peelBanana = planByActivityType.get("PeelBanana").iterator().next();
    assertEquals(changeProducer.startTime(), peelBanana.startTime());
    assertEquals(Map.of("peelDirection", SerializedValue.of("fromStem")), peelBanana.args());
  }

  @Test
  void testWindowsTransition_unsatisfied() {
    // Initial producer is "Chiquita"
    // ChangeProducer sets producer to "Dole"
    // The PeelBanana should be placed at the same time as the ChangeProducer
    final var results = runScheduler(
        BANANANATION,
        List.of(
            new MockMerlinService.PlannedActivityInstance(
                "ChangeProducer",
                Map.of("producer", SerializedValue.of("Fyffes")),
                Duration.of(2, Duration.HOURS))),
        List.of(new SchedulingGoal(new GoalId(0L), """
                export default (): Goal => {
                 return Goal.CoexistenceGoal({
                   activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
                   forEach: Discrete.Resource(Resources["/producer"]).transition("Chiquita", "Dole"),
                   startsAt: TimingConstraint.singleton(WindowProperty.START)
                 })
               }""", true)));

    assertEquals(1, results.scheduleResults.goalResults().size());
    assertEquals(1, results.updatedPlan().size());
    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var changeProducer = planByActivityType.get("ChangeProducer").iterator().next();
    assertEquals("Fyffes", changeProducer.args().get("producer").asString().get());
  }

  private static Map<String, Collection<MockMerlinService.PlannedActivityInstance>>
  partitionByActivityType(final Iterable<MockMerlinService.PlannedActivityInstance> activities) {
    final var result = new HashMap<String, Collection<MockMerlinService.PlannedActivityInstance>>();
    for (final var activity : activities) {
      result
          .computeIfAbsent(activity.type(), key -> new ArrayList<>())
          .add(activity);
    }
    return result;
  }

  private static MockMerlinService.MissionModelInfo getMissionModelInfo(final MissionModelDescription desc) {
    final var jarFile = getLatestJarFile(desc.libPath());
    try {
      return new MockMerlinService.MissionModelInfo(
          desc.libPath(),
          Path.of(jarFile.getName()),
          desc.name(),
          loadMissionModelTypesFromJar(jarFile.getAbsolutePath(), desc.config()),
          desc.config());
    } catch (MissionModelLoader.MissionModelLoadException e) {
      fail(e);
      throw new IllegalStateException("unreachable due to fail() call");
    }
  }

  private static File getLatestJarFile(final Path libPath) {
    final var files = libPath.toFile().listFiles(pathname -> pathname.getName().endsWith(".jar"));
    Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
    return files[0];
  }

  private SchedulingRunResults runScheduler(
      final MissionModelDescription desc,
      final List<MockMerlinService.PlannedActivityInstance> plannedActivities,
      final Iterable<SchedulingGoal> goals
  ) {
    final var mockMerlinService = new MockMerlinService();
    mockMerlinService.setMissionModel(getMissionModelInfo(desc));
    mockMerlinService.setInitialPlan(plannedActivities);
    mockMerlinService.setPlanningHorizon(new PlanningHorizon(
        TimeUtility.fromDOY("2021-001T00:00:00"),
        TimeUtility.fromDOY("2021-005T00:00:00")));
    final var planId = new PlanId(1L);
    final var goalsByPriority = new ArrayList<GoalRecord>();

    for (final var goal : goals) {
      final var goalResult = schedulingDSLCompiler.compileSchedulingGoalDSL(mockMerlinService, planId, goal.definition());
      if (goalResult instanceof SchedulingDSLCompilationService.SchedulingDSLCompilationResult.Success s) {
        goalsByPriority.add(new GoalRecord(goal.goalId(), s.goalSpecifier(), goal.enabled()));
      } else if (goalResult instanceof SchedulingDSLCompilationService.SchedulingDSLCompilationResult.Error e) {
        fail(e.toString());
      }
    }
    final var specificationService = new MockSpecificationService(Map.of(new SpecificationId(1L), new Specification(
        planId,
        1L,
        goalsByPriority,
        Timestamp.fromString("2021-001T00:00:00"),
        Timestamp.fromString("2021-005T00:00:00"),
        Map.of())));
    final var agent = new SynchronousSchedulerAgent(
        specificationService,
        mockMerlinService,
        desc.libPath(),
        Path.of(""),
        PlanOutputMode.UpdateInputPlanWithNewActivities);
    // Scheduling Goals -> Scheduling Specification
    final var writer = new MockResultsProtocolWriter();
    agent.schedule(new ScheduleRequest(new SpecificationId(1L), $ -> RevisionData.MatchResult.success()), writer);
    assertEquals(1, writer.results.size());
    final var result = writer.results.get(0);
    if (result instanceof MockResultsProtocolWriter.Result.Failure e) {
      System.err.println(e.reason());
      fail(e.reason());
    }
    return new SchedulingRunResults(((MockResultsProtocolWriter.Result.Success) result).results(), mockMerlinService.updatedPlan);
  }

  record SchedulingRunResults(ScheduleResults scheduleResults, Collection<MockMerlinService.PlannedActivityInstance> updatedPlan) {}

  static MissionModelService.MissionModelTypes loadMissionModelTypesFromJar(
      final String jarPath,
      final Map<String, SerializedValue> configuration)
  throws MissionModelLoader.MissionModelLoadException
  {
    final var missionModel = MissionModelLoader.loadMissionModel(
        SerializedValue.of(configuration),
        Path.of(jarPath),
        "",
        "");
    final Map<String, ? extends TaskSpecType<?, ?, ?>> taskSpecTypes = missionModel.getDirectiveTypes().taskSpecTypes();
    final var activityTypes = new ArrayList<MissionModelService.ActivityType>();
    for (final var entry : taskSpecTypes.entrySet()) {
      final var activityTypeName = entry.getKey();
      final var taskSpecType = entry.getValue();
      activityTypes.add(new MissionModelService.ActivityType(
          activityTypeName,
          taskSpecType
              .getParameters()
              .stream()
              .collect(Collectors.toMap(Parameter::name, Parameter::schema))));
    }

    final var resourceTypes = new ArrayList<MissionModelService.ResourceType>();
    for (final var entry : missionModel.getResources().entrySet()) {
      final var name = entry.getKey();
      final var resource = entry.getValue();
      resourceTypes.add(new MissionModelService.ResourceType(name, resource.getSchema()));
    }

    return new MissionModelService.MissionModelTypes(activityTypes, resourceTypes);
  }
}
