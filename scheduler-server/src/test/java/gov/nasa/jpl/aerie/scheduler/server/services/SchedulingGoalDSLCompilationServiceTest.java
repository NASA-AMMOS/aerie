package gov.nasa.jpl.aerie.scheduler.server.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SchedulingGoalDSLCompilationServiceTest {

  @Test
  void testSchedulingDSL_basic() throws SchedulingGoalDSLCompilationService.SchedulingGoalDSLCompilationException {
    final var goalDefinition = SchedulingGoalDSLCompilationService.compileSchedulingGoalDSL(
        """
                function goal() {
                  let abc = "hello world"

                  for (var i = 0; i < 100; i++) {
                    abc = "hello world " + i
                  }

                  return { abc }
                }
            """);
    assertEquals(new SchedulingGoalDSLCompilationService.GoalDefinition("abc", "hello world 99"), goalDefinition);
  }

  @Test
  void testSchedulingDSL_helper_function() throws SchedulingGoalDSLCompilationService.SchedulingGoalDSLCompilationException {
    final var goalDefinition = SchedulingGoalDSLCompilationService.compileSchedulingGoalDSL(
        """
                function goal() {
                  let abc = "hello world"

                  abc = helper(abc)

                  return { abc }
                }
                function helper(x: string) {
                  let abc = x;
                  for (var i = 0; i < 50; i++) {
                    abc = "hello world " + i
                  }
                  return abc
                }
            """);
    assertEquals(new SchedulingGoalDSLCompilationService.GoalDefinition("abc", "hello world 49"), goalDefinition);
  }

  @Test
  void testSchedulingDSL_type_error() {
    try {
      SchedulingGoalDSLCompilationService.compileSchedulingGoalDSL(
          """
                  function goal() {
                    let abc = "hello world" - 2  // this should be a type error
                    return { abc }
                  }
              """);
    } catch (SchedulingGoalDSLCompilationService.SchedulingGoalDSLCompilationException e) {
      final var expectedError =
          "error TS2362: The left-hand side of an arithmetic operation must be of type 'any', 'number', 'bigint' or an enum type.";
      assertTrue(e.getMessage().contains(expectedError), "Exception should contain " + expectedError + ", but was " + e.getMessage());
      return;
    }
    fail("Did not throw CompilationException");
  }

  @Test
  void testSchedulingDSL_wrong_return_type() {
    try {
      SchedulingGoalDSLCompilationService.compileSchedulingGoalDSL(
          """
                  function goal() {
                    return 5
                  }
              """);
    } catch (SchedulingGoalDSLCompilationService.SchedulingGoalDSLCompilationException e) {
      final var expectedError =
          "error TS2394: This overload signature is not compatible with its implementation signature";
      assertTrue(e.getMessage().contains(expectedError), "Exception should contain " + expectedError + ", but was " + e.getMessage());
      return;
    }
    fail("Did not throw CompilationException");
  }
}
