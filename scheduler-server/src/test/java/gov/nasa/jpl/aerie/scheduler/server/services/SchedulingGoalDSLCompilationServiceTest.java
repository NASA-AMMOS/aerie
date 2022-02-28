package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchedulingGoalDSLCompilationServiceTests {
  SchedulingGoalDSLCompilationService schedulingGoalDSLCompilationService;

  @BeforeAll
  void setUp() throws SchedulingGoalDSLCompilationService.SchedulingGoalDSLCompilationException, IOException {
    schedulingGoalDSLCompilationService = new SchedulingGoalDSLCompilationService();
  }

  @AfterAll
  void tearDown() {
    schedulingGoalDSLCompilationService.close();
  }

  @Test
  void testSchedulingDSL_basic()
  throws IOException, SchedulingGoalDSLCompilationService.SchedulingGoalDSLCompilationException
  {
    final SchedulingGoalDSLCompilationService.GoalDefinition actualGoalDefinition;
    actualGoalDefinition = schedulingGoalDSLCompilationService.compileSchedulingGoalDSL(
        """
                export default function myGoal() {
                  return Goal.ActivityRecurrenceGoal({
                    windowSet: WindowSet.entirePlanWindow,
                    activityTemplate: ActivityTemplates.PeelBanana('some goal', { peelDirection: 'fromStem' }),
                    rangeToGenerate: [1,1]
                  })
                }
            """, "goalfile");
    final var expectedGoalDefinition = new SchedulingGoalDSLCompilationService.GoalDefinition(
        "ActivityRecurrenceGoal",
        new SchedulingGoalDSLCompilationService.WindowExpression("ConstraintOperatorEntirePlanWindow"),
        new SchedulingGoalDSLCompilationService.ActivityTemplate(
            "some goal",
            "PeelBanana",
            Map.of(
                "peelDirection",
                SerializedValue.of("fromStem"))),
        List.of(1, 1));
    assertEquals(expectedGoalDefinition, actualGoalDefinition);
  }

  @Test
  void testSchedulingDSL_helper_function()
  throws SchedulingGoalDSLCompilationService.SchedulingGoalDSLCompilationException, IOException
  {
    final SchedulingGoalDSLCompilationService.GoalDefinition actualGoalDefinition;
    actualGoalDefinition = schedulingGoalDSLCompilationService.compileSchedulingGoalDSL(
        """
                export default function myGoal() {
                  return myHelper(ActivityTemplates.PeelBanana('some goal', { peelDirection: 'fromStem' }))
                }
                function myHelper(activityTemplate) {
                  return Goal.ActivityRecurrenceGoal({
                    windowSet: WindowSet.entirePlanWindow,
                    activityTemplate,
                    rangeToGenerate: [1,1]
                  })
                }
            """, "goalfile");
    final var expectedGoalDefinition = new SchedulingGoalDSLCompilationService.GoalDefinition(
        "ActivityRecurrenceGoal",
        new SchedulingGoalDSLCompilationService.WindowExpression("ConstraintOperatorEntirePlanWindow"),
        new SchedulingGoalDSLCompilationService.ActivityTemplate(
            "some goal",
            "PeelBanana",
            Map.of(
                "peelDirection",
                SerializedValue.of("fromStem"))),
        List.of(1, 1));
    assertEquals(expectedGoalDefinition, actualGoalDefinition);
  }

  @Test
  void testSchedulingDSL_variable_not_defined() {
    try {
      schedulingGoalDSLCompilationService.compileSchedulingGoalDSL(
          """
                export default function myGoal() {
                  const x = "hello world" - 2
                  return myHelper(ActivityTemplates.PeelBanana('some goal', { peelDirection: 'fromStem' }))
                }
                function myHelper(activityTemplate) {
                  return Goal.ActivityRecurrenceGoal({
                    windowSet: x,
                    activityTemplate,
                    rangeToGenerate: [1,1]
                  })
                }
              """, "goalfile_with_type_error");
    } catch (SchedulingGoalDSLCompilationService.SchedulingGoalDSLCompilationException | IOException e) {
      final var expectedError = "x is not defined";
      assertTrue(
          e.getMessage().contains(expectedError),
          "Exception should contain " + expectedError + ", but was " + e.getMessage());
      return;
    }
    fail("Did not throw CompilationException");
  }

  @Test
  @Disabled("We haven't figured out how to handle this case yet")
  void testSchedulingDSL_wrong_return_type() {
    try {
      schedulingGoalDSLCompilationService.compileSchedulingGoalDSL(
          """
                export default function myGoal() {
                  return 5
                }
              """, "goalfile_with_wrong_return_type");
    } catch (SchedulingGoalDSLCompilationService.SchedulingGoalDSLCompilationException | IOException e) {
      final var expectedError =
          "error TS2394: This overload signature is not compatible with its implementation signature";
      assertTrue(
          e.getMessage().contains(expectedError),
          "Exception should contain " + expectedError + ", but was " + e.getMessage());
      return;
    }
    fail("Did not throw CompilationException");
  }

  @Test
  void testCodeGen() {
    assertEquals(
        "fred",
        SchedulingGoalDSLCompilationService.
            generateTypescriptTypesFromMissionModel(
                new SchedulingGoalDSLCompilationService.MissionModelTypes(
                    List.of(new SchedulingGoalDSLCompilationService.ActivityType(
                        "PeelBanana",
                        Map.of(
                            "peelDirection",
                            ValueSchema.ofVariant(List.of(
                                new ValueSchema.Variant(
                                    "fromTip", "fromTip"
                                ),
                                new ValueSchema.Variant(
                                    "fromStem",
                                    "fromStem"))),
                            "duration",
                            ValueSchema.INT))),
                    null
                )));
  }
}
