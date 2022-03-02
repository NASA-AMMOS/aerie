package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.server.models.SchedulingDSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchedulingDSLCompilationServiceTests {
  SchedulingDSLCompilationService schedulingDSLCompilationService;

  @BeforeAll
  void setUp() throws SchedulingDSLCompilationService.SchedulingDSLCompilationException, IOException {
    schedulingDSLCompilationService = new SchedulingDSLCompilationService();
  }

  @AfterAll
  void tearDown() {
    schedulingDSLCompilationService.close();
  }

  @Test
  void testSchedulingDSL_basic()
  throws IOException, SchedulingDSLCompilationService.SchedulingDSLCompilationException
  {
    final SchedulingDSL.GoalSpecifier.GoalDefinition actualGoalDefinition;
    actualGoalDefinition = (SchedulingDSL.GoalSpecifier.GoalDefinition) schedulingDSLCompilationService.compileSchedulingGoalDSL(
        """
                export default function myGoal() {
                  return Goal.ActivityRecurrenceGoal({
                    activityTemplate: {
                      name: "MyActivity",
                      activityType: "PeelBanana",
                      args: {
                        "peelDirection": "fromStem"
                      }
                    },
                    interval: 60 * 60 * 1000 * 1000 // 1 hour in microseconds
                  })
                }
            """, "goalfile");
    final var expectedGoalDefinition = new SchedulingDSL.GoalSpecifier.GoalDefinition(
        SchedulingDSL.GoalKinds.ActivityRecurrenceGoal,
        new SchedulingDSL.ActivityTemplate(
            "MyActivity",
            "PeelBanana",
            Map.of(
                "peelDirection",
                SerializedValue.of("fromStem"))),
        Duration.HOUR);
    assertEquals(expectedGoalDefinition, actualGoalDefinition);
  }

  @Test
  void testSchedulingDSL_helper_function()
  throws SchedulingDSLCompilationService.SchedulingDSLCompilationException, IOException
  {
    final SchedulingDSL.GoalSpecifier.GoalDefinition actualGoalDefinition;
    actualGoalDefinition = (SchedulingDSL.GoalSpecifier.GoalDefinition) schedulingDSLCompilationService.compileSchedulingGoalDSL(
        """
                export default function myGoal() {
                  return myHelper({
                    name: "MyActivity",
                    activityType: "PeelBanana",
                    args: {
                      "peelDirection": "fromStem"
                    }
                  })
                }
                function myHelper(activityTemplate) {
                  return Goal.ActivityRecurrenceGoal({
                    activityTemplate,
                    interval: 60 * 60 * 1000 * 1000 // 1 hour in microseconds
                  })
                }
            """, "goalfile");
    final var expectedGoalDefinition = new SchedulingDSL.GoalSpecifier.GoalDefinition(
        SchedulingDSL.GoalKinds.ActivityRecurrenceGoal,
        new SchedulingDSL.ActivityTemplate(
            "MyActivity",
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
      schedulingDSLCompilationService.compileSchedulingGoalDSL(
          """
                export default function myGoal() {
                  const x = "hello world" - 2
                  return myHelper({
                    name: "MyActivity",
                    activityType: "PeelBanana",
                    args: {
                      "peelDirection": "fromStem"
                    }
                  })
                }
                function myHelper(activityTemplate) {
                  return Goal.ActivityRecurrenceGoal({
                    windowSet: x,
                    activityTemplate,
                    interval: 60 * 60 * 1000 * 1000 // 1 hour in microseconds
                  })
                }
              """, "goalfile_with_type_error");
    } catch (SchedulingDSLCompilationService.SchedulingDSLCompilationException | IOException e) {
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
      schedulingDSLCompilationService.compileSchedulingGoalDSL(
          """
                export default function myGoal() {
                  return 5
                }
              """, "goalfile_with_wrong_return_type");
    } catch (SchedulingDSLCompilationService.SchedulingDSLCompilationException | IOException e) {
      final var expectedError =
          "error TS2394: This overload signature is not compatible with its implementation signature";
      assertTrue(
          e.getMessage().contains(expectedError),
          "Exception should contain " + expectedError + ", but was " + e.getMessage());
      return;
    }
    fail("Did not throw CompilationException");
  }
}
