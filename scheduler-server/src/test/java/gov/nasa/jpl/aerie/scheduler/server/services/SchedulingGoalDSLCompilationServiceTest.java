package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.scheduler.server.models.SchedulingDSL;
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
    final SchedulingDSL.GoalSpecifier.GoalDefinition actualGoalDefinition;
    actualGoalDefinition = (SchedulingDSL.GoalSpecifier.GoalDefinition) schedulingGoalDSLCompilationService.compileSchedulingGoalDSL(
        """
                export default function myGoal() {
                  return Goal.ActivityRecurrenceGoal({
                    windowSet: WindowSet.entirePlanWindow,
                    activityTemplate: ActivityTemplates.PeelBanana('some goal', { peelDirection: 'fromStem' }),
                    interval: 60 * 60 * 1000 * 1000 // 1 hour in microseconds
                  })
                }
            """, "goalfile");
    final var expectedGoalDefinition = new SchedulingDSL.GoalSpecifier.GoalDefinition(
        SchedulingDSL.GoalKinds.ActivityRecurrenceGoal,
        new SchedulingDSL.WindowSetSpecifier.ConstraintOperatorEntirePlanWindow(),
        new SchedulingDSL.ActivityTemplate(
            "some goal",
            "PeelBanana",
            Map.of(
                "peelDirection",
                SerializedValue.of("fromStem"))),
        Duration.HOUR);
    assertEquals(expectedGoalDefinition, actualGoalDefinition);
  }

  @Test
  void testSchedulingDSL_helper_function()
  throws SchedulingGoalDSLCompilationService.SchedulingGoalDSLCompilationException, IOException
  {
    final SchedulingDSL.GoalSpecifier.GoalDefinition actualGoalDefinition;
    actualGoalDefinition = (SchedulingDSL.GoalSpecifier.GoalDefinition) schedulingGoalDSLCompilationService.compileSchedulingGoalDSL(
        """
                export default function myGoal() {
                  return myHelper(ActivityTemplates.PeelBanana('some goal', { peelDirection: 'fromStem' }))
                }
                function myHelper(activityTemplate) {
                  return Goal.ActivityRecurrenceGoal({
                    windowSet: WindowSet.entirePlanWindow,
                    activityTemplate,
                    interval: 60 * 60 * 1000 * 1000 // 1 hour in microseconds
                  })
                }
            """, "goalfile");
    final var expectedGoalDefinition = new SchedulingDSL.GoalSpecifier.GoalDefinition(
        SchedulingDSL.GoalKinds.ActivityRecurrenceGoal,
        new SchedulingDSL.WindowSetSpecifier.ConstraintOperatorEntirePlanWindow(),
        new SchedulingDSL.ActivityTemplate(
            "some goal",
            "PeelBanana",
            Map.of(
                "peelDirection",
                SerializedValue.of("fromStem"))),
        Duration.HOUR);
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
                    interval: 60 * 60 * 1000 * 1000 // 1 hour in microseconds
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
        "/** Start Codegen */\n"
        + "interface PeelBanana extends ActivityTemplate {}\n"
        + "export const ActivityTemplates = {\n"
        + "  PeelBanana: function PeelBanana(\n"
        + "    name: string,\n"
        + "    parameters: {\n"
        + "      duration: Duration,\n"
        + "  fancy: {\n"
        + "    subfield1: string,\n"
        + "    subfield2: {\n"
        + "      subsubfield1: Double,\n"
        + "    }[],\n"
        + "  },\n"
        + "  peelDirection: \"fromTip\" | \"fromStem\",\n"
        + "\n"
        + "    }): PeelBanana {\n"
        + "    return {\n"
        + "      name,\n"
        + "      activityType: 'PeelBanana',\n"
        + "      parameters: args,\n"
        + "    };\n"
        + "  },\n"
        + "}\n"
        + "/** End Codegen */",
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
                            ValueSchema.DURATION,
                            "fancy",
                            ValueSchema.ofStruct(Map.of(
                                "subfield1", ValueSchema.STRING,
                                "subfield2", ValueSchema.ofSeries(ValueSchema.ofStruct(Map.of("subsubfield1", ValueSchema.REAL)))
                            ))))),
                    null
                )));
  }
}
