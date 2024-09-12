package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.server.mocks.StubMissionModelService;
import gov.nasa.jpl.aerie.merlin.server.mocks.StubPlanService;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.services.constraints.ConstraintsDSLCompilationService;
import gov.nasa.jpl.aerie.merlin.server.services.constraints.TypescriptCodeGenerationServiceAdapter;
import gov.nasa.jpl.aerie.types.MissionModelId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConstraintsDSLCompilationServiceTests {
  private static final MissionModelId MISSION_MODEL_ID = new MissionModelId(1L);
  private static final PlanId PLAN_ID = new PlanId(1L);
  ConstraintsDSLCompilationService constraintsDSLCompilationService;

  @BeforeAll
  void setUp() throws IOException {
    constraintsDSLCompilationService = new ConstraintsDSLCompilationService(
        new TypescriptCodeGenerationServiceAdapter(new StubMissionModelService(), new StubPlanService())
    );
  }

  @AfterAll
  void tearDown() {
    constraintsDSLCompilationService.close();
  }
  synchronized private <T> void checkSuccessfulCompilation(String constraint, String expected)
  {
    final ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult result;
    result = assertDoesNotThrow(() -> constraintsDSLCompilationService.compileConstraintsDSL(MISSION_MODEL_ID, Optional.of(PLAN_ID), Optional.empty(), constraint));
    if (result instanceof ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Success r) {
      assertEquals(parseJsonString(expected), r.constraintExpression());
    } else if (result instanceof ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Error r) {
      fail(r.toString());
    }
  }

  private void checkFailedCompilation(String constraint, String error) {
    final ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Error actualErrors;
    actualErrors = (ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Error) assertDoesNotThrow(() -> constraintsDSLCompilationService.compileConstraintsDSL(
        MISSION_MODEL_ID, Optional.of(PLAN_ID), Optional.empty(), constraint
    ));
    if (actualErrors.errors()
                    .stream()
                    .noneMatch(e -> e.message().contains(error))) {
      fail("Expected error:\n" + error + "\nIn list of errors:\n" + actualErrors.errors() + "\n");
    }
  }

  private static JsonObject parseJsonString(String input) {
    try (final var parser = Json.createReader(new StringReader(input))) {
      return parser.readObject();
    }
  }

  @Test
  void testConstraintsDSL_helper_function()
  {
    checkSuccessfulCompilation(
        """
        export default function myConstraint() {
          return times2(Real.Resource("state of charge")).changes()
        }
        function times2(e: Real): Real {
          return e.times(2)
        }
        """,
        """
            {
              "kind": "ProfileChanges",
              "expression": {
                "kind": "RealProfileTimes",
                "multiplier": 2,
                "profile": {
                  "kind": "RealProfileResource",
                  "name": "state of charge"
                }
              }
            }"""
    );
  }

  @Test
  void testConstraintDSL_during(){
    checkSuccessfulCompilation(
        """
          export default () => {
              return Windows.During(ActivityType.activity)
          }
        """,
        """
{
    "kind": "WindowsExpressionOr",
    "expressions": [
        {
            "kind": "WindowsExpressionFromSpans",
            "spansExpression": {
                "kind": "ForEachActivitySpans",
                "activityType": "activity",
                "alias": "span activity alias 0",
                "expression": {
                    "kind": "SpansExpressionActivitySpan",
                    "alias": "span activity alias 0"
                }
            }
        }
    ]
}"""
    );
  }

  @Test
  void testConstraintsDSL_variable_not_defined() {
    checkFailedCompilation(
        """
          export default function myConstraint() {
            const x = 5;
            return times2(Real.Resource("mode")).changes()
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
        "TypeError: TS2322 Incorrect return type. Expected: 'Constraint | Promise<Constraint>', Actual: 'Real'."
    );
  }

  //// TESTS FOR `Discrete` CLASS API

  @Test
  void testDiscreteResource() {
    checkSuccessfulCompilation(
        """
            export default () => {
              return Discrete.Resource("mode").changes();
            }
        """,
        """
{
    "kind": "ProfileChanges",
    "expression": {
        "kind": "DiscreteProfileResource",
        "name": "mode"
    }
}"""
    );
  }

  @Test
  void testDiscreteValue() {
    checkSuccessfulCompilation(
        """
            export default () => {
              return Discrete.Value(5).changes()
            }
        """,
        """
{
    "kind": "ProfileChanges",
    "expression": {
        "kind": "DiscreteProfileValue",
        "value": 5
    }
}"""
    );
  }

  @Test
  void testDiscreteValueInterval() {
    checkSuccessfulCompilation(
        """
            export default () => {
              return Discrete.Value(5, Interval.Between(
                Temporal.Instant.fromEpochMilliseconds(1574074321816),
                Temporal.Instant.fromEpochMilliseconds(1574074322816)
              )).changes()
            }
        """,
        """
{
    "kind": "ProfileChanges",
    "expression": {
        "kind": "DiscreteProfileValue",
        "value": 5,
        "interval": {
            "kind": "AbsoluteInterval",
            "start": "2019-11-18T10:52:01.816Z",
            "end": "2019-11-18T10:52:02.816Z"
        }
    }
}"""
    );
  }

  @Test
  void testValueAt() {
    checkSuccessfulCompilation(
        """
            import { ActivityInstance } from './constraints-edsl-fluent-api.js';
            export default () => {
              return Discrete.Resource("mode").valueAt(new ActivityInstance(ActivityType.activity, "alias1").span().starts()).notEqual("Option1")
            }
        """,
        """
{
    "kind": "ExpressionNotEqual",
    "left": {
        "kind": "ValueAtExpression",
        "profile": {
            "kind": "DiscreteProfileResource",
            "name": "mode"
        },
        "timepoint": {
            "kind": "IntervalsExpressionStarts",
            "expression": {
                "kind": "SpansExpressionActivitySpan",
                "alias": "alias1"
            }
        }
    },
    "right": {
        "kind": "DiscreteProfileValue",
        "value": "Option1"
    }
}"""
    );
  }


  @Test
  void testDiscreteParameter() {
    checkSuccessfulCompilation(
        """
            export default () => Constraint.ForEachActivity(
              ActivityType.activity,
              (instance) => instance.parameters.Param.changes()
            )
        """,
        """
{
    "kind": "ForEachActivityViolations",
    "activityType": "activity",
    "alias": "activity alias 0",
    "expression": {
        "kind": "ProfileChanges",
        "expression": {
            "kind": "DiscreteProfileParameter",
            "alias": "activity alias 0",
            "name": "Param"
        }
    }
}"""
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
        """
{
    "kind": "DiscreteProfileTransition",
    "profile": {
        "kind": "DiscreteProfileResource",
        "name": "mode"
    },
    "from": "Option1",
    "to": "Option2"
}"""
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
        """
{
    "kind": "ExpressionEqual",
    "left": {
        "kind": "DiscreteProfileResource",
        "name": "mode"
    },
    "right": {
        "kind": "DiscreteProfileValue",
        "value": "Option2"
    }
}"""
    );
  }

  @Test
  void testDiscreteNotEqual() {
    checkSuccessfulCompilation(
        """
        export default () => {
          return Discrete.Resource("an integer").notEqual(4.0);
        }
        """,
        """
{
    "kind": "ExpressionNotEqual",
    "left": {
        "kind": "DiscreteProfileResource",
        "name": "an integer"
    },
    "right": {
        "kind": "DiscreteProfileValue",
        "value": 4
    }
}"""
    );
  }

  @Test
  void testDiscreteChanges() {
    checkSuccessfulCompilation(
        """
            export default () => {
              return Discrete.Value(4).changes()
            }
        """,
        """
{
    "kind": "ProfileChanges",
    "expression": {
        "kind": "DiscreteProfileValue",
        "value": 4
    }
}"""
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
        """
{
    "kind": "ExpressionEqual",
    "left": {
        "kind": "DiscreteProfileResource",
        "name": "mode"
    },
    "right": {
        "kind": "DiscreteProfileValue",
        "value": "Option1"
    }
}"""
    );
  }

  @Test
  void testDiscreteAssignGaps() {
    checkSuccessfulCompilation(
        """
          export default () => {
            return Discrete.Resource("mode").assignGaps("Option1").equal("Option1");
          }
        """,
        """
{
    "kind": "ExpressionEqual",
    "left": {
        "kind": "AssignGapsExpression",
        "originalProfile": {
            "kind": "DiscreteProfileResource",
            "name": "mode"
        },
        "defaultProfile": {
            "kind": "DiscreteProfileValue",
            "value": "Option1"
        }
    },
    "right": {
        "kind": "DiscreteProfileValue",
        "value": "Option1"
    }
}"""
    );

    checkSuccessfulCompilation(
        """
          export default () => {
            return Discrete.Resource("mode").assignGaps(Discrete.Resource("mode")).equal("Option1");
          }
        """,
        """
{
    "kind": "ExpressionEqual",
    "left": {
        "kind": "AssignGapsExpression",
        "originalProfile": {
            "kind": "DiscreteProfileResource",
            "name": "mode"
        },
        "defaultProfile": {
            "kind": "DiscreteProfileResource",
            "name": "mode"
        }
    },
    "right": {
        "kind": "DiscreteProfileValue",
        "value": "Option1"
    }
}"""
    );
  }

  //// TESTS FOR `Real` CLASS API

  @Test
  void testRealResource() {
    checkSuccessfulCompilation(
        """
            export default () => {
              return Real.Resource("state of charge").changes();
            }
        """,
        """
{
    "kind": "ProfileChanges",
    "expression": {
        "kind": "RealProfileResource",
        "name": "state of charge"
    }
}"""
    );
  }

  @Test
  void testRealValue() {
    checkSuccessfulCompilation(
        """
            export default () => {
              return Real.Value(5).changes()
            }
        """,
        """
{
    "kind": "ProfileChanges",
    "expression": {
        "kind": "RealProfileValue",
        "value": 5,
        "rate": 0
    }
}"""
    );
  }

  @Test
  void testRealValueInterval() {
    checkSuccessfulCompilation(
        """
            export default () => {
              return Real.Value(5, 3, Interval.Between(
                Temporal.Instant.from("2019-11-18T10:52:01.816Z"),
                Temporal.Instant.from("2019-11-18T10:52:02.816Z")
              )).changes()
            }
        """,
        """
{
    "kind": "ProfileChanges",
    "expression": {
        "kind": "RealProfileValue",
        "value": 5,
        "rate": 3,
        "interval": {
            "kind": "AbsoluteInterval",
            "start": "2019-11-18T10:52:01.816Z",
            "end": "2019-11-18T10:52:02.816Z"
        }
    }
}"""
    );

    checkSuccessfulCompilation(
        """
            export default () => {
              return Real.Value(5, 3, Interval.Between(
                Temporal.Instant.from("2019-11-18T10:52:01.816Z"),
                Temporal.Instant.from("2019-11-18T10:52:02.816Z"),
                Inclusivity.Exclusive,
                Inclusivity.Inclusive
              )).changes()
            }
        """,
        """
{
    "kind": "ProfileChanges",
    "expression": {
        "kind": "RealProfileValue",
        "value": 5,
        "rate": 3,
        "interval": {
            "kind": "AbsoluteInterval",
            "start": "2019-11-18T10:52:01.816Z",
            "end": "2019-11-18T10:52:02.816Z",
            "startInclusivity": "Exclusive",
            "endInclusivity": "Inclusive"
        }
    }
}"""
    );

    checkSuccessfulCompilation(
        """
            export default () => {
              return Real.Value(5, 3, Interval.At(
                Temporal.Instant.from("2019-11-18T10:52:01.816Z")
              )).changes()
            }
        """,
        """
{
    "kind": "ProfileChanges",
    "expression": {
        "kind": "RealProfileValue",
        "value": 5,
        "rate": 3,
        "interval": {
            "kind": "AbsoluteInterval",
            "start": "2019-11-18T10:52:01.816Z",
            "end": "2019-11-18T10:52:01.816Z"
        }
    }
}"""
    );

    checkSuccessfulCompilation(
        """
            export default () => {
              return Real.Value(5, 3, Interval.Horizon()).changes()
            }
        """,
        """
{
    "kind": "ProfileChanges",
    "expression": {
        "kind": "RealProfileValue",
        "value": 5,
        "rate": 3,
        "interval": {
            "kind": "AbsoluteInterval"
        }
    }
}"""
    );
  }

  @Test
  void testRealParameter() {
    checkSuccessfulCompilation(
        """
            export default () => Constraint.ForEachActivity(
              ActivityType.activity,
              (instance) => instance.parameters.AnotherParam.changes()
            )
        """,
        """
{
    "kind": "ForEachActivityViolations",
    "activityType": "activity",
    "alias": "activity alias 0",
    "expression": {
        "kind": "ProfileChanges",
        "expression": {
            "kind": "RealProfileParameter",
            "alias": "activity alias 0",
            "name": "AnotherParam"
        }
    }
}"""
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
        """
{
    "kind": "ExpressionEqual",
    "left": {
        "kind": "RealProfileRate",
        "profile": {
            "kind": "RealProfileResource",
            "name": "state of charge"
        }
    },
    "right": {
        "kind": "RealProfileValue",
        "value": 4,
        "rate": 0
    }
}"""
    );
  }

  @Test
  void testLongerThan() {
    checkSuccessfulCompilation(
        """
            export default() => {
              return Real.Resource("state of charge").rate().equal(Real.Value(4.0)).longerThan(Temporal.Duration.from({seconds: 1}));
            }
        """,
        """
{
    "kind": "WindowsExpressionLongerThan",
    "windowExpression": {
        "kind": "ExpressionEqual",
        "left": {
            "kind": "RealProfileRate",
            "profile": {
                "kind": "RealProfileResource",
                "name": "state of charge"
            }
        },
        "right": {
            "kind": "RealProfileValue",
            "value": 4,
            "rate": 0
        }
    },
    "duration": 1000000
}"""
    );
  }

  @Test
  void testShorterThan() {
    checkSuccessfulCompilation(
        """
            export default() => {
              return Real.Resource("state of charge").rate().equal(Real.Value(4.0)).shorterThan(Temporal.Duration.from({hours: 2}));
            }
        """,
        """
{
    "kind": "WindowsExpressionShorterThan",
    "windowExpression": {
        "kind": "ExpressionEqual",
        "left": {
            "kind": "RealProfileRate",
            "profile": {
                "kind": "RealProfileResource",
                "name": "state of charge"
            }
        },
        "right": {
            "kind": "RealProfileValue",
            "value": 4,
            "rate": 0
        }
    },
    "duration": 7200000000
}"""
    );
  }

  @Test
  void testShiftWindowsEdges() {
    checkSuccessfulCompilation(
        """
            const minute = (m: number) => Temporal.Duration.from({minutes: m});
            export default() => {
              return Real.Resource("state of charge").rate().equal(Real.Value(4.0)).shiftBy(minute(2), minute(-20))
            }
        """,
        """
{
    "kind": "IntervalsExpressionShiftEdges",
    "expression": {
        "kind": "ExpressionEqual",
        "left": {
            "kind": "RealProfileRate",
            "profile": {
                "kind": "RealProfileResource",
                "name": "state of charge"
            }
        },
        "right": {
            "kind": "RealProfileValue",
            "value": 4,
            "rate": 0
        }
    },
    "fromStart": 120000000,
    "fromEnd": -1200000000
}"""
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
        """
{
    "kind": "ExpressionEqual",
    "left": {
        "kind": "RealProfileTimes",
        "multiplier": 2,
        "profile": {
            "kind": "RealProfileResource",
            "name": "state of charge"
        }
    },
    "right": {
        "kind": "RealProfileValue",
        "value": 4,
        "rate": 0
    }
}"""
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
        """
{
    "kind": "ExpressionEqual",
    "left": {
        "kind": "RealProfilePlus",
        "left": {
            "kind": "RealProfileResource",
            "name": "state of charge"
        },
        "right": {
            "kind": "RealProfileValue",
            "value": 2,
            "rate": 0
        }
    },
    "right": {
        "kind": "RealProfileValue",
        "value": 4,
        "rate": 0
    }
}"""
    );
  }

  @Test
  void testNegate() {
    checkSuccessfulCompilation(
        """
            export default() => {
              return Real.Resource("state of charge").negate().equal(Real.Value(4.0))
            }
        """,
        """
{
    "kind": "ExpressionEqual",
    "left": {
        "kind": "RealProfileTimes",
        "profile": {
            "kind": "RealProfileResource",
            "name": "state of charge"
        },
        "multiplier": -1
    },
    "right": {
        "kind": "RealProfileValue",
        "value": 4,
        "rate": 0
    }
}"""
    );
  }

  @Test
  void testMinus() {
    checkSuccessfulCompilation(
        """
            export default() => {
              return Real.Resource("state of charge").minus(2.0).equal(4.0)
            }
        """,
        """
{
    "kind": "ExpressionEqual",
    "left": {
        "kind": "RealProfilePlus",
        "left": {
            "kind": "RealProfileResource",
            "name": "state of charge"
        },
        "right": {
            "kind": "RealProfileValue",
            "value": -2,
            "rate": 0
        }
    },
    "right": {
        "kind": "RealProfileValue",
        "value": 4,
        "rate": 0
    }
}"""
    );

    checkSuccessfulCompilation(
        """
            export default() => {
              return Real.Resource("state of charge").minus(Real.Resource("state of charge")).equal(4.0)
            }
        """,
        """
{
    "kind": "ExpressionEqual",
    "left": {
        "kind": "RealProfilePlus",
        "left": {
            "kind": "RealProfileResource",
            "name": "state of charge"
        },
        "right": {
            "kind": "RealProfileTimes",
            "profile": {
                "kind": "RealProfileResource",
                "name": "state of charge"
            },
            "multiplier": -1
        }
    },
    "right": {
        "kind": "RealProfileValue",
        "value": 4,
        "rate": 0
    }
}"""
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
        """
{
    "kind": "RealProfileLessThan",
    "left": {
        "kind": "RealProfileResource",
        "name": "state of charge"
    },
    "right": {
        "kind": "RealProfileValue",
        "value": 2,
        "rate": 0
    }
}"""
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
        """
{
    "kind": "RealProfileLessThanOrEqual",
    "left": {
        "kind": "RealProfileResource",
        "name": "state of charge"
    },
    "right": {
        "kind": "RealProfileValue",
        "value": 2,
        "rate": 0
    }
}"""
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
        """
{
    "kind": "RealProfileGreaterThan",
    "left": {
        "kind": "RealProfileResource",
        "name": "state of charge"
    },
    "right": {
        "kind": "RealProfileValue",
        "value": 2,
        "rate": 0
    }
}"""
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
        """
{
    "kind": "RealProfileGreaterThanOrEqual",
    "left": {
        "kind": "RealProfileResource",
        "name": "state of charge"
    },
    "right": {
        "kind": "RealProfileValue",
        "value": 2,
        "rate": 0
    }
}"""
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
        """
{
    "kind": "ExpressionEqual",
    "left": {
        "kind": "RealProfileResource",
        "name": "state of charge"
    },
    "right": {
        "kind": "RealProfileValue",
        "value": -1,
        "rate": 0
    }
}"""
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
        """
{
    "kind": "ExpressionNotEqual",
    "left": {
        "kind": "RealProfileResource",
        "name": "an integer"
    },
    "right": {
        "kind": "RealProfileValue",
        "value": -1,
        "rate": 0
    }
}"""
    );
  }

  @Test
  void testRealIsWithin() {
    checkSuccessfulCompilation(
        """
        export default () => {
          return Real.Resource("an integer").isWithin(Real.Value(10), Real.Value(1));
        }
        """,
        """
{
    "kind": "WindowsExpressionAnd",
    "expressions": [
        {
            "kind": "RealProfileLessThanOrEqual",
            "left": {
                "kind": "RealProfileResource",
                "name": "an integer"
            },
            "right": {
                "kind": "RealProfilePlus",
                "left": {
                    "kind": "RealProfileValue",
                    "value": 10,
                    "rate": 0
                },
                "right": {
                    "kind": "RealProfileValue",
                    "value": 1,
                    "rate": 0
                }
            }
        },
        {
            "kind": "RealProfileGreaterThanOrEqual",
            "left": {
                "kind": "RealProfileResource",
                "name": "an integer"
            },
            "right": {
                "kind": "RealProfilePlus",
                "left": {
                    "kind": "RealProfileValue",
                    "value": 10,
                    "rate": 0
                },
                "right": {
                    "kind": "RealProfileTimes",
                    "profile": {
                        "kind": "RealProfileValue",
                        "value": 1,
                        "rate": 0
                    },
                    "multiplier": -1
                }
            }
        }
    ]
}"""
    );
  }

  @Test
  void testRealChanges() {
    checkSuccessfulCompilation(
        """
            export default () => {
              return Real.Value(4).changes()
            }
        """,
        """
{
    "kind": "ProfileChanges",
    "expression": {
        "kind": "RealProfileValue",
        "value": 4,
        "rate": 0
    }
}"""
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
        """
            {
              "kind": "RealProfileLessThan",
              "left": {
                "kind": "RealProfilePlus",
                "left": {
                  "kind": "RealProfileValue",
                  "value": 2.2,
                  "rate": 0
                },
                "right": {
                  "kind": "RealProfileValue",
                  "value": -3,
                  "rate": 0
                }
              },
              "right": {
                "kind": "RealProfileValue",
                "value": 5,
                "rate": 0
              }
            }"""
    );
  }

  @Test
  void testRealAssignGaps() {
    checkSuccessfulCompilation(
        """
          export default () => {
            return Real.Resource("an integer").assignGaps(0).lessThan(5);
          }
        """,
        """
{
    "kind": "RealProfileLessThan",
    "left": {
        "kind": "AssignGapsExpression",
        "originalProfile": {
            "kind": "RealProfileResource",
            "name": "an integer"
        },
        "defaultProfile": {
            "kind": "RealProfileValue",
            "value": 0,
            "rate": 0
        }
    },
    "right": {
        "kind": "RealProfileValue",
        "value": 5,
        "rate": 0
    }
}"""
    );

    checkSuccessfulCompilation(
        """
          export default () => {
            return Real.Resource("an integer").assignGaps(Real.Resource("an integer")).lessThan(5);
          }
        """,
        """
{
    "kind": "RealProfileLessThan",
    "left": {
        "kind": "AssignGapsExpression",
        "originalProfile": {
            "kind": "RealProfileResource",
            "name": "an integer"
        },
        "defaultProfile": {
            "kind": "RealProfileResource",
            "name": "an integer"
        }
    },
    "right": {
        "kind": "RealProfileValue",
        "value": 5,
        "rate": 0
    }
}"""
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
        """
{
    "kind": "ForEachActivityViolations",
    "activityType": "activity",
    "alias": "activity alias 0",
    "expression": {
        "kind": "WindowsExpressionActivityWindow",
        "alias": "activity alias 0"
    }
}"""
    );
  }

  @Test
  void testStartOf() {
    checkSuccessfulCompilation(
        """
          export default () => {
            return Constraint.ForEachActivity(ActivityType.activity, (alias) => alias.start().windows());
          }
        """,
        """
{
    "kind": "ForEachActivityViolations",
    "activityType": "activity",
    "alias": "activity alias 0",
    "expression": {
        "kind": "WindowsExpressionFromSpans",
        "spansExpression": {
            "kind": "IntervalsExpressionStarts",
            "expression": {
                "kind": "SpansExpressionActivitySpan",
                "alias": "activity alias 0"
            }
        }
    }
}"""
    );
  }

  @Test
  void testEndOf() {
    checkSuccessfulCompilation(
        """
          export default () => {
            return Constraint.ForEachActivity(ActivityType.activity, (alias) => alias.end().windows());
          }
        """,
        """
{
    "kind": "ForEachActivityViolations",
    "activityType": "activity",
    "alias": "activity alias 0",
    "expression": {
        "kind": "WindowsExpressionFromSpans",
        "spansExpression": {
            "kind": "IntervalsExpressionEnds",
            "expression": {
                "kind": "SpansExpressionActivitySpan",
                "alias": "activity alias 0"
            }
        }
    }
}"""
    );
  }

  @Test
  void testIf() {
    checkSuccessfulCompilation(
        """
          export default () => {
            return Real.Resource("state of charge").lessThan(2)
              .if(Discrete.Resource("mode").changes());
          }
        """,
        """
{
    "kind": "WindowsExpressionOr",
    "expressions": [
        {
            "kind": "WindowsExpressionNot",
            "expression": {
                "kind": "ProfileChanges",
                "expression": {
                    "kind": "DiscreteProfileResource",
                    "name": "mode"
                }
            }
        },
        {
            "kind": "RealProfileLessThan",
            "left": {
                "kind": "RealProfileResource",
                "name": "state of charge"
            },
            "right": {
                "kind": "RealProfileValue",
                "value": 2,
                "rate": 0
            }
        }
    ]
}"""
    );
  }

  @Test
  void testAnd() {
    checkSuccessfulCompilation(
        """
          export default () => {
            return Windows.And(
              Real.Resource("state of charge").lessThan(2),
              Discrete.Value("hello there").notEqual(Discrete.Value("hello there")),
              Real.Value(5).changes()
            );
          }
        """,
        """
{
    "kind": "WindowsExpressionAnd",
    "expressions": [
        {
            "kind": "RealProfileLessThan",
            "left": {
                "kind": "RealProfileResource",
                "name": "state of charge"
            },
            "right": {
                "kind": "RealProfileValue",
                "value": 2,
                "rate": 0
            }
        },
        {
            "kind": "ExpressionNotEqual",
            "left": {
                "kind": "DiscreteProfileValue",
                "value": "hello there"
            },
            "right": {
                "kind": "DiscreteProfileValue",
                "value": "hello there"
            }
        },
        {
            "kind": "ProfileChanges",
            "expression": {
                "kind": "RealProfileValue",
                "value": 5,
                "rate": 0
            }
        }
    ]
}"""
    );
  }

  @Test
  void testOr() {
    checkSuccessfulCompilation(
        """
          export default () => {
            return Windows.Or(
              Real.Resource("state of charge").lessThan(2),
              Discrete.Value("hello there").notEqual(Discrete.Value("hello there")),
              Real.Value(5).changes()
            );
          }
        """,
        """
{
    "kind": "WindowsExpressionOr",
    "expressions": [
        {
            "kind": "RealProfileLessThan",
            "left": {
                "kind": "RealProfileResource",
                "name": "state of charge"
            },
            "right": {
                "kind": "RealProfileValue",
                "value": 2,
                "rate": 0
            }
        },
        {
            "kind": "ExpressionNotEqual",
            "left": {
                "kind": "DiscreteProfileValue",
                "value": "hello there"
            },
            "right": {
                "kind": "DiscreteProfileValue",
                "value": "hello there"
            }
        },
        {
            "kind": "ProfileChanges",
            "expression": {
                "kind": "RealProfileValue",
                "value": 5,
                "rate": 0
            }
        }
    ]
}"""
    );
  }

  @Test
  void testNot() {
    checkSuccessfulCompilation(
        """
          export default () => {
            return Discrete.Resource("mode").changes().not()
          }
        """,
        """
{
    "kind": "WindowsExpressionNot",
    "expression": {
        "kind": "ProfileChanges",
        "expression": {
            "kind": "DiscreteProfileResource",
            "name": "mode"
        }
    }
}"""
    );
  }

  @Test
  void testStarts() {
    checkSuccessfulCompilation(
        """
          export default () => {
            return Real.Resource("state of charge").lessThan(0.3).starts()
          }
        """,
        """
{
    "kind": "IntervalsExpressionStarts",
    "expression": {
        "kind": "RealProfileLessThan",
        "left": {
            "kind": "RealProfileResource",
            "name": "state of charge"
        },
        "right": {
            "kind": "RealProfileValue",
            "value": 0.3,
            "rate": 0
        }
    }
}"""
    );

    checkSuccessfulCompilation(
        """
          export default () => {
            return Real.Resource("state of charge").lessThan(0.3).spans().starts().windows()
          }
        """,
        """
{
    "kind": "WindowsExpressionFromSpans",
    "spansExpression": {
        "kind": "IntervalsExpressionStarts",
        "expression": {
            "kind": "SpansExpressionFromWindows",
            "windowsExpression": {
                "kind": "RealProfileLessThan",
                "left": {
                    "kind": "RealProfileResource",
                    "name": "state of charge"
                },
                "right": {
                    "kind": "RealProfileValue",
                    "value": 0.3,
                    "rate": 0
                }
            }
        }
    }
}"""
    );
  }
@Test
  void testKeepTrueSegment() {
    checkSuccessfulCompilation(
        """
          export default () => {
            return Real.Resource("state of charge").lessThan(0.3).starts().keepTrueSegment(2)
          }
        """,
        """
{
    "kind": "WindowsExpressionKeepTrueSegment",
    "expression": {
        "kind": "IntervalsExpressionStarts",
        "expression": {
            "kind": "RealProfileLessThan",
            "left": {
                "kind": "RealProfileResource",
                "name": "state of charge"
            },
            "right": {
                "kind": "RealProfileValue",
                "value": 0.3,
                "rate": 0
            }
        }
    },
    "index": 2
}"""
    );
  }

  @Test
  void testEnds() {
    checkSuccessfulCompilation(
        """
          export default () => {
            return Real.Resource("state of charge").lessThan(0.3).ends()
          }
        """,
        """
{
    "kind": "IntervalsExpressionEnds",
    "expression": {
        "kind": "RealProfileLessThan",
        "left": {
            "kind": "RealProfileResource",
            "name": "state of charge"
        },
        "right": {
            "kind": "RealProfileValue",
            "value": 0.3,
            "rate": 0
        }
    }
}"""
    );

    checkSuccessfulCompilation(
        """
          export default () => {
            return Real.Resource("state of charge").lessThan(0.3).spans().ends().windows()
          }
        """,
        """
{
    "kind": "WindowsExpressionFromSpans",
    "spansExpression": {
        "kind": "IntervalsExpressionEnds",
        "expression": {
            "kind": "SpansExpressionFromWindows",
            "windowsExpression": {
                "kind": "RealProfileLessThan",
                "left": {
                    "kind": "RealProfileResource",
                    "name": "state of charge"
                },
                "right": {
                    "kind": "RealProfileValue",
                    "value": 0.3,
                    "rate": 0
                }
            }
        }
    }
}"""
    );
  }

  @Test
  void testSplit() {
    checkSuccessfulCompilation(
        """
          export default () => {
            return Real.Resource("state of charge").lessThan(0.3).split(4).windows()
          }
        """,
        """
{
    "kind": "WindowsExpressionFromSpans",
    "spansExpression": {
        "kind": "SpansExpressionSplit",
        "intervals": {
            "kind": "RealProfileLessThan",
            "left": {
                "kind": "RealProfileResource",
                "name": "state of charge"
            },
            "right": {
                "kind": "RealProfileValue",
                "value": 0.3,
                "rate": 0
            }
        },
        "numberOfSubIntervals": 4,
        "internalStartInclusivity": "Inclusive",
        "internalEndInclusivity": "Exclusive"
    }
}"""
    );

    checkSuccessfulCompilation(
        """
          export default () => {
            return Real.Resource("state of charge").lessThan(0.3).spans().split(4).windows()
          }
        """,
        """
{
    "kind": "WindowsExpressionFromSpans",
    "spansExpression": {
        "kind": "SpansExpressionSplit",
        "intervals": {
            "kind": "SpansExpressionFromWindows",
            "windowsExpression": {
                "kind": "RealProfileLessThan",
                "left": {
                    "kind": "RealProfileResource",
                    "name": "state of charge"
                },
                "right": {
                    "kind": "RealProfileValue",
                    "value": 0.3,
                    "rate": 0
                }
            }
        },
        "numberOfSubIntervals": 4,
        "internalStartInclusivity": "Inclusive",
        "internalEndInclusivity": "Exclusive"
    }
}"""
    );

    checkSuccessfulCompilation(
        """
          export default () => {
            return Real.Resource("state of charge").lessThan(0.3).split(4, Inclusivity.Exclusive, Inclusivity.Inclusive).windows()
          }
        """,
        """
{
    "kind": "WindowsExpressionFromSpans",
    "spansExpression": {
        "kind": "SpansExpressionSplit",
        "intervals": {
            "kind": "RealProfileLessThan",
            "left": {
                "kind": "RealProfileResource",
                "name": "state of charge"
            },
            "right": {
                "kind": "RealProfileValue",
                "value": 0.3,
                "rate": 0
            }
        },
        "numberOfSubIntervals": 4,
        "internalStartInclusivity": "Exclusive",
        "internalEndInclusivity": "Inclusive"
    }
}"""
    );

    checkSuccessfulCompilation(
        """
          export default () => {
            return Real.Resource("state of charge").lessThan(0.3).spans().split(4, Inclusivity.Exclusive, Inclusivity.Exclusive).windows()
          }
        """,
        """
{
    "kind": "WindowsExpressionFromSpans",
    "spansExpression": {
        "kind": "SpansExpressionSplit",
        "intervals": {
            "kind": "SpansExpressionFromWindows",
            "windowsExpression": {
                "kind": "RealProfileLessThan",
                "left": {
                    "kind": "RealProfileResource",
                    "name": "state of charge"
                },
                "right": {
                    "kind": "RealProfileValue",
                    "value": 0.3,
                    "rate": 0
                }
            }
        },
        "numberOfSubIntervals": 4,
        "internalStartInclusivity": "Exclusive",
        "internalEndInclusivity": "Exclusive"
    }
}"""
    );
  }

  @Test
  void testSplitArgumentError() {
    checkFailedCompilation(
        """
          export default () => {
            return Real.Resource("state of charge").lessThan(0.3).split(0).windows()
          }
        """,
        ".split numberOfSubSpans cannot be less than 1, but was: 0"
    );

    checkFailedCompilation(
        """
          export default () => {
            return Real.Resource("state of charge").lessThan(0.3).split(-2).windows()
          }
        """,
        ".split numberOfSubSpans cannot be less than 1, but was: -2"
    );

    checkFailedCompilation(
        """
          export default () => {
            return Real.Resource("state of charge").lessThan(0.3).spans().split(0).windows()
          }
        """,
        ".split numberOfSubSpans cannot be less than 1, but was: 0"
    );

    checkFailedCompilation(
        """
          export default () => {
            return Real.Resource("state of charge").lessThan(0.3).spans().split(-2).windows()
          }
        """,
        ".split numberOfSubSpans cannot be less than 1, but was: -2"
    );
  }

  @Test
  void testAccumulatedDuration() {
    checkSuccessfulCompilation(
        """
          export default () => {
            return Real.Resource("state of charge").equal(4)
              .accumulatedDuration(Temporal.Duration.from({minutes: 1}))
              .lessThan(5);
          }
        """,
        """
{
    "kind": "RealProfileLessThan",
    "left": {
        "kind": "RealProfileAccumulatedDuration",
        "intervalsExpression": {
            "kind": "ExpressionEqual",
            "left": {
                "kind": "RealProfileResource",
                "name": "state of charge"
            },
            "right": {
                "kind": "RealProfileValue",
                "value": 4,
                "rate": 0
            }
        },
        "unit": 60000000
    },
    "right": {
        "kind": "RealProfileValue",
        "value": 5,
        "rate": 0
    }
}"""
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
        """
{
    "kind": "ViolationsOf",
    "expression": {
        "kind": "ExpressionEqual",
        "left": {
            "kind": "RealProfileResource",
            "name": "state of charge"
        },
        "right": {
            "kind": "RealProfileValue",
            "value": -1,
            "rate": 0
        }
    }
}"""
    );
  }

  @Test
  void testWindowsValue() {
    checkSuccessfulCompilation(
        """
          export default () => {
            return Windows.Value(false);
          }
        """,
        """
{
    "kind": "WindowsExpressionValue",
    "value": false
}"""
    );
  }

  @Test
  void testWindowsAssignGaps() {
    checkSuccessfulCompilation(
        """
          export default () => {
            return Real.Resource("an integer").lessThan(5).assignGaps(false);
          }
        """,
        """
{
    "kind": "AssignGapsExpression",
    "originalProfile": {
        "kind": "RealProfileLessThan",
        "left": {
            "kind": "RealProfileResource",
            "name": "an integer"
        },
        "right": {
            "kind": "RealProfileValue",
            "value": 5,
            "rate": 0
        }
    },
    "defaultProfile": {
        "kind": "WindowsExpressionValue",
        "value": false
    }
}"""
    );
  }

  //// TESTS FOR `Spans` API CLASS

  @Test
  void testSpansFromWindows() {
    checkSuccessfulCompilation(
        """
            export default () => {
                return Real.Resource("state of charge").equal(0.3).spans().windows();
            }
        """,
        """
{
    "kind": "WindowsExpressionFromSpans",
    "spansExpression": {
        "kind": "SpansExpressionFromWindows",
        "windowsExpression": {
            "kind": "ExpressionEqual",
            "left": {
                "kind": "RealProfileResource",
                "name": "state of charge"
            },
            "right": {
                "kind": "RealProfileValue",
                "value": 0.3,
                "rate": 0
            }
        }
    }
}"""
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
        """
{
    "kind": "ForEachActivityViolations",
    "activityType": "activity",
    "alias": "activity alias 0",
    "expression": {
        "kind": "ForEachActivityViolations",
        "activityType": "activity",
        "alias": "activity alias 1",
        "expression": {
            "kind": "WindowsExpressionNot",
            "expression": {
                "kind": "WindowsExpressionAnd",
                "expressions": [
                    {
                        "kind": "WindowsExpressionActivityWindow",
                        "alias": "activity alias 0"
                    },
                    {
                        "kind": "WindowsExpressionActivityWindow",
                        "alias": "activity alias 1"
                    }
                ]
            }
        }
    }
}"""
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
        """
{
    "kind": "ForEachActivityViolations",
    "activityType": "activity",
    "alias": "activity alias 0",
    "expression": {
        "kind": "WindowsExpressionActivityWindow",
        "alias": "activity alias 0"
    }
}"""
    );

    checkSuccessfulCompilation(
        """
        import { ActivityInstance } from './constraints-edsl-fluent-api.js';
        export default () => {
          return Constraint.ForEachActivity(
            ActivityType.activity,
            myHelperFunction
          )
        }

        function myHelperFunction(instance: ActivityInstance<ActivityType.activity>): Constraint {
          return instance.window();
        }
        """,
        """
{
    "kind": "ForEachActivityViolations",
    "activityType": "activity",
    "alias": "activity alias 0",
    "expression": {
        "kind": "WindowsExpressionActivityWindow",
        "alias": "activity alias 0"
    }
}"""
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
              (alias2) => Windows.And(alias1.window(), alias2.window())
            )
          )
        }
        """,
        """
{
    "kind": "ForEachActivityViolations",
    "activityType": "activity",
    "alias": "activity alias 0",
    "expression": {
        "kind": "ForEachActivityViolations",
        "activityType": "activity",
        "alias": "activity alias 1",
        "expression": {
            "kind": "WindowsExpressionAnd",
            "expressions": [
                {
                    "kind": "WindowsExpressionActivityWindow",
                    "alias": "activity alias 0"
                },
                {
                    "kind": "WindowsExpressionActivityWindow",
                    "alias": "activity alias 1"
                }
            ]
        }
    }
}"""
    );
  }

  @Test
  void testDuringedsl() {
    checkSuccessfulCompilation(
        """
        export default () => {
          return Windows.During(ActivityType.activity)
        }
        """,
        """
{
    "kind": "WindowsExpressionOr",
    "expressions": [
        {
            "kind": "WindowsExpressionFromSpans",
            "spansExpression": {
                "kind": "ForEachActivitySpans",
                "activityType": "activity",
                "alias": "span activity alias 0",
                "expression": {
                    "kind": "SpansExpressionActivitySpan",
                    "alias": "span activity alias 0"
                }
            }
        }
    ]
}"""
    );
  }

  @Test
  void testDuringAlledsl() {
    checkSuccessfulCompilation(
        """
        export default () => {
          return Windows.During(ActivityType.activity, ActivityType.activity2)
        }
        """,
        """
{
    "kind": "WindowsExpressionOr",
    "expressions": [
        {
            "kind": "WindowsExpressionFromSpans",
            "spansExpression": {
                "kind": "ForEachActivitySpans",
                "activityType": "activity",
                "alias": "span activity alias 0",
                "expression": {
                    "kind": "SpansExpressionActivitySpan",
                    "alias": "span activity alias 0"
                }
            }
        },
        {
            "kind": "WindowsExpressionFromSpans",
            "spansExpression": {
                "kind": "ForEachActivitySpans",
                "activityType": "activity2",
                "alias": "span activity alias 1",
                "expression": {
                    "kind": "SpansExpressionActivitySpan",
                    "alias": "span activity alias 1"
                }
            }
        }
    ]
}"""
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
          return Discrete.Resource("wrong resource").changes()
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
          return Real.Resource("mode").changes()
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
        "TypeError: TS2345 Argument of type 'Discrete<{ initial: number; rate: number; }>' is not assignable to parameter of type '\"Option1\" | \"Option2\" | Discrete<\"Option1\" | \"Option2\">'."
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

  @Test
  void testProfileShiftBy() {
    checkSuccessfulCompilation(
        """
        const minute = (m: number) => Temporal.Duration.from({minutes: m});
        export default() => {
          return Real.Resource("state of charge").shiftBy(minute(2)).equal(Real.Value(4.0))
        }
        """,
        """
{
    "kind": "ExpressionEqual",
    "left": {
        "kind": "ProfileExpressionShiftBy",
        "expression": {
            "kind": "RealProfileResource",
            "name": "state of charge"
        },
        "duration": 120000000
    },
    "right": {
        "kind": "RealProfileValue",
        "value": 4,
        "rate": 0
    }
}"""
    );

    checkSuccessfulCompilation(
        """
        const minute = (m: number) => Temporal.Duration.from({minutes: m});
        export default() => {
          return Discrete.Resource("mode").shiftBy(minute(2)).equal("Option1")
        }
        """,
        """
{
    "kind": "ExpressionEqual",
    "left": {
        "kind": "ProfileExpressionShiftBy",
        "expression": {
            "kind": "DiscreteProfileResource",
            "name": "mode"
        },
        "duration": 120000000
    },
    "right": {
        "kind": "DiscreteProfileValue",
        "value": "Option1"
    }
}"""
    );
  }

  @Test
  void testRollingThreshold() {
    final var algs = List.of(
        "ExcessHull",
        "ExcessSpans",
        "DeficitHull",
        "DeficitSpans"
    );
    for (final var entry: algs) {
      checkSuccessfulCompilation(
          """
              export default () => {
                return Constraint.RollingThreshold(
                  Spans.ForEachActivity(ActivityType.activity),
                  Temporal.Duration.from({hours: 1}),
                  Temporal.Duration.from({minutes: 5}),
                  RollingThresholdAlgorithm.%s
                );
              }
              """.formatted(entry),
          """
{
    "kind": "RollingThreshold",
    "spans": {
        "kind": "ForEachActivitySpans",
        "activityType": "activity",
        "alias": "span activity alias 0",
        "expression": {
            "kind": "SpansExpressionActivitySpan",
            "alias": "span activity alias 0"
        }
    },
    "width": 3600000000,
    "threshold": 300000000,
    "algorithm": "%s"
}""".formatted(entry)
      );
    }
  }

  @Test
  void testSpansShiftBy() {
    checkSuccessfulCompilation(
        """
        const minute = (m: number) => Temporal.Duration.from({minutes: m});
        export default() => {
          return Spans.ForEachActivity(ActivityType.activity, i => i.span()).shiftBy(minute(2)).windows();
        }
        """,
        """
{
    "kind": "WindowsExpressionFromSpans",
    "spansExpression": {
        "kind": "IntervalsExpressionShiftEdges",
        "expression": {
            "kind": "ForEachActivitySpans",
            "activityType": "activity",
            "alias": "span activity alias 0",
            "expression": {
                "kind": "SpansExpressionActivitySpan",
                "alias": "span activity alias 0"
            }
        },
        "fromStart": 120000000,
        "fromEnd": 120000000
    }
}"""
    );

    checkSuccessfulCompilation(
        """
        const minute = (m: number) => Temporal.Duration.from({minutes: m});
        export default() => {
          return Spans.ForEachActivity(ActivityType.activity, i => i.span()).shiftBy(minute(2), minute(3)).windows();
        }
        """,
        """
{
    "kind": "WindowsExpressionFromSpans",
    "spansExpression": {
        "kind": "IntervalsExpressionShiftEdges",
        "expression": {
            "kind": "ForEachActivitySpans",
            "activityType": "activity",
            "alias": "span activity alias 0",
            "expression": {
                "kind": "SpansExpressionActivitySpan",
                "alias": "span activity alias 0"
            }
        },
        "fromStart": 120000000,
        "fromEnd": 180000000
    }
}"""
    );
  }

  @Test
  void testSpansSelectWhenTrue() {
    checkSuccessfulCompilation(
        """
        const minute = (m: number) => Temporal.Duration.from({minutes: m});
        export default() => {
          return Spans.ForEachActivity(ActivityType.activity, i => i.span()).selectWhenTrue(Windows.Value(true)).windows()
        }
        """,
        """
{
    "kind": "WindowsExpressionFromSpans",
    "spansExpression": {
        "kind": "SpansSelectWhenTrue",
        "spansExpression": {
            "kind": "ForEachActivitySpans",
            "activityType": "activity",
            "alias": "span activity alias 0",
            "expression": {
                "kind": "SpansExpressionActivitySpan",
                "alias": "span activity alias 0"
            }
        },
        "windowsExpression": {
            "kind": "WindowsExpressionValue",
            "value": true
        }
    }
}"""
    );
  }
  @Test
  void testSpansConnectTo() {
    checkSuccessfulCompilation(
        """
          export default () => {
            return Windows.Value(true).spans().connectTo(
              Windows.Value(false).spans()
            ).windows();
          }
        """,
        """
            {
              "kind": "WindowsExpressionFromSpans",
              "spansExpression": {
                "kind": "SpansExpressionConnectTo",
                "from": {
                  "kind": "SpansExpressionFromWindows",
                  "windowsExpression": {
                    "kind": "WindowsExpressionValue",
                    "value": true
                  }
                },
                "to": {
                  "kind": "SpansExpressionFromWindows",
                  "windowsExpression": {
                    "kind": "WindowsExpressionValue",
                    "value": false
                  }
                }
              }
            }"""
    );
  }

  @Test
  void testSpansContains() {
    checkSuccessfulCompilation(
        """
          export default () => {
            return Spans.ForEachActivity(ActivityType.activity).contains(Spans.ForEachActivity(ActivityType.activity));
          }
        """,
        """
{
    "kind": "SpansExpressionContains",
    "parents": {
        "kind": "ForEachActivitySpans",
        "activityType": "activity",
        "alias": "span activity alias 0",
        "expression": {
            "kind": "SpansExpressionActivitySpan",
            "alias": "span activity alias 0"
        }
    },
    "children": {
        "kind": "ForEachActivitySpans",
        "activityType": "activity",
        "alias": "span activity alias 1",
        "expression": {
            "kind": "SpansExpressionActivitySpan",
            "alias": "span activity alias 1"
        }
    },
    "requirement": {
        "count": {
            "min": 1
        },
        "duration": {
        }
    }
}"""
    );

    checkSuccessfulCompilation(
        """
          export default () => {
            return Spans.ForEachActivity(ActivityType.activity).contains(Spans.ForEachActivity(ActivityType.activity), {count: 3});
          }
        """,
        """
{
    "kind": "SpansExpressionContains",
    "parents": {
        "kind": "ForEachActivitySpans",
        "activityType": "activity",
        "alias": "span activity alias 0",
        "expression": {
            "kind": "SpansExpressionActivitySpan",
            "alias": "span activity alias 0"
        }
    },
    "children": {
        "kind": "ForEachActivitySpans",
        "activityType": "activity",
        "alias": "span activity alias 1",
        "expression": {
            "kind": "SpansExpressionActivitySpan",
            "alias": "span activity alias 1"
        }
    },
    "requirement": {
        "count": {
            "min": 3,
            "max": 3
        },
        "duration": {
        }
    }
}"""
    );

    checkSuccessfulCompilation(
        """
          export default () => {
            return Spans.ForEachActivity(ActivityType.activity).contains(Spans.ForEachActivity(ActivityType.activity), {duration: {min: Temporal.Duration.from({minutes: 1})}});
          }
        """,
        """
{
    "kind": "SpansExpressionContains",
    "parents": {
        "kind": "ForEachActivitySpans",
        "activityType": "activity",
        "alias": "span activity alias 0",
        "expression": {
            "kind": "SpansExpressionActivitySpan",
            "alias": "span activity alias 0"
        }
    },
    "children": {
        "kind": "ForEachActivitySpans",
        "activityType": "activity",
        "alias": "span activity alias 1",
        "expression": {
            "kind": "SpansExpressionActivitySpan",
            "alias": "span activity alias 1"
        }
    },
    "requirement": {
        "duration": {
            "min": 60000000
        },
        "count": {
        }
    }
}"""
    );

    checkSuccessfulCompilation(
        """
          export default () => {
            return Spans.ForEachActivity(ActivityType.activity).contains(Spans.ForEachActivity(ActivityType.activity), {
              count: {min: 1, max: 2},
              duration: {min: Temporal.Duration.from({minutes: 1}), max: Temporal.Duration.from({minutes: 2})}
            });
          }
        """,
        """
{
    "kind": "SpansExpressionContains",
    "parents": {
        "kind": "ForEachActivitySpans",
        "activityType": "activity",
        "alias": "span activity alias 0",
        "expression": {
            "kind": "SpansExpressionActivitySpan",
            "alias": "span activity alias 0"
        }
    },
    "children": {
        "kind": "ForEachActivitySpans",
        "activityType": "activity",
        "alias": "span activity alias 1",
        "expression": {
            "kind": "SpansExpressionActivitySpan",
            "alias": "span activity alias 1"
        }
    },
    "requirement": {
        "count": {
            "min": 1,
            "max": 2
        },
        "duration": {
            "min": 60000000,
            "max": 120000000
        }
    }
}"""
    );
  }
}
