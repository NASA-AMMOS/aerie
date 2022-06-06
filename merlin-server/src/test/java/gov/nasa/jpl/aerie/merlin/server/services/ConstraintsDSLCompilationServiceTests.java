package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.constraints.tree.*;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.server.mocks.StubMissionModelService;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConstraintsDSLCompilationServiceTests {
  private static final PlanId PLAN_ID = new PlanId(1L);
  ConstraintsDSLCompilationService constraintsDSLCompilationService;

  @BeforeAll
  void setUp() throws IOException {
    constraintsDSLCompilationService = new ConstraintsDSLCompilationService(
        new TypescriptCodeGenerationServiceAdapter(new StubMissionModelService())
    );
  }

  @AfterAll
  void tearDown() {
    constraintsDSLCompilationService.close();
  }

  private <T> void checkSuccessfulCompilation(String constraint, Expression<T> expected)
  {
    final ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult result;
    result = assertDoesNotThrow(() -> constraintsDSLCompilationService.compileConstraintsDSL("abc", constraint));
    if (result instanceof ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Success r) {
      assertEquals(expected, r.constraintExpression());
    } else if (result instanceof ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Error r) {
      fail(r.toString());
    }
  }

  private void checkFailedCompilation(String constraint, String error) {
    final ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Error actualErrors;
    actualErrors = (ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Error) assertDoesNotThrow(() -> constraintsDSLCompilationService.compileConstraintsDSL(
        "abc", constraint
    ));
    if (actualErrors.errors()
                    .stream()
                    .noneMatch(e -> e.message().contains(error))) {
      fail("Expected error:\n" + error + "\nIn list of errors:\n" + actualErrors.errors() + "\n");
    }
  }

  @Test
  void testConstraintsDSL_helper_function()
  {
    checkSuccessfulCompilation(
      """
        export default function myConstraint() {
          return times2(Real.Resource("state of charge")).changed()
        }
        function times2(e: Real): Real {
          return e.times(2)
        }
      """,
      new ViolationsOf(
          new Changed<>(new ProfileExpression<>(new Times(new RealResource("state of charge"), 2.0)))
      )
    );
  }

  @Test
  void testConstraintsDSL_variable_not_defined() {
    checkFailedCompilation(
        """
          export default function myConstraint() {
            const x = 5;
            return times2(Real.Resource("mode")).changed()
          }
          function times2(e: Real): Real {
            return e.times(x)
          }
        """,
       "TypeError: TS2304 Cannot find name 'x'."
    );
  }

  @Test
  void testConstraintsDSL_wrong_return_type() {
    checkFailedCompilation(
        """
          export default function myConstraint() {
             return Real.Resource("state of charge");
          }
        """,
        "TypeError: TS2322 Incorrect return type. Expected: 'Constraint', Actual: 'Real'."
    );
  }

  //// TESTS FOR `Discrete` CLASS API

  @Test
  void testDiscreteResource() {
    checkSuccessfulCompilation(
        """
            export default () => {
              return Discrete.Resource("mode").changed();
            }
        """,
        new ViolationsOf(new Changed<>(new ProfileExpression<>(new DiscreteResource("mode"))))
    );
  }

  @Test
  void testDiscreteValue() {
    checkSuccessfulCompilation(
        """
            export default () => {
              return Discrete.Value(5).changed()
            }
        """,
        new ViolationsOf(new Changed<>(new ProfileExpression<>(new DiscreteValue(SerializedValue.of(5)))))
    );
  }

  @Test
  void testDiscreteParameter() {
    checkSuccessfulCompilation(
        """
            export default () => Constraint.ForEachActivity(
              ActivityType.activity,
              (instance) => instance.parameters.Param.changed()
            )
        """,
        new ForEachActivity(
            "activity",
            "activity alias 0",
            new ViolationsOf(new Changed<>(new ProfileExpression<>(new DiscreteParameter("activity alias 0", "Param"))))
        )
    );
  }

  @Test
  void testTransition() {
    checkSuccessfulCompilation(
        """
            export default () => {
              return Discrete.Resource("mode").transition("Option1", "Option2");
            }
        """,
        new ViolationsOf(
            new Transition(
                new DiscreteResource("mode"),
                SerializedValue.of("Option1"),
                SerializedValue.of("Option2")
            )
        )
    );
  }

  @Test
  void testTransitionMismatchedTypes() {
    checkFailedCompilation(
        """
          export default () => {
            return Discrete.Resource("mode").transition("something else", 5);
          }
        """,
        "TS2345 Argument of type '\"something else\"' is not assignable to parameter of type '\"Option1\" | \"Option2\"'."
    );
  }

  @Test
  void testDiscreteEqual() {
    checkSuccessfulCompilation(
        """
        export default () => {
          return Discrete.Resource("mode").equal("Option2");
        }
        """,
        new ViolationsOf(new Equal<>(new DiscreteResource("mode"), new DiscreteValue(SerializedValue.of("Option2"))))
    );
  }

  @Test
  void testDiscreteNotEqual() {
    checkSuccessfulCompilation(
        """
        export default () => {
          return Discrete.Resource("state of charge").notEqual(4.0);
        }
        """,
        new ViolationsOf(new NotEqual<>(new DiscreteResource("state of charge"), new DiscreteValue(SerializedValue.of(4.0))))
    );
  }

  @Test
  void testDiscreteChanged() {
    checkSuccessfulCompilation(
        """
            export default () => {
              return Discrete.Value(4).changed()
            }
        """,
        new ViolationsOf(new Changed<>(new ProfileExpression<>(new DiscreteValue(SerializedValue.of(4)))))
    );
  }

  @Test
  void testAutomaticWrapAnyInDiscreteValue() {
    checkSuccessfulCompilation(
        """
          export default () => {
            return Discrete.Resource("mode").equal("Option1")
          }
        """,
        new ViolationsOf(new Equal<>(new DiscreteResource("mode"), new DiscreteValue(SerializedValue.of("Option1"))))
    );
  }

  //// TESTS FOR `Real` CLASS API

  @Test
  void testRealResource() {
    checkSuccessfulCompilation(
        """
            export default () => {
              return Real.Resource("state of charge").changed();
            }
        """,
        new ViolationsOf(new Changed<>(new ProfileExpression<>(new RealResource("state of charge"))))
    );
  }

  @Test
  void testRealValue() {
    checkSuccessfulCompilation(
        """
            export default () => {
              return Real.Value(5).changed()
            }
        """,
        new ViolationsOf(new Changed<>(new ProfileExpression<>(new RealValue(5.0))))
    );
  }

  @Test
  void testRealParameter() {
    checkSuccessfulCompilation(
        """
            export default () => Constraint.ForEachActivity(
              ActivityType.activity,
              (instance) => instance.parameters.AnotherParam.changed()
            )
        """,
        new ForEachActivity(
            "activity",
            "activity alias 0",
            new ViolationsOf(new Changed<>(new ProfileExpression<>(new RealParameter("activity alias 0", "AnotherParam"))))
        )
    );
  }

  @Test
  void testRate() {
    checkSuccessfulCompilation(
        """
            export default() => {
              return Real.Resource("state of charge").rate().equal(Real.Value(4.0))
            }
        """,
        new ViolationsOf(new Equal<>(new Rate(new RealResource("state of charge")), new RealValue(4.0)))
    );
  }

  @Test
  void testTimes() {
    checkSuccessfulCompilation(
        """
            export default() => {
              return Real.Resource("state of charge").times(2).equal(Real.Value(4.0))
            }
        """,
        new ViolationsOf(new Equal<>(new Times(new RealResource("state of charge"), 2.0), new RealValue(4.0)))
    );
  }

  @Test
  void testPlus() {
    checkSuccessfulCompilation(
        """
            export default() => {
              return Real.Resource("state of charge").plus(Real.Value(2.0)).equal(Real.Value(4.0))
            }
        """,
        new ViolationsOf(new Equal<>(new Plus(new RealResource("state of charge"), new RealValue(2.0)), new RealValue(4.0)))
    );
  }

  @Test
  void testLessThan() {
    checkSuccessfulCompilation(
        """
            export default() => {
              return Real.Resource("state of charge").lessThan(Real.Value(2.0))
            }
        """,
        new ViolationsOf(new LessThan(new RealResource("state of charge"), new RealValue(2.0)))
    );
  }

  @Test
  void testLessThanOrEqual() {
    checkSuccessfulCompilation(
        """
            export default() => {
              return Real.Resource("state of charge").lessThanOrEqual(Real.Value(2.0))
            }
        """,
        new ViolationsOf(new LessThanOrEqual(new RealResource("state of charge"), new RealValue(2.0)))
    );
  }

  @Test
  void testGreaterThan() {
    checkSuccessfulCompilation(
        """
            export default() => {
              return Real.Resource("state of charge").greaterThan(Real.Value(2.0))
            }
        """,
        new ViolationsOf(new GreaterThan(new RealResource("state of charge"), new RealValue(2.0)))
    );
  }

  @Test
  void testGreaterThanOrEqual() {
    checkSuccessfulCompilation(
        """
            export default() => {
              return Real.Resource("state of charge").greaterThanOrEqual(Real.Value(2.0))
            }
        """,
        new ViolationsOf(new GreaterThanOrEqual(new RealResource("state of charge"), new RealValue(2.0)))
    );
  }

  @Test
  void testRealEqual() {
    checkSuccessfulCompilation(
        """
        export default () => {
          return Real.Resource("state of charge").equal(Real.Value(-1));
        }
        """,
        new ViolationsOf(new Equal<>(new RealResource("state of charge"), new RealValue(-1.0)))
    );
  }

  @Test
  void testRealNotEqual() {
    checkSuccessfulCompilation(
        """
        export default () => {
          return Real.Resource("an integer").notEqual(Real.Value(-1));
        }
        """,
        new ViolationsOf(new NotEqual<>(new RealResource("an integer"), new RealValue(-1.0)))
    );
  }

  @Test
  void testRealChanged() {
    checkSuccessfulCompilation(
        """
            export default () => {
              return Real.Value(4).changed()
            }
        """,
        new ViolationsOf(new Changed<>(new ProfileExpression<>(new RealValue(4.0))))
    );
  }

  @Test
  void testAutomaticWrapNumberInRealValue() {
    checkSuccessfulCompilation(
        """
          export default () => {
            return Real.Value(2.2).plus(-3).lessThan(5);
          }
        """,
        new ViolationsOf(new LessThan(new Plus(new RealValue(2.2), new RealValue(-3.0)), new RealValue(5.0)))
    );
  }

  //// TESTS FOR `Windows` CLASS API

  @Test
  void testDuring() {
    checkSuccessfulCompilation(
        """
          export default () => {
            return Constraint.ForEachActivity(ActivityType.activity, (alias) => alias.window());
          }
        """,
        new ForEachActivity("activity", "activity alias 0", new ViolationsOf(new During("activity alias 0")))
    );
  }

  @Test
  void testStartOf() {
    checkSuccessfulCompilation(
        """
          export default () => {
            return Constraint.ForEachActivity(ActivityType.activity, (alias) => alias.start());
          }
        """,
        new ForEachActivity("activity", "activity alias 0", new ViolationsOf(new StartOf("activity alias 0")))
    );
  }

  @Test
  void testEndOf() {
    checkSuccessfulCompilation(
        """
          export default () => {
            return Constraint.ForEachActivity(ActivityType.activity, (alias) => alias.end());
          }
        """,
        new ForEachActivity("activity", "activity alias 0", new ViolationsOf(new EndOf("activity alias 0")))
    );
  }

  @Test
  void testIf() {
    checkSuccessfulCompilation(
        """
          export default () => {
            return Real.Resource("state of charge").lessThan(2)
              .if(Discrete.Resource("mode").changed());
          }
        """,
        new ViolationsOf(
            new Or(
                new Not(new Changed<>(
                    new ProfileExpression<>(new DiscreteResource("mode"))
                )),
                new LessThan(new RealResource("state of charge"), new RealValue(2.0))
            )
        )
    );
  }

  @Test
  void testAll() {
    checkSuccessfulCompilation(
        """
          export default () => {
            return Windows.All(
              Real.Resource("state of charge").lessThan(2),
              Discrete.Value("hello there").notEqual(Discrete.Value("hello there")),
              Real.Value(5).changed()
            );
          }
        """,
        new ViolationsOf(
            new And(
                java.util.List.of(
                    new LessThan(new RealResource("state of charge"), new RealValue(2.0)),
                    new NotEqual<>(new DiscreteValue(SerializedValue.of("hello there")), new DiscreteValue(SerializedValue.of("hello there"))),
                    new Changed<>(new ProfileExpression<>(new RealValue(5.0)))
                )
            )
        )
    );
  }

  @Test
  void testAny() {
    checkSuccessfulCompilation(
        """
          export default () => {
            return Windows.Any(
              Real.Resource("state of charge").lessThan(2),
              Discrete.Value("hello there").notEqual(Discrete.Value("hello there")),
              Real.Value(5).changed()
            );
          }
        """,
        new ViolationsOf(
            new Or(
                java.util.List.of(
                    new LessThan(new RealResource("state of charge"), new RealValue(2.0)),
                    new NotEqual<>(new DiscreteValue(SerializedValue.of("hello there")), new DiscreteValue(SerializedValue.of("hello there"))),
                    new Changed<>(new ProfileExpression<>(new RealValue(5.0)))
                )
            )
        )
    );
  }

  @Test
  void testNot() {
    checkSuccessfulCompilation(
        """
          export default () => {
            return Discrete.Resource("mode").changed().not()
          }
        """,
        new ViolationsOf(
            new Not(
                new Changed<>(new ProfileExpression<>(new DiscreteResource("mode")))
            )
        )
    );
  }

  @Test
  void testViolations() {
    checkSuccessfulCompilation(
        """
        export default () => {
          return Real.Resource("state of charge").equal(Real.Value(-1)).violations();
        }
        """,
        new ViolationsOf(new Equal<>(new RealResource("state of charge"), new RealValue(-1.0)))
    );
  }

  //// TESTS FOR `Constraint` API CLASS

  @Test
  void testForbiddenActivityOverlap() {
    checkSuccessfulCompilation(
        """
        export default () => {
          return Constraint.ForbiddenActivityOverlap(ActivityType.activity, ActivityType.activity)
        }
        """,
        new ForEachActivity(
            "activity",
            "activity alias 0",
            new ForEachActivity(
                "activity",
                "activity alias 1",
                new ViolationsOf(new Not(new And(new During("activity alias 0"), new During("activity alias 1"))))
            )
        )
    );
  }

  @Test
  void testForEachActivity() {
    checkSuccessfulCompilation(
        """
        export default () => {
          return Constraint.ForEachActivity(
            ActivityType.activity,
            (myAlias) => myAlias.window()
          )
        }
        """,
        new ForEachActivity("activity", "activity alias 0", new ViolationsOf(new During("activity alias 0")))
    );

    checkSuccessfulCompilation(
        """
        import * as Gen from './mission-model-generated-code.js';
        export default () => {
          return Constraint.ForEachActivity(
            ActivityType.activity,
            myHelperFunction
          )
        }

        function myHelperFunction(instance: Gen.ActivityInstance<ActivityType.activity>): Constraint {
          return instance.window();
        }
        """,
        new ForEachActivity("activity", "activity alias 0", new ViolationsOf(new During("activity alias 0")))
    );
  }

  @Test
  void testNestedForEachActivity() {
    checkSuccessfulCompilation(
        """
        export default () => {
          return Constraint.ForEachActivity(
            ActivityType.activity,
            (alias1) => Constraint.ForEachActivity(
              ActivityType.activity,
              (alias2) => Windows.All(alias1.window(), alias2.window())
            )
          )
        }
        """,
        new ForEachActivity("activity", "activity alias 0", new ForEachActivity("activity", "activity alias 1", new ViolationsOf(
            new And(new During("activity alias 0"), new During("activity alias 1"))
        )))
    );
  }

  // TYPECHECKING FAILURE TESTS

  @Test
  void testWrongActivityType() {
    checkFailedCompilation(
        """
        export default () => {
          return Constraint.ForbiddenActivityOverlap(
            ActivityType.activity,
            "other activity"
          );
        }
        """,
        "TypeError: TS2345 Argument of type '\"other activity\"' is not assignable to parameter of type 'ActivityType'."
    );
  }

  @Test
  void testWrongResource() {
    checkFailedCompilation(
        """
        export default () => {
          return Discrete.Resource("wrong resource").changed()
        }
        """,
        "TypeError: TS2345 Argument of type '\"wrong resource\"' is not assignable to parameter of type 'ResourceName'."
    );
  }

  @Test
  void testWrongRealResource() {
    checkFailedCompilation(
        """
        export default () => {
          return Real.Resource("mode").changed()
        }
        """,
        "TypeError: TS2345 Argument of type '\"mode\"' is not assignable to parameter of type 'RealResourceName'."
    );
  }

  @Test
  void testWrongDiscreteSchema() {
    checkFailedCompilation(
        """
        export default () => {
          return Discrete.Resource("mode").equal(5)
        }
        """,
        "TypeError: TS2345 Argument of type '5' is not assignable to parameter of type '\"Option1\" | \"Option2\" | Discrete<\"Option1\" | \"Option2\">'."
    );

    checkFailedCompilation(
        """
        export default () => {
          return Discrete.Resource("mode").equal(Discrete.Resource("state of charge"))
        }
        """,
        "TypeError: TS2345 Argument of type 'Discrete<number>' is not assignable to parameter of type '\"Option1\" | \"Option2\" | Discrete<\"Option1\" | \"Option2\">'."
    );

    checkFailedCompilation(
        """
        export default () => {
          return Constraint.ForEachActivity(
            ActivityType.activity,
            (alias) => Discrete.Resource("mode").equal(alias.parameters.Param)
          )
        }
        """,
        "TypeError: TS2345 Argument of type 'Discrete<string>' is not assignable to parameter of type '\"Option1\" | \"Option2\" | Discrete<\"Option1\" | \"Option2\">'."
    );
  }
}
