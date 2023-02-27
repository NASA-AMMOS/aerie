package gov.nasa.jpl.aerie.scheduler.worker.services;

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
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.HOURS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECOND;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MINUTES;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;
import static org.junit.jupiter.api.Assertions.*;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelLoader;
import gov.nasa.jpl.aerie.merlin.protocol.model.DirectiveType;
import gov.nasa.jpl.aerie.merlin.protocol.model.InputType.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.TimeUtility;
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
import gov.nasa.jpl.aerie.scheduler.server.services.MissionModelService;
import gov.nasa.jpl.aerie.scheduler.server.services.RevisionData;
import gov.nasa.jpl.aerie.scheduler.server.services.ScheduleRequest;
import gov.nasa.jpl.aerie.scheduler.server.services.ScheduleResults;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SchedulingIntegrationTests {

  public static final PlanningHorizon PLANNING_HORIZON = new PlanningHorizon(
      TimeUtility.fromDOY("2021-001T00:00:00"),
      TimeUtility.fromDOY("2021-005T00:00:00"));

  private record MissionModelDescription(String name, Map<String, SerializedValue> config, Path libPath) {}

  private record SchedulingGoal(GoalId goalId, String definition, boolean enabled) {}

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
      final var arguments = activity.serializedActivity().getArguments();
      assertEquals("PeelBanana", activity.serializedActivity().getTypeName());
      assertEquals(SerializedValue.of("fromStem"), arguments.get("peelDirection"));
    }
  }

  @Test
  void testRecurrenceGoalNegative() {
    try {
      runScheduler(BANANANATION, List.of(), List.of(new SchedulingGoal(new GoalId(0L), """
          export default () => Goal.ActivityRecurrenceGoal({
            activityTemplate: ActivityTemplates.PeelBanana({
              peelDirection: "fromStem",
            }),
            interval: Temporal.Duration.from({ hours : -4})
          })
          """, true)), PLANNING_HORIZON);
      fail();
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
          """, true)),List.of(createAutoMutex("GrowBanana")), PLANNING_HORIZON);
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
      assertTrue(setStartTimes.remove(growBanana.startOffset()));
      assertEquals(SerializedValue.of(1), growBanana.serializedActivity().getArguments().get("quantity"));
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
                    """, true)),List.of(createAutoMutex("GrowBanana")),PLANNING_HORIZON);

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
      assertTrue(setStartTimes.remove(growBanana.startOffset()));
      assertEquals(SerializedValue.of(1), growBanana.serializedActivity().getArguments().get("quantity"));
    }
  }


  @Test
  void testSingleActivityPlanSimpleRecurrenceGoal() {
    final var results = runScheduler(
        BANANANATION,
        List.of(
            new ActivityDirective(
                Duration.ZERO,
                "BiteBanana",
                Map.of("biteSize", SerializedValue.of(1)),
                null,
                true)),
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
    assertEquals(SerializedValue.of(1), biteBanana.serializedActivity().getArguments().get("biteSize"));

    final var peelBananas = activitiesByType.get("PeelBanana");
    assertEquals(4, peelBananas.size());

    for (final var peelBanana : peelBananas) {
      assertEquals(SerializedValue.of("fromStem"), peelBanana.serializedActivity().getArguments().get("peelDirection"));
    }
  }
  @Test
  void testSingleActivityPlanSimpleCoexistenceGoalWithValueAtParams() {
    final var growBananaDuration = Duration.of(1, Duration.HOUR);
    final var results = runScheduler(
        BANANANATION,
        List.of(new ActivityDirective(
            Duration.ZERO,
            "GrowBanana",
            Map.of(
                "quantity", SerializedValue.of(3),
                "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
            null,
            true)),
        List.of(new SchedulingGoal(new GoalId(0L), """
          export default () => Goal.CoexistenceGoal({
            forEach: ActivityExpression.ofType(ActivityTypes.GrowBanana),
            activityTemplate: (growBananaActivity) => ActivityTemplates.ChangeProducer({producer: Discrete.Resource("/producer").valueAt(growBananaActivity.span().starts())}),
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
    final var changeProducers = planByActivityType.get("ChangeProducer");
    final var growBananas = planByActivityType.get("GrowBanana");
    assertEquals(1, changeProducers.size());
    assertEquals(1, growBananas.size());
    final var peelBanana = changeProducers.iterator().next();
    final var growBanana = growBananas.iterator().next();

    assertEquals(SerializedValue.of("Chiquita"), peelBanana.serializedActivity().getArguments().get("producer"));

    assertEquals(growBanana.startOffset().plus(growBananaDuration).plus(Duration.of(5, Duration.MINUTES)), peelBanana.startOffset());
  }


  @Test
  void testSingleActivityPlanSimpleCoexistenceGoalWithFunctionalParameters() {
    final var growBananaDuration = Duration.of(1, Duration.HOUR);
    final var results = runScheduler(
        BANANANATION,
        List.of(new ActivityDirective(
            Duration.ZERO,
            "GrowBanana",
            Map.of(
                "quantity", SerializedValue.of(3),
                "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
            null,
            true)),
        List.of(new SchedulingGoal(new GoalId(0L), """
          export default () => Goal.CoexistenceGoal({
            forEach: ActivityExpression.ofType(ActivityTypes.GrowBanana),
            activityTemplate: (growBananaActivity) => ActivityTemplates.PickBanana({quantity: growBananaActivity.parameters.quantity}),
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
    final var peelBananas = planByActivityType.get("PickBanana");
    final var growBananas = planByActivityType.get("GrowBanana");
    assertEquals(1, peelBananas.size());
    assertEquals(1, growBananas.size());
    final var peelBanana = peelBananas.iterator().next();
    final var growBanana = growBananas.iterator().next();

    assertEquals(SerializedValue.of(3), peelBanana.serializedActivity().getArguments().get("quantity"));

    assertEquals(growBanana.startOffset().plus(growBananaDuration).plus(Duration.of(5, Duration.MINUTES)), peelBanana.startOffset());
  }

  @Test
  void testSingleActivityPlanSimpleCoexistenceGoal() {
    final var growBananaDuration = Duration.of(1, Duration.HOUR);
    final var results = runScheduler(
        BANANANATION,
        List.of(
            new ActivityDirective(
                Duration.ZERO,
                "GrowBanana",
                Map.of(
                    "quantity", SerializedValue.of(1),
                    "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
                null,
                true)),
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

    assertEquals(SerializedValue.of("fromStem"), peelBanana.serializedActivity().getArguments().get("peelDirection"));
    assertEquals(SerializedValue.of(1), growBanana.serializedActivity().getArguments().get("quantity"));

    assertEquals(growBanana.startOffset().plus(growBananaDuration).plus(Duration.of(5, Duration.MINUTES)), peelBanana.startOffset());
  }

  @Test
  void testSingleActivityPlanSimpleCoexistenceGoal_constrainEndTime() {
    final var growBananaDuration = Duration.of(1, Duration.HOUR);
    final var results = runScheduler(
        BANANANATION,
        List.of(
            new ActivityDirective(
                Duration.of(5, Duration.MINUTES),
                "GrowBanana",
                Map.of(
                    "quantity", SerializedValue.of(1),
                    "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
                null,
                true)),
        List.of(new SchedulingGoal(new GoalId(0L), """
          export default () => Goal.CoexistenceGoal({
            forEach: ActivityExpression.ofType(ActivityTypes.GrowBanana),
            activityTemplate: (span) => ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
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

    assertEquals(SerializedValue.of("fromStem"), peelBanana.serializedActivity().getArguments().get("peelDirection"));
    assertEquals(SerializedValue.of(1), growBanana.serializedActivity().getArguments().get("quantity"));

    assertTrue(growBanana.startOffset().plus(growBananaDuration).plus(Duration.of(5, Duration.MINUTES)).noShorterThan(peelBanana.startOffset()));
    assertTrue(growBanana.startOffset().plus(growBananaDuration).noLongerThan(peelBanana.startOffset()));
  }

  @Test
  void testStateCoexistenceGoal_greaterThan() {
    // Initial plant count is 200 in default configuration
    // GrowBanana adds 100
    // PickBanana removes 100
    // Between the end of the GrowBanana, and the beginning of the PickBanana, the StateConstraint is satisfied
    final var growBananaDuration = Duration.of(1, Duration.HOUR);
    final var results = runScheduler(BANANANATION, List.of(
        new ActivityDirective(
            Duration.of(2, HOURS),
            "GrowBanana",
            Map.of(
                "quantity", SerializedValue.of(100),
                "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
            null,
            true),
        new ActivityDirective(
            Duration.of(4, HOURS),
            "PickBanana",
            Map.of("quantity", SerializedValue.of(100)),
            null,
            true)), List.of(new SchedulingGoal(new GoalId(0L), """
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
    assertTrue(peelBanana.startOffset().noShorterThan(growBanana.startOffset().plus(growBananaDuration)));
    assertTrue(peelBanana.startOffset().noLongerThan(pickBanana.startOffset()));
    assertEquals(Map.of("peelDirection", SerializedValue.of("fromStem")), peelBanana.serializedActivity().getArguments());
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
            new ActivityDirective(
                Duration.of(2, HOURS),
                "PickBanana",
                Map.of("quantity", SerializedValue.of(100)),
                null,
                true),
            new ActivityDirective(
                Duration.of(4, HOURS),
                "GrowBanana",
                Map.of(
                    "quantity", SerializedValue.of(100),
                    "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
                null,
                true)),
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
    assertTrue(peelBanana.startOffset().noShorterThan(pickBanana.startOffset()), "PeelBanana was placed at %s which is before the start/end of PickBanana at %s".formatted(peelBanana.startOffset(), pickBanana.startOffset()));
    assertTrue(peelBanana.startOffset().noLongerThan(growBanana.startOffset().plus(growBananaDuration)), "PeelBanana was placed at %s which is after the end of GrowBanana at %s".formatted(peelBanana.startOffset(), growBanana.startOffset()));
    assertEquals(Map.of("peelDirection", SerializedValue.of("fromStem")), peelBanana.serializedActivity().getArguments());
  }

  @Test
  void testLinear_atChangePoints() {
    // Initial fruit count is 4.0 in default configuration
    // BiteBanana takes away 1.0
    // The constraint should be satisfied between the two BiteBananaActivities
    final var results = runScheduler(BANANANATION, List.of(
        new ActivityDirective(
            Duration.of(2, HOURS),
            "BiteBanana",
            Map.of("biteSize", SerializedValue.of(1.0)),
            null,
            true),
        new ActivityDirective(
            Duration.of(4, HOURS),
            "BiteBanana",
            Map.of("biteSize", SerializedValue.of(1.0)),
            null,
            true)
    ), List.of(new SchedulingGoal(new GoalId(0L), """
         export default (): Goal => {
          return Goal.CoexistenceGoal({
            activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
            forEach: Windows.And(
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
    assertTrue(peelBanana.startOffset().noShorterThan(Duration.of(2, HOURS)), "PeelBanana was placed at %s which is before the start/end of BiteBanana at %s".formatted(peelBanana.startOffset(), Duration.of(2, HOURS)));
    assertTrue(peelBanana.startOffset().noLongerThan(Duration.of(4, HOURS)), "PeelBanana was placed at %s which is before the start/end of BiteBanana at %s".formatted(peelBanana.startOffset(), Duration.of(4, HOURS)));
    assertEquals(Map.of("peelDirection", SerializedValue.of("fromStem")), peelBanana.serializedActivity().getArguments());
  }

  @Test
  void testLinear_interpolated() {
    // Initial fruit count is 4.0 in default configuration
    // GrowBanana takes it from 4.0 to 7.0 in 3 hours
    // The constraint should be satisfied between the two BiteBananaActivities
    final var results = runScheduler(
        BANANANATION,
        List.of(
            new ActivityDirective(
                Duration.of(1, HOURS),
                "GrowBanana",
                Map.of("quantity", SerializedValue.of(3.0),
                       "growingDuration", SerializedValue.of(Duration.of(3, HOURS).in(Duration.MICROSECONDS))),
                null,
                true)),

        List.of(new SchedulingGoal(new GoalId(0L), """
                export default (): Goal => {
                 return Goal.CoexistenceGoal({
                   activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromTip"}),
                   forEach: Windows.And(
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
    assertTrue(peelBanana.startOffset().noShorterThan(Duration.of(2, HOURS)), "PeelBanana was placed at %s which is before the start/end of GrowBanana at %s".formatted(peelBanana.startOffset(), Duration.of(2, HOURS)));
    assertTrue(peelBanana.startOffset().noLongerThan(Duration.of(3, HOURS)), "PeelBanana was placed at %s which is before the start/end of GrowBanana at %s".formatted(peelBanana.startOffset(), Duration.of(3, HOURS)));
    assertEquals(Map.of("peelDirection", SerializedValue.of("fromTip")), peelBanana.serializedActivity().getArguments());
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
            new ActivityDirective(
                Duration.of(2, HOURS),
                "PickBanana",
                Map.of("quantity", SerializedValue.of(100)),
                null,
                true),
            new ActivityDirective(
                Duration.of(4, HOURS),
                "GrowBanana",
                Map.of(
                    "quantity", SerializedValue.of(100),
                    "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
                null,
                true)),
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
    assertTrue(peelBanana.startOffset().noShorterThan(pickBanana.startOffset()), "PeelBanana was placed at %s which is before the start/end of PickBanana at %s".formatted(peelBanana.startOffset(), pickBanana.startOffset()));
    assertTrue(peelBanana.startOffset().noLongerThan(growBanana.startOffset().plus(growBananaDuration)), "PeelBanana was placed at %s which is after the end of GrowBanana at %s".formatted(peelBanana.startOffset(), growBanana.startOffset()));
    assertEquals(Map.of("peelDirection", SerializedValue.of("fromStem")), peelBanana.serializedActivity().getArguments());
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
            new ActivityDirective(
                Duration.of(2, HOURS),
                "PickBanana",
                Map.of("quantity", SerializedValue.of(99)),
                null,
                true),
            new ActivityDirective(
                Duration.of(4, HOURS),
                "GrowBanana",
                Map.of(
                    "quantity", SerializedValue.of(100),
                    "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
                null,
                true)),
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
    assertEquals(Duration.of(2, HOURS), pickBanana.startOffset());
    assertEquals(Duration.of(4, HOURS), growBanana.startOffset());
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
            new ActivityDirective(
                Duration.of(2, HOURS),
                "PickBanana",
                Map.of("quantity", SerializedValue.of(100)),
                null,
                true),
            new ActivityDirective(
                Duration.of(4, HOURS),
                "GrowBanana",
                Map.of(
                    "quantity", SerializedValue.of(100),
                    "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
                null,
                true)),
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
    assertTrue(peelBanana.startOffset().noShorterThan(pickBanana.startOffset()), "PeelBanana was placed at %s which is before the start/end of PickBanana at %s".formatted(peelBanana.startOffset(), pickBanana.startOffset()));
    assertTrue(peelBanana.startOffset().noLongerThan(growBanana.startOffset().plus(growBananaDuration)), "PeelBanana was placed at %s which is after the end of GrowBanana at %s".formatted(peelBanana.startOffset(), growBanana.startOffset()));
    assertEquals(Map.of("peelDirection", SerializedValue.of("fromStem")), peelBanana.serializedActivity().getArguments());
  }

  @Test
  void testBetweenInTermsOfAnd() {
    // Initial plant count is 200 in default configuration
    // PickBanana removes 100
    // GrowBanana adds 100
    // The StateConstraint is satisfied between the two
    final var growBananaDuration = Duration.of(1, Duration.HOUR);
    final var results = runScheduler(
        BANANANATION,
        List.of(
            new ActivityDirective(
                Duration.of(2, HOURS),
                "PickBanana",
                Map.of("quantity", SerializedValue.of(100)),
                null,
                true),
            new ActivityDirective(
                Duration.of(4, HOURS),
                "GrowBanana",
                Map.of(
                    "quantity", SerializedValue.of(100),
                    "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
                null,
                true)),
        List.of(new SchedulingGoal(new GoalId(0L), """
                export default (): Goal => {
                 return Goal.CoexistenceGoal({
                   activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
                   forEach: Windows.And(
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
    assertTrue(peelBanana.startOffset().noShorterThan(pickBanana.startOffset()), "PeelBanana was placed at %s which is before the start/end of PickBanana at %s".formatted(peelBanana.startOffset(), pickBanana.startOffset()));
    assertTrue(peelBanana.startOffset().noLongerThan(growBanana.startOffset().plus(growBananaDuration)), "PeelBanana was placed at %s which is after the end of GrowBanana at %s".formatted(peelBanana.startOffset(), growBanana.startOffset()));
    assertEquals(Map.of("peelDirection", SerializedValue.of("fromStem")), peelBanana.serializedActivity().getArguments());
  }

  @Test
  void testWindowsOr() {
    // Initial plant count is 200 in default configuration
    // PickBanana removes 100
    // GrowBanana adds 100
    // Between the end of the PickBanana, and the beginning of the GrowBanana, the StateConstraint is satisfied
    final var growBananaDuration = Duration.of(1, Duration.HOUR);
    final var results = runScheduler(
        BANANANATION,
        List.of(
            new ActivityDirective(
                Duration.of(2, HOURS),
                "PickBanana",
                Map.of("quantity", SerializedValue.of(100)),
                null,
                true),
            new ActivityDirective(
                Duration.of(4, HOURS),
                "GrowBanana",
                Map.of(
                    "quantity", SerializedValue.of(100),
                    "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
                null,
                true)),
        List.of(new SchedulingGoal(new GoalId(0L), """
                export default (): Goal => {
                 return Goal.CoexistenceGoal({
                   activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
                   forEach: Windows.Or(
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
    assertTrue(peelBanana.startOffset().noShorterThan(pickBanana.startOffset()), "PeelBanana was placed at %s which is before the start/end of PickBanana at %s".formatted(peelBanana.startOffset(), pickBanana.startOffset()));
    assertTrue(peelBanana.startOffset().noLongerThan(growBanana.startOffset().plus(growBananaDuration)), "PeelBanana was placed at %s which is after the end of GrowBanana at %s".formatted(peelBanana.startOffset(), growBanana.startOffset()));
    assertEquals(Map.of("peelDirection", SerializedValue.of("fromStem")), peelBanana.serializedActivity().getArguments());
  }

  @Test
  void testWindowsTransition() {
    // Initial producer is "Chiquita"
    // ChangeProducer sets producer to "Dole"
    // The PeelBanana should be placed at the same time as the ChangeProducer
    final var results = runScheduler(BANANANATION, List.of(
        new ActivityDirective(
            Duration.of(2, HOURS),
            "ChangeProducer",
            Map.of(),
            null,
            true)), List.of(new SchedulingGoal(new GoalId(0L), """
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
    assertEquals(changeProducer.startOffset(), peelBanana.startOffset());
    assertEquals(Map.of("peelDirection", SerializedValue.of("fromStem")), peelBanana.serializedActivity().getArguments());
  }

  @Test
  void testWindowsTransition_unsatisfied() {
    // Initial producer is "Chiquita"
    // ChangeProducer sets producer to "Dole"
    // The PeelBanana should be placed at the same time as the ChangeProducer
    final var results = runScheduler(BANANANATION, List.of(
        new ActivityDirective(
            Duration.of(2, HOURS),
            "ChangeProducer",
            Map.of("producer", SerializedValue.of("Fyffes")),
            null,
            true)), List.of(new SchedulingGoal(new GoalId(0L), """
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
    assertEquals("Fyffes", changeProducer.serializedActivity().getArguments().get("producer").asString().get());
  }

  @Test
  void testApplyWhen() {
    final var growBananaDuration = Duration.of(1, Duration.SECONDS);

    final var results = runScheduler(
        BANANANATION,
        List.of(
            new ActivityDirective(
                Duration.of(1, Duration.SECONDS),
                "GrowBanana",
                Map.of(
                    "quantity", SerializedValue.of(1),
                    "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
                null,
                true),
            new ActivityDirective(
                Duration.of(2, Duration.SECONDS),
                "GrowBanana",
                Map.of(
                    "quantity", SerializedValue.of(1),
                    "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
                null,
                true),
            new ActivityDirective(
                Duration.of(3, Duration.SECONDS),
                "GrowBanana",
                Map.of(
                    "quantity", SerializedValue.of(1),
                    "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
                null,
                true),
            new ActivityDirective(
                Duration.of(48, HOURS),
                "PickBanana",
                Map.of(
                    "quantity", SerializedValue.of(100000)
                ),
                null,
                true)
        ),
        List.of(new SchedulingGoal(new GoalId(0L), """
                  export default () => Goal.ActivityRecurrenceGoal({
                      activityTemplate: ActivityTemplates.ChangeProducer({producer: "Morpheus"}),
                      interval: Temporal.Duration.from({ hours : 24 })
                    }).applyWhen(Real.Resource("/plant").greaterThan(1.0))""", true)
        ),
        PLANNING_HORIZON
    );

    for (ActivityDirective i : results.updatedPlan().stream().toList()) {
      System.out.println(i.serializedActivity().getTypeName() + ": " + i.startOffset().toString());
    }

    assertEquals(1, results.scheduleResults.goalResults().size()); //starts an instant before 12:00 (when there is more than 1 plant - the interval is about 00:00-24:00 because the plant gets created early, then the interval is subdivided, and then a interval is picked, seemingly arbitrarily, as this is uncontrollable duration (see line 304 of ActivityCreationTemplate), after which cadence is maintained) then schedules 4, as expected. 7 total then.
    assertEquals(6, results.updatedPlan().size());

    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var changeProducer = planByActivityType.get("ChangeProducer").iterator().next();
    assertEquals("Morpheus", changeProducer.serializedActivity().getArguments().get("producer").asString().get());
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
                new GlobalSchedulingConditionSource("""
                export default () => GlobalSchedulingCondition.scheduleActivitiesOnlyWhen(Real.Resource(\"/fruit\").greaterThan(5.0))
                """),
                true
            ),
            createAutoMutex("ChangeProducer")
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
                new GlobalSchedulingConditionSource("export default () => GlobalSchedulingCondition.scheduleActivitiesOnlyWhen(Real.Resource(\"/fruit\").lessThan(5.0))"),
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
            new ActivityDirective(
                Duration.of(24, HOURS).minus(MICROSECOND),
                "BiteBanana",
                Map.of("biteSize", SerializedValue.of(1)),
                null,
                true)
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
                new GlobalSchedulingConditionSource("export default () => GlobalSchedulingCondition.scheduleActivitiesOnlyWhen(Real.Resource(\"/fruit\").greaterThan(3.5))"),
                true
            )
        ),
        PLANNING_HORIZON);
    assertEquals(2, results.updatedPlan().size());
  }

  private static Map<String, Collection<ActivityDirective>>
  partitionByActivityType(final Iterable<ActivityDirective> activities) {
    final var result = new HashMap<String, Collection<ActivityDirective>>();
    for (final var activity : activities) {
      result
          .computeIfAbsent(activity.serializedActivity().getTypeName(), key -> new ArrayList<>())
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

  public static GlobalSchedulingConditionRecord createAutoMutex(String activityType){
    return new GlobalSchedulingConditionRecord(
        new GlobalSchedulingConditionSource("""
                    export default function myCondition() {
                      return GlobalSchedulingCondition.mutex([ActivityTypes.%s], [ActivityTypes.%s])
                    }
                    """.formatted(activityType, activityType)),
        true
    );
  }

  private SchedulingRunResults runScheduler(
      final MissionModelDescription desc,
      final List<ActivityDirective> plannedActivities,
      final Iterable<SchedulingGoal> goals,
      final PlanningHorizon planningHorizon
  )
  {
    final var activities = new HashMap<ActivityDirectiveId, ActivityDirective>();
    long id = 0L;
    for (final var activityDirective : plannedActivities) {
      activities.put(new ActivityDirectiveId(id++), activityDirective);
    }
    return runScheduler(desc, activities, goals, List.of(), planningHorizon);
  }

  private SchedulingRunResults runScheduler(
      final MissionModelDescription desc,
      final Map<ActivityDirectiveId, ActivityDirective> plannedActivities,
      final Iterable<SchedulingGoal> goals,
      final PlanningHorizon planningHorizon
  )
  {
    return runScheduler(desc, plannedActivities, goals, List.of(), planningHorizon);
  }


  private SchedulingRunResults runScheduler(
      final MissionModelDescription desc,
      final List<ActivityDirective> plannedActivities,
      final Iterable<SchedulingGoal> goals,
      final List<GlobalSchedulingConditionRecord> globalSchedulingConditions,
      final PlanningHorizon planningHorizon
  ){
    final var activities = new HashMap<ActivityDirectiveId, ActivityDirective>();
    long id = 0L;
    for (final var activityDirective : plannedActivities) {
      activities.put(new ActivityDirectiveId(id++), activityDirective);
    }
    return runScheduler(desc, activities, goals, globalSchedulingConditions, planningHorizon);
  }

  private SchedulingRunResults runScheduler(
      final MissionModelDescription desc,
      final Map<ActivityDirectiveId, ActivityDirective> plannedActivities,
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

  record SchedulingRunResults(ScheduleResults scheduleResults, Collection<ActivityDirective> updatedPlan) {}

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
            new ActivityDirective(
                Duration.of(1, Duration.MINUTES),
                "PickBanana",
                Map.of("quantity", SerializedValue.of(99)),
                null,
                true),
            new ActivityDirective(
                Duration.of(15, Duration.MINUTES),
                "GrowBanana",
                Map.of(
                    "quantity", SerializedValue.of(100),
                    "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
                null,
                true)),
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
        List.of(
            createAutoMutex("GrowBanana"),
            createAutoMutex("PeelBanana")
        ),
        planningHorizon);

    assertEquals(1, results.scheduleResults.goalResults().size());
    assertEquals(4, results.updatedPlan().size());
    assertFalse(results.scheduleResults.goalResults().entrySet().iterator().next().getValue().satisfied());
    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var pickBanana = planByActivityType.get("PickBanana").iterator().next();
    final var peelBanana = planByActivityType.get("PeelBanana").iterator().next();
    final var growStartTimes = new HashSet<Duration>();
    planByActivityType.get("GrowBanana").forEach(activity -> growStartTimes.add(activity.startOffset()));
    assertEquals(Duration.of(1, Duration.MINUTES), pickBanana.startOffset());
    assertEquals(Duration.of(1, Duration.MINUTES), peelBanana.startOffset());
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
            new ActivityDirective(
                Duration.of(2, Duration.MINUTES),
                "PickBanana",
                Map.of("quantity", SerializedValue.of(99)),
                null,
                true),
            new ActivityDirective(
                Duration.of(4, Duration.MINUTES),
                "GrowBanana",
                Map.of(
                    "quantity", SerializedValue.of(100),
                    "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
                null,
                true)),
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
        List.of(
            createAutoMutex("GrowBanana"),
            createAutoMutex("PeelBanana")
        ),
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
            new ActivityDirective(
                Duration.of(2, HOURS),
                "PickBanana",
                Map.of("quantity", SerializedValue.of(99)),
                null,
                true),
            new ActivityDirective(
                Duration.of(4, HOURS),
                "GrowBanana",
                Map.of(
                    "quantity", SerializedValue.of(100),
                    "growingDuration", SerializedValue.of(growBananaDuration.in(Duration.MICROSECONDS))),
                null,
                true)),
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
        List.of(
            createAutoMutex("GrowBanana"),
            createAutoMutex("PeelBanana")
        ),
        PLANNING_HORIZON);

    assertEquals(1, results.scheduleResults.goalResults().size());
    assertTrue(results.scheduleResults.goalResults().entrySet().iterator().next().getValue().satisfied());
    assertEquals(3, results.updatedPlan().size());
    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var pickBanana = planByActivityType.get("PickBanana").iterator().next();
    final var growBanana = planByActivityType.get("GrowBanana").iterator().next();
    final var peelBanana = planByActivityType.get("PeelBanana").iterator().next();
    assertEquals(Duration.of(2, HOURS), pickBanana.startOffset());
    assertEquals(Duration.of(4, HOURS), peelBanana.startOffset());
    assertEquals(Duration.of(4, HOURS), growBanana.startOffset());
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
                     new ActivityDirective(
                         Duration.of(1, HOURS),
                         "parent",
                         Map.of(),
                         null,
                         true)),
                 List.of(new SchedulingGoal(new GoalId(0L), goalDefinition, true)),
                 planningHorizon);
    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var biteBanana = planByActivityType.get("BiteBanana").stream().map((bb) -> bb.startOffset()).toList();
    final var childs = planByActivityType.get("child");
    assertEquals(childs.size(), biteBanana.size());
    assertEquals(childs.size(), 2);
    for(final var childAct: childs){
      assertTrue(biteBanana.contains(childAct.startOffset()));
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
                                         new ActivityDirective(
                                             Duration.of(1, HOURS),
                                             "parent",
                                             Map.of(),
                                             null,
                                             true)),
                                     List.of(new SchedulingGoal(new GoalId(0L), goalDefinition, true)),
                                     List.of(createAutoMutex("child")),
                                     planningHorizon);
    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    final var parentActs = planByActivityType.get("parent");
    final var childActs = planByActivityType.get("child").stream().map((bb) -> bb.startOffset()).toList();
    //ensure no new child activity has been inserted
    assertEquals(childActs.size(), 2);
    //ensure no new parent activity has been inserted
    assertEquals(parentActs.size(), 1);
    for(final var parentAct: parentActs){
      assertTrue(childActs.contains(parentAct.startOffset()));
    }
  }

  @Test
  void testDurationParameter() {
    final var results = runScheduler(
        BANANANATION,
        List.of(),
        List.of(new SchedulingGoal(new GoalId(0L), """
        export default function myGoal() {
          return Goal.ActivityRecurrenceGoal({
            activityTemplate: ActivityTemplates.DurationParameterActivity({
              duration: Temporal.Duration.from({seconds: 1}),
            }),
            interval: Temporal.Duration.from({hours: 1})
          })
        }
          """, true)),
        PLANNING_HORIZON);
    assertEquals(96, results.updatedPlan().size());
  }

  /**
   * If you passed activities without duration to the scheduler in an initial plan, it would fail
   */
  @Test
  public void testBugDurationInMicroseconds(){
    final var results = runScheduler(
        BANANANATION,
        List.of(),
        List.of(new SchedulingGoal(new GoalId(0L), """
            export default (): Goal =>
              Goal.ActivityRecurrenceGoal({
                activityTemplate: ActivityTemplates.BakeBananaBread({ temperature: 325.0, tbSugar: 2, glutenFree: false }),
                interval: Temporal.Duration.from({ hours: 12 }),
              });
            """, true)),
        PLANNING_HORIZON);
    runScheduler(
        BANANANATION,
        results.updatedPlan.stream().toList(),
        List.of(new SchedulingGoal(new GoalId(0L), """
            export default (): Goal =>
              Goal.ActivityRecurrenceGoal({
                activityTemplate: ActivityTemplates.BakeBananaBread({ temperature: 325.0, tbSugar: 2, glutenFree: false }),
                interval: Temporal.Duration.from({ hours: 12 }),
              });
            """, true)),
        PLANNING_HORIZON);
    assertEquals(8, results.updatedPlan().size());
  }

  @Test
  void test_inf_loop(){
    final var results = runScheduler(
        BANANANATION,
        List.of(new ActivityDirective(
            Duration.of(23, HOURS),
            "BiteBanana",
            Map.of("biteSize", SerializedValue.of(10)),
            null,
            true)),
        List.of(new SchedulingGoal(new GoalId(0L), """
              export default (): Goal =>
                 Goal.ActivityRecurrenceGoal({
                   activityTemplate: ActivityTemplates.BakeBananaBread({ temperature: 325.0, tbSugar: 2, glutenFree: false }),
                   interval: Temporal.Duration.from({ hours: 2 }),
                 });
            """, true)),
        List.of(new GlobalSchedulingConditionRecord(new GlobalSchedulingConditionSource(
            """
        export default function myFirstSchedulingCondition(): GlobalSchedulingCondition {
          return GlobalSchedulingCondition.scheduleActivitiesOnlyWhen(Real.Resource('/fruit').lessThan(3.0));
        }
        """
        ), true)),
        new PlanningHorizon(
            TimeUtility.fromDOY("2022-318T00:00:00"),
            TimeUtility.fromDOY("2022-319T00:00:00")));
    assertEquals(2, results.updatedPlan().size());
  }

  @Test
  void testRelativeActivityPlanSimpleCoexistenceGoalEnd() {
    /*
    Start with a plan with B -> A
    Goal: for each B, place a C
    And make sure that C ends up in the right place
     */
    final var activityDuration = Duration.of(1, Duration.HOUR);
    final var results = runScheduler(
        BANANANATION,
        Map.of(
            new ActivityDirectiveId(1L),
            new ActivityDirective(
                Duration.ZERO,
                "DurationParameterActivity",
                Map.of(
                    "duration", SerializedValue.of(activityDuration.in(Duration.MICROSECONDS))),
                null,
                true),
            new ActivityDirectiveId(2L),
            new ActivityDirective(
                Duration.ZERO,
                "GrowBanana",
                Map.of(
                    "quantity", SerializedValue.of(1),
                    "growingDuration", SerializedValue.of(activityDuration.in(Duration.MICROSECONDS))),
                new ActivityDirectiveId(1L),
                false),
            new ActivityDirectiveId(3L),
            new ActivityDirective(
                Duration.of(5, MINUTES),
                "ControllableDurationActivity",
                Map.of(
                    "duration", SerializedValue.of(activityDuration.in(Duration.MICROSECONDS))),
                new ActivityDirectiveId(2L),
                false)),
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
    final var durationParamActivities = planByActivityType.get("DurationParameterActivity");
    final var controllableDurationActivities = planByActivityType.get("ControllableDurationActivity");

    assertEquals(1, peelBananas.size());
    assertEquals(1, growBananas.size());
    assertEquals(1, durationParamActivities.size());
    assertEquals(1, controllableDurationActivities.size());
    final var peelBanana = peelBananas.iterator().next();
    final var growBanana = growBananas.iterator().next();
    final var durationParamActivity = durationParamActivities.iterator().next();
    final var controllableDuration = controllableDurationActivities.iterator().next();

    assertEquals(Duration.ZERO, durationParamActivity.startOffset());
    assertEquals(Duration.ZERO, growBanana.startOffset());
    assertEquals(Duration.of(5, MINUTES), controllableDuration.startOffset());
    assertEquals(SerializedValue.of(1), growBanana.serializedActivity().getArguments().get("quantity"));
    assertEquals(SerializedValue.of("fromStem"), peelBanana.serializedActivity().getArguments().get("peelDirection"));
    assertEquals(Duration.of(125, Duration.MINUTES), peelBanana.startOffset());
  }

  @Test
  void testRelativeActivityPlanSimpleCoexistenceGoalStart() {
    /*
    Start with a plan with B -> A
    Goal: for each B, place a C
    And make sure that C ends up in the right place
     */
    final var activityDuration = Duration.of(1, Duration.HOUR);
    final var results = runScheduler(
        BANANANATION,
        Map.of(
            new ActivityDirectiveId(1L),
            new ActivityDirective(
                Duration.ZERO,
                "DurationParameterActivity",
                Map.of(
                    "duration", SerializedValue.of(activityDuration.in(Duration.MICROSECONDS))),
                null,
                true),
            new ActivityDirectiveId(2L),
            new ActivityDirective(
                Duration.ZERO,
                "GrowBanana",
                Map.of(
                    "quantity", SerializedValue.of(1),
                    "growingDuration", SerializedValue.of(activityDuration.in(Duration.MICROSECONDS))),
                new ActivityDirectiveId(1L),
                false),
            new ActivityDirectiveId(3L),
            new ActivityDirective(
                Duration.of(5, MINUTES),
                "ControllableDurationActivity",
                Map.of(
                    "duration", SerializedValue.of(activityDuration.in(Duration.MICROSECONDS))),
                new ActivityDirectiveId(2L),
                false)),
        List.of(new SchedulingGoal(new GoalId(0L), """
          export default () => Goal.CoexistenceGoal({
            forEach: ActivityExpression.ofType(ActivityTypes.GrowBanana),
            activityTemplate: ActivityTemplates.PeelBanana({peelDirection: "fromStem"}),
            startsAt: TimingConstraint.singleton(WindowProperty.START).plus(Temporal.Duration.from({ minutes : 5}))
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
    final var durationParamActivities = planByActivityType.get("DurationParameterActivity");
    final var controllableDurationActivities = planByActivityType.get("ControllableDurationActivity");

    assertEquals(1, peelBananas.size());
    assertEquals(1, growBananas.size());
    assertEquals(1, durationParamActivities.size());
    assertEquals(1, controllableDurationActivities.size());
    final var peelBanana = peelBananas.iterator().next();
    final var growBanana = growBananas.iterator().next();
    final var durationParamActivity = durationParamActivities.iterator().next();
    final var controllableDuration = controllableDurationActivities.iterator().next();

    assertEquals(Duration.ZERO, durationParamActivity.startOffset());
    assertEquals(Duration.ZERO, growBanana.startOffset());
    assertEquals(Duration.of(5, MINUTES), controllableDuration.startOffset());
    assertEquals(SerializedValue.of(1), growBanana.serializedActivity().getArguments().get("quantity"));
    assertEquals(SerializedValue.of("fromStem"), peelBanana.serializedActivity().getArguments().get("peelDirection"));
    assertEquals(Duration.of(65, Duration.MINUTES), peelBanana.startOffset());
  }

  private static final MissionModelDescription FOO = new MissionModelDescription(
      "foo",
      Map.of(),
      Path.of(System.getenv("AERIE_ROOT"), "examples", "foo-missionmodel", "build", "libs")
  );

  /**
   * Test that the daemon task did not get dropped.
   * If the daemon was dropped, then DaemonCheckerActivity will throw a RuntimeException when simulated.
   */
  @Test
  void daemonTaskTest(){
    final PlanningHorizon PLANNING_HORIZON = new PlanningHorizon(
        TimeUtility.fromDOY("2030-001T00:00:00"),
        TimeUtility.fromDOY("2030-001T02:00:00"));
    final var results = runScheduler(
        FOO,
        List.of(
            new ActivityDirective(
                Duration.of(5, MINUTES),
                "ZeroDurationUncontrollableActivity",
                Map.of(),
                null,
                true)
        ),
        List.of(new SchedulingGoal(new GoalId(0L), """
      export default () => Goal.CoexistenceGoal({
        forEach: ActivityExpression.ofType(ActivityTypes.ZeroDurationUncontrollableActivity),
        activityTemplate: ActivityTemplates.DaemonCheckerActivity({
          "minutesElapsed": 10
        }),
        startsAt: TimingConstraint.singleton(WindowProperty.START).plus(Temporal.Duration.from({ minutes: 5, seconds: 1 })),
        });
      """, true)), PLANNING_HORIZON);

    assertEquals(1, results.scheduleResults.goalResults().size());
    assertTrue(results.scheduleResults.goalResults().get(new GoalId(0L)).satisfied());

    assertEquals(2, results.updatedPlan.size());

    final var planByActivityType = partitionByActivityType(results.updatedPlan());
    assertEquals(2, planByActivityType.size());

    final var zeroDurations = planByActivityType.get("ZeroDurationUncontrollableActivity");
    final var daemonCheckers = planByActivityType.get("DaemonCheckerActivity");
    assertEquals(1, zeroDurations.size());
    assertEquals(1, daemonCheckers.size());

    final var zeroDuration = zeroDurations.iterator().next();
    final var daemonChecker = daemonCheckers.iterator().next();

    assertEquals(Duration.of(5, MINUTES), zeroDuration.startOffset());
    assertEquals(Duration.of(10, MINUTES).plus(Duration.of(1, SECOND)), daemonChecker.startOffset());
  }
}
