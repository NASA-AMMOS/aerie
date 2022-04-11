package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.model.ActivityInstance;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.Problem;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityInstanceId;
import gov.nasa.jpl.aerie.scheduler.server.models.MerlinPlan;
import gov.nasa.jpl.aerie.scheduler.server.models.MissionModelId;
import gov.nasa.jpl.aerie.scheduler.server.models.PlanId;
import gov.nasa.jpl.aerie.scheduler.server.models.PlanMetadata;
import gov.nasa.jpl.aerie.scheduler.server.models.SchedulingDSL;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static gov.nasa.jpl.aerie.scheduler.server.services.TypescriptCodeGenerationServiceTest.MISSION_MODEL_TYPES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchedulingDSLCompilationServiceTests {
  private static final PlanId PLAN_ID = new PlanId(1L);
  SchedulingDSLCompilationService schedulingDSLCompilationService;

  @BeforeAll
  void setUp() throws IOException {
    schedulingDSLCompilationService = new SchedulingDSLCompilationService(new TypescriptCodeGenerationService(new MerlinService() {
      @Override
      public long getPlanRevision(final PlanId planId) {
        return 0;
      }

      @Override
      public PlanMetadata getPlanMetadata(final PlanId planId) {
        return null;
      }

      @Override
      public MerlinPlan getPlanActivities(final PlanMetadata planMetadata, final Problem mission)
      {
        return null;
      }

      @Override
      public Pair<PlanId, Map<ActivityInstance, ActivityInstanceId>> createNewPlanWithActivities(
          final PlanMetadata planMetadata,
          final Plan plan)
      {
        return null;
      }

      @Override
      public PlanId createEmptyPlan(
          final String name,
          final long modelId,
          final Instant startTime,
          final Duration duration)
      {
        return null;
      }

      @Override
      public void createSimulationForPlan(final PlanId planId) {

      }

      @Override
      public Map<ActivityInstance, ActivityInstanceId> updatePlanActivities(final PlanId planId,
                                                                            final Map<SchedulingActivityInstanceId, ActivityInstanceId> idsFromInitialPlan,
                                                                            final MerlinPlan initialPlan,
                                                                            final Plan plan)
      {
        return null;
      }

      @Override
      public void ensurePlanExists(final PlanId planId) {

      }

      @Override
      public void clearPlanActivities(final PlanId planId) {

      }

      @Override
      public Map<ActivityInstance, ActivityInstanceId> createAllPlanActivities(final PlanId planId, final Plan plan)
      {
        return null;
      }

      @Override
      public TypescriptCodeGenerationService.MissionModelTypes getMissionModelTypes(final PlanId missionModelId)
      {
        return MISSION_MODEL_TYPES;
      }

      @Override
      public TypescriptCodeGenerationService.MissionModelTypes getMissionModelTypes(final MissionModelId missionModelId)
      {
        return MISSION_MODEL_TYPES;
      }
    }));
  }

  @AfterAll
  void tearDown() {
    schedulingDSLCompilationService.close();
  }

  @Test
  void testSchedulingDSL_basic()
  {
    final SchedulingDSLCompilationService.SchedulingDSLCompilationResult result;
    result = schedulingDSLCompilationService.compileSchedulingGoalDSL(
        PLAN_ID, """
                export default function myGoal() {
                  return Goal.ActivityRecurrenceGoal({
                    activityTemplate: ActivityTemplates.SampleActivity1({
                      variant: 'option2',
                      fancy: { subfield1: 'value1', subfield2: [{subsubfield1: 2}]},
                      duration: 60 * 60 * 1000 * 1000 // 1 hour in microseconds
                    }),
                    interval: 60 * 60 * 1000 * 1000 // 1 hour in microseconds
                  })
                }
            """, "goalfile");
    final var expectedGoalDefinition = new SchedulingDSL.GoalSpecifier.RecurrenceGoalDefinition(
        new SchedulingDSL.ActivityTemplate(
            "SampleActivity1",
            Map.ofEntries(
                Map.entry("variant", SerializedValue.of("option2")),
                Map.entry("fancy", SerializedValue.of(Map.ofEntries(
                    Map.entry("subfield1", SerializedValue.of("value1")),
                    Map.entry("subfield2", SerializedValue.of(List.of(SerializedValue.of(Map.of("subsubfield1", SerializedValue.of(2)))))
                )))),
                Map.entry("duration", SerializedValue.of(60L * 60 * 1000 * 1000))
            )
        ),
        Duration.HOUR
    );
    if (result instanceof SchedulingDSLCompilationService.SchedulingDSLCompilationResult.Success r) {
      assertEquals(expectedGoalDefinition, r.goalSpecifier());
    } else if (result instanceof SchedulingDSLCompilationService.SchedulingDSLCompilationResult.Error r) {
      fail(r.toString());
    }
  }

  @Test
  void testSchedulingDSL_helper_function()
  {
    final SchedulingDSLCompilationService.SchedulingDSLCompilationResult result;
    result = schedulingDSLCompilationService.compileSchedulingGoalDSL(
        PLAN_ID, """
                export default function myGoal() {
                  return myHelper(ActivityTemplates.SampleActivity1({
                    variant: 'option2',
                    fancy: { subfield1: 'value1', subfield2: [{subsubfield1: 2}]},
                    duration: 60 * 60 * 1000 * 1000 // 1 hour in microseconds
                  }))
                }
                function myHelper(activityTemplate) {
                  return Goal.ActivityRecurrenceGoal({
                    activityTemplate,
                    interval: 60 * 60 * 1000 * 1000 // 1 hour in microseconds
                  })
                }
            """, "goalfile");
    final var expectedGoalDefinition = new SchedulingDSL.GoalSpecifier.RecurrenceGoalDefinition(
        new SchedulingDSL.ActivityTemplate(
            "SampleActivity1",
            Map.ofEntries(
                Map.entry("variant", SerializedValue.of("option2")),
                Map.entry("fancy", SerializedValue.of(Map.ofEntries(
                    Map.entry("subfield1", SerializedValue.of("value1")),
                    Map.entry("subfield2", SerializedValue.of(List.of(SerializedValue.of(Map.of("subsubfield1", SerializedValue.of(2)))))
                    )))),
                Map.entry("duration", SerializedValue.of(60L * 60 * 1000 * 1000))
            )
        ),
        Duration.HOUR);
    if (result instanceof SchedulingDSLCompilationService.SchedulingDSLCompilationResult.Success r) {
      assertEquals(expectedGoalDefinition, r.goalSpecifier());
    } else if (result instanceof SchedulingDSLCompilationService.SchedulingDSLCompilationResult.Error r) {
      fail(r.toString());
    }
  }

  @Test
  void testSchedulingDSL_variable_not_defined() {
    final SchedulingDSLCompilationService.SchedulingDSLCompilationResult.Error actualErrors;
    actualErrors = (SchedulingDSLCompilationService.SchedulingDSLCompilationResult.Error) schedulingDSLCompilationService.compileSchedulingGoalDSL(
          PLAN_ID, """
                export default function myGoal() {
                  const x = 4 - 2
                  return myHelper(ActivityTemplates.SampleActivity1({
                    variant: 'option2',
                    fancy: { subfield1: 'value1', subfield2: [{subsubfield1: 2}]},
                    duration: 60 * 60 * 1000 * 1000 // 1 hour in microseconds
                  }))
                }
                function myHelper(activityTemplate) {
                  return Goal.ActivityRecurrenceGoal({
                    activityTemplate,
                    interval: x // 1 hour in microseconds
                  })
                }
              """, "goalfile_with_type_error");
    assertTrue(
        actualErrors.errors()
                    .stream()
                    .anyMatch(e -> e.message().contains("TypeError: TS2304 Cannot find name 'x'."))
    );
  }

  @Test
  void testSchedulingDSL_wrong_return_type() {
    final SchedulingDSLCompilationService.SchedulingDSLCompilationResult.Error actualErrors;
    actualErrors = (SchedulingDSLCompilationService.SchedulingDSLCompilationResult.Error) schedulingDSLCompilationService.compileSchedulingGoalDSL(
          PLAN_ID, """
                export default function myGoal() {
                  return 5
                }
              """, "goalfile_with_wrong_return_type");
    assertTrue(
        actualErrors.errors()
                    .stream()
                    .anyMatch(e -> e.message().contains("TypeError: TS2322 Incorrect return type. Expected: 'Goal', Actual: 'number'."))
    );
  }

  @Test
  void testHugeGoal()
  {
    // This test is intended to create a Goal that is bigger than the node subprocess's standard input buffer
    final SchedulingDSLCompilationService.SchedulingDSLCompilationResult result;
    result = schedulingDSLCompilationService.compileSchedulingGoalDSL(
        PLAN_ID, """
                export default function myGoal() {
                  return Goal.ActivityRecurrenceGoal({
                    activityTemplate: ActivityTemplates.SampleActivity1({
                      variant: 'option2',
                      fancy: { subfield1: 'value1', subfield2: [{subsubfield1: 2}]},
                      duration: 60 * 60 * 1000 * 1000 // 1 hour in microseconds
                    }),
                    interval: 60 * 60 * 1000 * 1000 // 1 hour in microseconds
                  })
                }
            """ + " ".repeat(9001), "goalfile");
    final var expectedGoalDefinition = new SchedulingDSL.GoalSpecifier.RecurrenceGoalDefinition(
        new SchedulingDSL.ActivityTemplate(
            "SampleActivity1",
            Map.ofEntries(
                Map.entry("variant", SerializedValue.of("option2")),
                Map.entry("fancy", SerializedValue.of(Map.ofEntries(
                    Map.entry("subfield1", SerializedValue.of("value1")),
                    Map.entry("subfield2", SerializedValue.of(List.of(SerializedValue.of(Map.of("subsubfield1", SerializedValue.of(2)))))
                    )))),
                Map.entry("duration", SerializedValue.of(60L * 60 * 1000 * 1000))
            )
        ),
        Duration.HOUR
    );
    if (result instanceof SchedulingDSLCompilationService.SchedulingDSLCompilationResult.Success r) {
      assertEquals(expectedGoalDefinition, r.goalSpecifier());
    } else if (result instanceof SchedulingDSLCompilationService.SchedulingDSLCompilationResult.Error r) {
      fail(r.toString());
    }
  }
}
