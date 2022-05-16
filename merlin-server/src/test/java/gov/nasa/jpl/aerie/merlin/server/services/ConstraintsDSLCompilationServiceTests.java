package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.tree.Changed;
import gov.nasa.jpl.aerie.constraints.tree.LessThan;
import gov.nasa.jpl.aerie.constraints.tree.ProfileExpression;
import gov.nasa.jpl.aerie.constraints.tree.RealResource;
import gov.nasa.jpl.aerie.constraints.tree.RealValue;
import gov.nasa.jpl.aerie.constraints.tree.Times;
import gov.nasa.jpl.aerie.constraints.tree.True;
import gov.nasa.jpl.aerie.constraints.tree.ViolationsOf;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.models.RealProfile;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;

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
                export default () => {
                  return Real.Value(4).lessThan(Real.Value(5))
                }
            """);
    final var expectedConstraintDefinition = new ViolationsOf(
        new LessThan(new RealValue(4), new RealValue(5))
    );
    if (result instanceof ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Success r) {
      assertEquals(expectedConstraintDefinition, r.constraintExpression());
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
                  return times2(Real.Resource("my_resource")).changed()
                }
                function times2(e: Real): Real {
                  return e.times(2)
                }
            """);
    final var expectedConstraintDefinition = new ViolationsOf(
        new Changed<>(new ProfileExpression<>(new Times(new RealResource("my_resource"), 2)))
    );
    if (result instanceof ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Success r) {
      assertEquals(expectedConstraintDefinition, r.constraintExpression());
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
                  const x = 5;
                  return times2(Real.Resource("my_resource")).changed()
                }
                function times2(e: Real): Real {
                  return e.times(x)
                }}
              """);
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
                   return Real.Resource("my_resource");
                }
              """);
    assertTrue(
        actualErrors.errors()
                    .stream()
                    .anyMatch(e -> e.message().contains("TypeError: TS2322 Incorrect return type. Expected: 'Constraint', Actual: 'Real'."))
    );
  }
}
