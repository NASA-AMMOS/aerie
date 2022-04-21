package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.server.models.ConstraintsDSL;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConstraintsDSLCompilationServiceTests {
  private static final PlanId PLAN_ID = new PlanId(1L);
  ConstraintsDSLCompilationService constraintsDSLCompilationService;

  @BeforeAll
  void setUp() throws IOException {
    constraintsDSLCompilationService = new ConstraintsDSLCompilationService(new TypescriptCodeGenerationService());
  }

  @AfterAll
  void tearDown() {
    constraintsDSLCompilationService.close();
  }

  @Test
  void testConstraintsDSL_basic()
  {
    final ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult result;
    result = constraintsDSLCompilationService.compileConstraintsDSL(
        PLAN_ID, """
                export default function myConstraint() {
                  return Constraint.DummyConstraint(4)
                }
            """, "constraintfile");
    final var expectedConstraintDefinition = new ConstraintsDSL.ConstraintSpecifier.DummyConstraintDefinition(4);
    if (result instanceof ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Success r) {
      assertEquals(expectedConstraintDefinition, r.constraintSpecifier());
    } else if (result instanceof ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Error r) {
      fail(r.toString());
    }
  }

  @Test
  void testConstraintsDSL_helper_function()
  {
    final ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult result;
    result = constraintsDSLCompilationService.compileConstraintsDSL(
        PLAN_ID, """
                export default function myConstraint() {
                  return myHelper(2)
                }
                function myHelper(n: number) {
                  return Constraint.DummyConstraint(n*2)
                }
            """, "constraintfile");
    final var expectedConstraintDefinition = new ConstraintsDSL.ConstraintSpecifier.DummyConstraintDefinition(4);
    if (result instanceof ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Success r) {
      assertEquals(expectedConstraintDefinition, r.constraintSpecifier());
    } else if (result instanceof ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Error r) {
      fail(r.toString());
    }
  }

  @Test
  void testConstraintsDSL_variable_not_defined() {
    final ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Error actualErrors;
    actualErrors = (ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Error) constraintsDSLCompilationService.compileConstraintsDSL(
          PLAN_ID, """
                export default function myConstraint() {
                  const x = 4 - 2
                  return myHelper(2);
                }
                function myHelper(n: number) {
                  return Constraint.DummyConstraint(x * n);
                  )
                }
              """, "constraintfile_with_type_error");
    assertTrue(
        actualErrors.errors()
                    .stream()
                    .anyMatch(e -> e.message().contains("TypeError: TS2304 Cannot find name 'x'."))
    );
  }

  @Test
  void testConstraintsDSL_wrong_return_type() {
    final ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Error actualErrors;
    actualErrors = (ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Error) constraintsDSLCompilationService.compileConstraintsDSL(
          PLAN_ID, """
                export default function myConstraint() {
                  return 5
                }
              """, "constraintfile_with_wrong_return_type");
    assertTrue(
        actualErrors.errors()
                    .stream()
                    .anyMatch(e -> e.message().contains("TypeError: TS2322 Incorrect return type. Expected: 'Constraint', Actual: 'number'."))
    );
  }
}
