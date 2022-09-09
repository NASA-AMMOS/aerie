package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.merlin.driver.MissionModelLoader;
import gov.nasa.jpl.aerie.merlin.protocol.model.DirectiveType;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.TimeUtility;
import gov.nasa.jpl.aerie.scheduler.model.ActivityTypeList;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.server.config.PlanOutputMode;
import gov.nasa.jpl.aerie.scheduler.server.http.SchedulerParsers;
import gov.nasa.jpl.aerie.scheduler.server.models.GlobalSchedulingConditionRecord;
import gov.nasa.jpl.aerie.scheduler.server.models.GlobalSchedulingConditionSource;
import gov.nasa.jpl.aerie.scheduler.server.models.GoalId;
import gov.nasa.jpl.aerie.scheduler.server.models.GoalRecord;
import gov.nasa.jpl.aerie.scheduler.server.models.GoalSource;
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
import java.time.Instant;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SchedulingIntegrationTests {

  public static final PlanningHorizon PLANNING_HORIZON = new PlanningHorizon(
      TimeUtility.fromDOY("2021-001T00:00:00"),
      TimeUtility.fromDOY("2021-005T00:00:00"));

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
    this.schedulingDSLCompiler = new SchedulingDSLCompilationService();
  }

  @Test
  void testEmptyPlanEmptySpecification() {
    final var results = runScheduler(BANANANATION, List.of(), List.of(), PLANNING_HORIZON);
    assertEquals(Map.of(), results.scheduleResults.goalResults());
  }

  @Test
  void testEmptyPlanSimpleRecurrenceGoal() {
    final var results = runScheduler(BANANANATION, List.of(), List.of(new SchedulingGoal(new GoalId(0L), """
        export default () => Goal.ActivityRecurrenceGoal({
          activityTemplate: ActivityTemplates.PeelBanana({
            peelDirection: "fromStem",
          }),
          interval: Temporal.Duration.from({ milliseconds: 24 * 60 * 60 * 1000 })
        })
        """, true)), PLANNING_HORIZON);
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
  void testRecurrenceGoalNegative() {
    try {
      final var results = runScheduler(BANANANATION, List.of(), List.of(new SchedulingGoal(new GoalId(0L), """
          export default () => Goal.ActivityRecurrenceGoal({
            activityTemplate: ActivityTemplates.PeelBanana({
              peelDirection: "fromStem",
            }),
            interval: Temporal.Duration.from({ hours : -4})
          })
          """, true)), PLANNING_HORIZON);
    }
    catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("Duration passed to RecurrenceGoal as the goal's minimum recurrence interval cannot be negative!"));
    }
    catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  void testEmptyPlanDurationCardinalityGoal() {
    final var results = runScheduler(BANANANATION, List.of(), List.of(new SchedulingGoal(new GoalId(0L), """
        export default function myGoal() {
                          return Goal.CardinalityGoal({
                            activityTemplate: ActivityTemplates.GrowBanana({
                              quantity: 1,
                              growingDuration: Temporal.Duration.from({seconds: 1}),
                            }),
                            specification : {duration: Temporal.Duration.from({seconds: 10})}
                          })
        }
          """, true)), PLANNING_HORIZON);
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
                                        growingDuration: Temporal.Duration.from({seconds: 1}),
                                      }),
                                      specification : {occurrence: 10}
                                    })
                  }
                    """, true)), PLANNING_HORIZON);

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
        List.of(new MockMerlinService.PlannedActivityInstance("BiteBanana",
                                                              Map.of("biteSize", SerializedValue.of(1)),
                                                              Duration.ZERO)),
        List.of(new SchedulingGoal(new GoalId(0L), """
            export default () => Goal.ActivityRecurrenceGoal({
              activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
              interval: Temporal.Duration.from({days: 1})
            })
            """, true)),
        PLANNING_HORIZON);

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
                "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
            Duration.ZERO)),
        List.of(new SchedulingGoal(new GoalId(0L), """
          export default () => Goal.CoexistenceGoal({
            forEach: ActivityExpression.ofType(ActivityTypes.GrowBanana),
            activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
            startsAt: TimingConstraint.singleton(WindowProperty.END).plus(Temporal.Duration.from({ minutes : 5}))
          })
          """, true)),
        PLANNING_HORIZON);

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
                "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
            Duration.of(5, Duration.MINUTES))),
        List.of(new SchedulingGoal(new GoalId(0L), """
          export default () => Goal.CoexistenceGoal({
            forEach: ActivityExpression.ofType(ActivityTypes.GrowBanana),
            activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
            endsWithin: TimingConstraint.range(WindowProperty.END, Operator.PLUS, Temporal.Duration.from( { minutes: 5 }))
          })
          """, true)),
        PLANNING_HORIZON);

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
    final var results = runScheduler(BANANANATION, List.of(
        new MockMerlinService.PlannedActivityInstance(
            "GrowBanana",
            Map.of(
                "quantity", SerializedValue.of(100),
                "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
            Duration.of(2, Duration.HOURS)),
        new MockMerlinService.PlannedActivityInstance(
            "PickBanana",
            Map.of("quantity", SerializedValue.of(100)),
            Duration.of(4, Duration.HOURS))), List.of(new SchedulingGoal(new GoalId(0L), """
         export default (): Goal => {
          return Goal.CoexistenceGoal({
            activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
            forEach: Real.Resource("/plant").greaterThan(201.0),
            startsAt: TimingConstraint.singleton(WindowProperty.START)
          })
        }""", true)), PLANNING_HORIZON);

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
                        "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
                    Duration.of(4, Duration.HOURS))),
        List.of(new SchedulingGoal(new GoalId(0L), """
                export default (): Goal => {
                 return Goal.CoexistenceGoal({
                   activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
                   forEach: Real.Resource("/plant").lessThan(199.0),
                   startsAt: TimingConstraint.singleton(WindowProperty.START)
                 })
               }""", true)),
        PLANNING_HORIZON);

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
    final var results = runScheduler(BANANANATION, List.of(
        new MockMerlinService.PlannedActivityInstance(
            "BiteBanana",
            Map.of("biteSize", SerializedValue.of(1.0)),
            Duration.of(2, Duration.HOURS)),
        new MockMerlinService.PlannedActivityInstance(
            "BiteBanana",
            Map.of("biteSize", SerializedValue.of(1.0)),
            Duration.of(4, Duration.HOURS))
    ), List.of(new SchedulingGoal(new GoalId(0L), """
         export default (): Goal => {
          return Goal.CoexistenceGoal({
            activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
            forEach: Windows.All(
              Real.Resource("/fruit").lessThan(4.0),
              Real.Resource("/fruit").greaterThan(2.0)
            ),
            startsAt: TimingConstraint.singleton(WindowProperty.START)
          })
        }""", true)), PLANNING_HORIZON);

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
                       "growingDuration", SerializedValue.of(Duration.of(3, Duration.HOURS).in(Duration.MICROSECONDS))),
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
               }""", true)),
        PLANNING_HORIZON);

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
                    "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
                Duration.of(4, Duration.HOURS))),
        List.of(new SchedulingGoal(new GoalId(0L), """
                export default (): Goal => {
                 return Goal.CoexistenceGoal({
                   activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
                   forEach: Real.Resource("/plant").equal(100.0),
                   startsAt: TimingConstraint.singleton(WindowProperty.START)
                 })
               }""", true)),
        PLANNING_HORIZON);

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
                    "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
                Duration.of(4, Duration.HOURS))),
        List.of(new SchedulingGoal(new GoalId(0L), """
                export default (): Goal => {
                 return Goal.CoexistenceGoal({
                   activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
                   forEach: Real.Resource("/plant").equal(100.0),
                   startsAt: TimingConstraint.singleton(WindowProperty.START)
                 })
               }""", true)),
        PLANNING_HORIZON);

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
                    "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
                Duration.of(4, Duration.HOURS))),
        List.of(new SchedulingGoal(new GoalId(0L), """
                export default (): Goal => {
                 return Goal.CoexistenceGoal({
                   activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
                   forEach: Real.Resource("/plant").notEqual(200.0),
                   startsAt: TimingConstraint.singleton(WindowProperty.START)
                 })
               }""", true)),
        PLANNING_HORIZON);

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
                    "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
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
               }""", true)),
        PLANNING_HORIZON);

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
                    "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
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
               }""", true)),
        PLANNING_HORIZON);

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
    final var results = runScheduler(BANANANATION, List.of(
        new MockMerlinService.PlannedActivityInstance(
            "ChangeProducer",
            Map.of(),
            Duration.of(2, Duration.HOURS))), List.of(new SchedulingGoal(new GoalId(0L), """
         export default (): Goal => {
          return Goal.CoexistenceGoal({
            activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
            forEach: Discrete.Resource(Resources["/producer"]).transition("Chiquita", "Dole"),
            startsAt: TimingConstraint.singleton(WindowProperty.START)
          })
        }""", true)), PLANNING_HORIZON);

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
    final var results = runScheduler(BANANANATION, List.of(
        new MockMerlinService.PlannedActivityInstance(
            "ChangeProducer",
            Map.of("producer", SerializedValue.of("Fyffes")),
            Duration.of(2, Duration.HOURS))), List.of(new SchedulingGoal(new GoalId(0L), """
         export default (): Goal => {
          return Goal.CoexistenceGoal({
            activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
            forEach: Discrete.Resource(Resources["/producer"]).transition("Chiquita", "Dole"),
            startsAt: TimingConstraint.singleton(WindowProperty.START)
          })
        }""", true)), PLANNING_HORIZON);

    assertEquals(1, results.scheduleResults.goalResults().size());
    assertEquals(1, results.updatedPlan().size());
    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var changeProducer = planByActivityType.get("ChangeProducer").iterator().next();
    assertEquals("Fyffes", changeProducer.args().get("producer").asString().get());
  }

  @Test
  void testApplyWhen() {
    final var growBananaDuration = Duration.of(1, Duration.SECONDS);

    final var results = runScheduler(
        BANANANATION,
        List.of(
            new MockMerlinService.PlannedActivityInstance(
                "GrowBanana",
                Map.of(
                    "quantity", SerializedValue.of(1),
                    "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
                Duration.of(1, Duration.SECONDS)),
            new MockMerlinService.PlannedActivityInstance(
                "GrowBanana",
                Map.of(
                    "quantity", SerializedValue.of(1),
                    "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
                Duration.of(2, Duration.SECONDS)),
            new MockMerlinService.PlannedActivityInstance(
                "GrowBanana",
                Map.of(
                    "quantity", SerializedValue.of(1),
                    "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
                Duration.of(3, Duration.SECONDS)),
            new MockMerlinService.PlannedActivityInstance(
                "PickBanana",
                Map.of(
                    "quantity", SerializedValue.of(100000)
                ),
                Duration.of(48, Duration.HOURS))
        ),
        List.of(new SchedulingGoal(new GoalId(0L), """
                  export default () => Goal.ActivityRecurrenceGoal({
                      activityTemplate: ActivityTemplates.ChangeProducer({producer: "Morpheus"}),
                      interval: Temporal.Duration.from({ hours : 24 })
                    }).applyWhen(Real.Resource("/plant").greaterThan(1.0))""", true)
        ),
        PLANNING_HORIZON
    );

    for (MockMerlinService.PlannedActivityInstance i : results.updatedPlan().stream().toList()) {
      System.out.println(i.type().toString() + ": " + i.startTime().toString());
    }

    assertEquals(1, results.scheduleResults.goalResults().size()); //starts an instant before 12:00 (when there is more than 1 plant - the interval is about 00:00-24:00 because the plant gets created early, then the interval is subdivided, and then a interval is picked, seemingly arbitrarily, as this is uncontrollable duration (see line 304 of ActivityCreationTemplate), after which cadence is maintained) then schedules 4, as expected. 7 total then.
    assertEquals(6, results.updatedPlan().size());

    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var changeProducer = planByActivityType.get("ChangeProducer").iterator().next();
    assertEquals("Morpheus", changeProducer.args().get("producer").asString().get());
  }

  @Test
  void testGlobalSchedulingConditions_conditionNeverOccurs() {
    final var results = runScheduler(
        BANANANATION,
        List.of(),
        List.of(new SchedulingGoal(new GoalId(0L), """
          export default () => Goal.ActivityRecurrenceGoal({
            activityTemplate: ActivityTemplates.ChangeProducer({producer: "Morpheus"}),
            interval: Temporal.Duration.from({days: 1})
          })
          """, true)
        ),
        List.of(
            new GlobalSchedulingConditionRecord(
                new GlobalSchedulingConditionSource("export default () => Real.Resource(\"/fruit\").greaterThan(5.0)"),
                ActivityTypeList.matchAny(),
                true
            )
        ),
        PLANNING_HORIZON);
    assertEquals(0, results.updatedPlan().size());
  }

  @Test
  void testGlobalSchedulingConditions_conditionAlwaysTrue() {
    final var results = runScheduler(
        BANANANATION,
        List.of(),
        List.of(new SchedulingGoal(new GoalId(0L), """
          export default () => Goal.ActivityRecurrenceGoal({
            activityTemplate: ActivityTemplates.ChangeProducer({producer: "Morpheus"}),
            interval: Temporal.Duration.from({days: 1})
          })
          """, true)
        ),
        List.of(
            new GlobalSchedulingConditionRecord(
                new GlobalSchedulingConditionSource("export default () => Real.Resource(\"/fruit\").lessThan(5.0)"),
                ActivityTypeList.matchAny(),
                true
            )
        ),
        PLANNING_HORIZON);
    assertEquals(4, results.updatedPlan().size());
  }

  @Test
  void testGlobalSchedulingConditions_conditionSometimesTrue() {
    final var results = runScheduler(
        BANANANATION,
        List.of(
            new MockMerlinService.PlannedActivityInstance(
                "BiteBanana",
                Map.of("biteSize", SerializedValue.of(1)),
                Duration.of(24L * 60 * 60 * 1000 * 1000 - 1, Duration.MICROSECONDS))
        ),
        List.of(new SchedulingGoal(new GoalId(0L), """
          export default () => Goal.ActivityRecurrenceGoal({
            activityTemplate: ActivityTemplates.ChangeProducer({producer: "Morpheus"}),
            interval: Temporal.Duration.from({days: 1})
          })
          """, true)
        ),
        List.of(
            new GlobalSchedulingConditionRecord(
                new GlobalSchedulingConditionSource("export default () => Real.Resource(\"/fruit\").greaterThan(3.5)"),
                ActivityTypeList.matchAny(),
                true
            )
        ),
        PLANNING_HORIZON);
    assertEquals(2, results.updatedPlan().size());
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
      final Iterable<SchedulingGoal> goals,
      final PlanningHorizon planningHorizon
  )
  {
    return runScheduler(desc, plannedActivities, goals, List.of(), planningHorizon);
  }

  private SchedulingRunResults runScheduler(
      final MissionModelDescription desc,
      final List<MockMerlinService.PlannedActivityInstance> plannedActivities,
      final Iterable<SchedulingGoal> goals,
      final List<GlobalSchedulingConditionRecord> globalSchedulingConditions,
      final PlanningHorizon planningHorizon
  ) {
    final var mockMerlinService = new MockMerlinService();
    mockMerlinService.setMissionModel(getMissionModelInfo(desc));
    mockMerlinService.setInitialPlan(plannedActivities);
    mockMerlinService.setPlanningHorizon(planningHorizon);
    final var planId = new PlanId(1L);
    final var goalsByPriority = new ArrayList<GoalRecord>();

    for (final var goal : goals) {
      goalsByPriority.add(new GoalRecord(goal.goalId(), new GoalSource(goal.definition()), goal.enabled()));
    }
    final var specificationService = new MockSpecificationService(Map.of(new SpecificationId(1L), new Specification(
        planId,
        1L,
        goalsByPriority,
        new Timestamp(planningHorizon.getStartInstant()),
        new Timestamp(planningHorizon.getEndInstant()),
        Map.of(),
        false,
        globalSchedulingConditions)));
    final var agent = new SynchronousSchedulerAgent(
        specificationService,
        mockMerlinService,
        mockMerlinService,
        desc.libPath(),
        Path.of(""),
        PlanOutputMode.UpdateInputPlanWithNewActivities,
        schedulingDSLCompiler);
    // Scheduling Goals -> Scheduling Specification
    final var writer = new MockResultsProtocolWriter();
    agent.schedule(new ScheduleRequest(new SpecificationId(1L), $ -> RevisionData.MatchResult.success()), writer);
    assertEquals(1, writer.results.size());
    final var result = writer.results.get(0);
    if (result instanceof MockResultsProtocolWriter.Result.Failure e) {
      final var serializedReason = SchedulerParsers.scheduleFailureP.unparse(e.reason()).toString();
      System.err.println(serializedReason);
      fail(serializedReason);
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
        Instant.EPOCH,
        SerializedValue.of(configuration),
        Path.of(jarPath),
        "",
        "");
    final Map<String, ? extends DirectiveType<?, ?, ?>> taskSpecTypes = missionModel.getDirectiveTypes().directiveTypes();
    final var activityTypes = new ArrayList<MissionModelService.ActivityType>();
    for (final var entry : taskSpecTypes.entrySet()) {
      final var activityTypeName = entry.getKey();
      final var taskSpecType = entry.getValue();
      activityTypes.add(new MissionModelService.ActivityType(
          activityTypeName,
          taskSpecType
              .getInputType()
              .getParameters()
              .stream()
              .collect(Collectors.toMap(Parameter::name, Parameter::schema))));
    }

    final var resourceTypes = new ArrayList<MissionModelService.ResourceType>();
    for (final var entry : missionModel.getResources().entrySet()) {
      final var name = entry.getKey();
      final var resource = entry.getValue();
      resourceTypes.add(new MissionModelService.ResourceType(name, resource.getOutputType().getSchema()));
    }

    return new MissionModelService.MissionModelTypes(activityTypes, resourceTypes);
  }

  @Test
  void testAndFailure(){
    //The cardinality goal is not satisfiable but the partial satisfaction is on by default so as the second subgoal is satisfied
    //the composite goal is not satified but does not backtrack
    final var growBananaDuration = Duration.of(1, Duration.MINUTES);
    final var planningHorizon = new PlanningHorizon(TimeUtility.fromDOY("2021-001T00:00:00"),
        TimeUtility.fromDOY("2021-001T01:00:00"));
    final var results = runScheduler(
        BANANANATION,
        List.of(
            new MockMerlinService.PlannedActivityInstance(
                "PickBanana",
                Map.of("quantity", SerializedValue.of(99)),
                Duration.of(1, Duration.MINUTES)),
            new MockMerlinService.PlannedActivityInstance(
                "GrowBanana",
                Map.of(
                    "quantity", SerializedValue.of(100),
                    "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
                Duration.of(15, Duration.MINUTES))),
        List.of(new SchedulingGoal(new GoalId(0L), """
                export default (): Goal => {
                 return Goal.CardinalityGoal({
                                      activityTemplate: ActivityTemplates.GrowBanana({
                                        quantity: 1,
                                        growingDuration: Temporal.Duration.from({ minutes : 30 }),
                                      }),
                                      specification : {duration: Temporal.Duration.from({ hours: 4 })}
                                      })
                        .and(
                           Goal.CoexistenceGoal({
                             activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
                             forEach: ActivityExpression.ofType(ActivityTypes.PickBanana),
                             startsAt: TimingConstraint.singleton(WindowProperty.START)
                           })
                        )
               }""", true)),
        planningHorizon);

    assertEquals(1, results.scheduleResults.goalResults().size());
    assertEquals(4, results.updatedPlan().size());
    assertFalse(results.scheduleResults.goalResults().entrySet().iterator().next().getValue().satisfied());
    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var pickBanana = planByActivityType.get("PickBanana").iterator().next();
    final var itGrowBanana = planByActivityType.get("GrowBanana").iterator();
    final var peelBanana = planByActivityType.get("PeelBanana").iterator().next();
    final var growStartTimes = new HashSet<Duration>();
    planByActivityType.get("GrowBanana").forEach(activity -> growStartTimes.add(activity.startTime()));
    assertEquals(Duration.of(1, Duration.MINUTES), pickBanana.startTime());
    assertEquals(Duration.of(1, Duration.MINUTES), peelBanana.startTime());
    assertTrue(growStartTimes.contains(Duration.of(15, Duration.MINUTES)));
    assertTrue(growStartTimes.contains(Duration.of(16, Duration.MINUTES)));
  }

  @Test
  void testOrFailure(){
    //the two goals are unsatisfiable so the supergoal will not appear satisfied.
    //partial satisfaction is activated by default so the solver does not backtrack on insertion of activities
    final var growBananaDuration = Duration.of(1, Duration.MINUTES);
    final var planningHorizon = new PlanningHorizon(TimeUtility.fromDOY("2021-001T00:00:00"),
                                                    TimeUtility.fromDOY("2021-001T01:00:00"));
    final var results = runScheduler(
        BANANANATION,
        List.of(
            new MockMerlinService.PlannedActivityInstance(
                "PickBanana",
                Map.of("quantity", SerializedValue.of(99)),
                Duration.of(2, Duration.MINUTES)),
            new MockMerlinService.PlannedActivityInstance(
                "GrowBanana",
                Map.of(
                    "quantity", SerializedValue.of(100),
                    "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
                Duration.of(4, Duration.MINUTES))),
        List.of(new SchedulingGoal(new GoalId(0L), """
                export default (): Goal => {
                 return Goal.CardinalityGoal({
                                      activityTemplate: ActivityTemplates.GrowBanana({
                                        quantity: 1,
                                       growingDuration: Temporal.Duration.from({ minutes : 15 }),
                                      }),
                                      specification : { duration: Temporal.Duration.from({ hours : 4 }) }
                                      })
                        .or(
                           Goal.CardinalityGoal({
                                      activityTemplate: ActivityTemplates.GrowBanana({
                                        quantity: 1,
                                        growingDuration: Temporal.Duration.from({ minutes : 30}),
                                      }),
                                      specification : {duration: Temporal.Duration.from({ hours : 4 })}
                                      })
                        )
               }""", true)),
        planningHorizon);
    assertEquals(1, results.scheduleResults.goalResults().size());
    //as goal is partially satisfiable, it does not remove activities from the plan
    assertEquals(5, results.updatedPlan().size());
    assertFalse(results.scheduleResults.goalResults().entrySet().iterator().next().getValue().satisfied());
  }

  @Test
  void testOr(){
    //the first subgoal is satisfied which prevents the second subgoal to be looked at
    //overall, the or goal is satisfied.
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
                    "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
                Duration.of(4, Duration.HOURS))),
        List.of(new SchedulingGoal(new GoalId(0L), """
                export default (): Goal => {
                 return Goal.CoexistenceGoal({
                             activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
                             forEach: ActivityExpression.ofType(ActivityTypes.GrowBanana),
                             startsAt: TimingConstraint.singleton(WindowProperty.START)
                           })
                        .or(
                           Goal.CoexistenceGoal({
                             activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
                             forEach: ActivityExpression.ofType(ActivityTypes.PickBanana),
                             startsAt: TimingConstraint.singleton(WindowProperty.START)
                           })
                        )
               }""", true)),
        PLANNING_HORIZON);

    assertEquals(1, results.scheduleResults.goalResults().size());
    assertTrue(results.scheduleResults.goalResults().entrySet().iterator().next().getValue().satisfied());
    assertEquals(3, results.updatedPlan().size());
    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var pickBanana = planByActivityType.get("PickBanana").iterator().next();
    final var growBanana = planByActivityType.get("GrowBanana").iterator().next();
    final var peelBanana = planByActivityType.get("PeelBanana").iterator().next();
    assertEquals(Duration.of(2, Duration.HOURS), pickBanana.startTime());
    assertEquals(Duration.of(4, Duration.HOURS), peelBanana.startTime());
    assertEquals(Duration.of(4, Duration.HOURS), growBanana.startTime());
  }

  @Test
  void testTreeActivities(){
    final var goalDefinition = """
        export default function myGoal() {
                          return Goal.CoexistenceGoal({
                            forEach: ActivityExpression.ofType(ActivityType.child),
                            activityTemplate: ActivityTemplates.BiteBanana({
                              biteSize: 1,
                            }),
                            startsAt:TimingConstraint.singleton(WindowProperty.START)
                          })
                        }
        """;
    final var planningHorizon = new PlanningHorizon(TimeUtility.fromDOY("2021-001T00:00:00"),
                                                    TimeUtility.fromDOY("2021-200T01:00:00"));
    final var results = runScheduler(BANANANATION,
                 List.of(
                     new MockMerlinService.PlannedActivityInstance(
                         "parent",
                         Map.of(),
                         Duration.of(1, Duration.HOURS))),
                 List.of(new SchedulingGoal(new GoalId(0L), goalDefinition, true)),
                 planningHorizon);
    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var biteBanana = planByActivityType.get("BiteBanana").stream().map((bb) -> bb.startTime()).toList();
    final var childs = planByActivityType.get("child");
    assertEquals(childs.size(), biteBanana.size());
    assertEquals(childs.size(), 2);
    for(final var childAct: childs){
      assertTrue(biteBanana.contains(childAct.startTime()));
    }
  }

  @Test
  void testAutoSatisfaction(){
    final var goalDefinition = """
        export default function myGoal() {
                          return Goal.CoexistenceGoal({
                            forEach: ActivityExpression.ofType(ActivityType.parent),
                            activityTemplate: ActivityTemplates.child({
                              counter: 0,
                            }),
                            startsAt:TimingConstraint.singleton(WindowProperty.START)
                          })
                        }
        """;
    final var planningHorizon = new PlanningHorizon(TimeUtility.fromDOY("2021-001T00:00:00"),
                                                    TimeUtility.fromDOY("2021-200T01:00:00"));
    final var results = runScheduler(BANANANATION,
                                     List.of(
                                         new MockMerlinService.PlannedActivityInstance(
                                             "parent",
                                             Map.of(),
                                             Duration.of(1, Duration.HOURS))),
                                     List.of(new SchedulingGoal(new GoalId(0L), goalDefinition, true)),
                                     planningHorizon);
    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var parentActs = planByActivityType.get("parent");
    final var childActs = planByActivityType.get("child").stream().map((bb) -> bb.startTime()).toList();
    //ensure no new child activity has been inserted
    assertEquals(childActs.size(), 2);
    //ensure no new parent activity has been inserted
    assertEquals(parentActs.size(), 1);
    for(final var parentAct: parentActs){
      assertTrue(childActs.contains(parentAct.startTime()));
    }
  }

}
