package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.server.models.MissionModelId;
import gov.nasa.jpl.aerie.scheduler.server.models.PlanId;
import gov.nasa.jpl.aerie.scheduler.server.models.SchedulingDSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
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
    schedulingDSLCompilationService = new SchedulingDSLCompilationService(new TypescriptCodeGenerationService(new MissionModelService() {
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
            """);
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
            """);
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
              """);
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
              """);
    assertTrue(
        actualErrors.errors()
                    .stream()
                    .anyMatch(e -> e.message().contains("TypeError: TS2322 Incorrect return type. Expected: 'Goal', Actual: 'number'."))
    );
  }

  @Test
  void testHugeGoal() {
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
            """ + " ".repeat(9001));
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
  void testCoexistenceGoal() {
    final var result = schedulingDSLCompilationService.compileSchedulingGoalDSL(PLAN_ID, """
          export default function() {
            return Goal.CoexistenceGoal({
              activityTemplate: ActivityTemplates.SampleActivity1({
                variant: 'option2',
                fancy: { subfield1: 'value1', subfield2: [{subsubfield1: 2}]},
                duration: 60 * 60 * 1000 * 1000 // 1 hour in microseconds
              }),
              forEach: ActivityTypes.SampleActivity1,
            })
          }
        """);

    if (result instanceof SchedulingDSLCompilationService.SchedulingDSLCompilationResult.Success r) {
      assertEquals(
          new SchedulingDSL.GoalSpecifier.CoexistenceGoalDefinition(
              new SchedulingDSL.ActivityTemplate("SampleActivity1",
                                                 Map.ofEntries(
                                                     Map.entry("variant", SerializedValue.of("option2")),
                                                     Map.entry("fancy", SerializedValue.of(Map.ofEntries(
                                                         Map.entry("subfield1", SerializedValue.of("value1")),
                                                         Map.entry("subfield2", SerializedValue.of(List.of(SerializedValue.of(Map.of("subsubfield1", SerializedValue.of(2)))))
                                                         )))),
                                                     Map.entry("duration", SerializedValue.of(60L * 60 * 1000 * 1000))
                                                 )
              ),
              new SchedulingDSL.ActivityExpression("SampleActivity1")
          ),
          r.goalSpecifier()
      );
    } else if (result instanceof SchedulingDSLCompilationService.SchedulingDSLCompilationResult.Error r) {
      fail(r.toString());
    }
  }

  @Test
  void strictTypeCheckingTest() {
    final var result = schedulingDSLCompilationService.compileSchedulingGoalDSL(PLAN_ID, """
          interface FakeGoal {
            and(...others: FakeGoal[]): FakeGoal;
            or(...others: FakeGoal[]): FakeGoal;
          }
          export default function() {
            const myFakeGoal: FakeGoal = {
              and: (...others: FakeGoal[]) => {
                return myFakeGoal;
              },
              or: (...others: FakeGoal[]) => {
                return myFakeGoal;
              },
            };
            return myFakeGoal;
          }
        """);

    if (result instanceof SchedulingDSLCompilationService.SchedulingDSLCompilationResult.Error r) {
      assertEquals(r.errors().size(), 1);
      assertEquals(
          "TypeError: TS2741 Incorrect return type. Property '__astNode' is missing in type 'FakeGoal' but required in type 'Goal'.",
          r.errors().get(0).message()
      );
    }
  }
}
