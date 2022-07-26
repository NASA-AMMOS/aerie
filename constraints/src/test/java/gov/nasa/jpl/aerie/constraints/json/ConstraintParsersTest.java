package gov.nasa.jpl.aerie.constraints.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nasa.jpl.aerie.constraints.tree.All;
import gov.nasa.jpl.aerie.constraints.tree.Changes;
import gov.nasa.jpl.aerie.constraints.tree.ConstraintExpression;
import gov.nasa.jpl.aerie.constraints.tree.DiscreteParameter;
import gov.nasa.jpl.aerie.constraints.tree.DiscreteProfileExpression;
import gov.nasa.jpl.aerie.constraints.tree.DiscreteResource;
import gov.nasa.jpl.aerie.constraints.tree.DiscreteValue;
import gov.nasa.jpl.aerie.constraints.tree.ActivityWindow;
import gov.nasa.jpl.aerie.constraints.tree.EndOf;
import gov.nasa.jpl.aerie.constraints.tree.Equal;
import gov.nasa.jpl.aerie.constraints.tree.ForEachActivity;
import gov.nasa.jpl.aerie.constraints.tree.GreaterThan;
import gov.nasa.jpl.aerie.constraints.tree.GreaterThanOrEqual;
import gov.nasa.jpl.aerie.constraints.tree.LessThan;
import gov.nasa.jpl.aerie.constraints.tree.LessThanOrEqual;
import gov.nasa.jpl.aerie.constraints.tree.Invert;
import gov.nasa.jpl.aerie.constraints.tree.LinearProfileExpression;
import gov.nasa.jpl.aerie.constraints.tree.NotEqual;
import gov.nasa.jpl.aerie.constraints.tree.Any;
import gov.nasa.jpl.aerie.constraints.tree.Plus;
import gov.nasa.jpl.aerie.constraints.tree.Rate;
import gov.nasa.jpl.aerie.constraints.tree.RealParameter;
import gov.nasa.jpl.aerie.constraints.tree.RealResource;
import gov.nasa.jpl.aerie.constraints.tree.RealValue;
import gov.nasa.jpl.aerie.constraints.tree.StartOf;
import gov.nasa.jpl.aerie.constraints.tree.Times;
import gov.nasa.jpl.aerie.constraints.tree.Transition;
import gov.nasa.jpl.aerie.constraints.tree.ViolationsOf;
import gov.nasa.jpl.aerie.constraints.tree.WindowsExpression;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.junit.jupiter.api.Test;

import static gov.nasa.jpl.aerie.constraints.Assertions.assertEquivalent;

public final class ConstraintParsersTest {
  private static final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void testParseDiscreteValue() throws JsonProcessingException {
    final var input = """
        {
            "kind": ".DiscreteValue",
            "value": false
        }
        """;

    final var expected =
        new DiscreteValue(SerializedValue.of(false));

    assertEquivalent(expected, mapper.readValue(input, DiscreteProfileExpression.class));
  }

  @Test
  public void testParseChanges() throws JsonProcessingException {
    final var input = """
        {
            "kind": ".Changes",
            "expression": {
                "kind": ".DiscreteValue",
                "value": false
            }
        }
        """;

    final var expected =
        new Changes<>(new DiscreteValue(SerializedValue.of(false)));

    assertEquivalent(expected, mapper.readValue(input, WindowsExpression.class));
  }

  @Test
  public void testParseDiscreteResource() throws JsonProcessingException {
    final var input = """
        {
            "kind": ".DiscreteResource",
            "name": "ResA"
        }
        """;

    final var expected =
        new DiscreteResource("ResA");

    assertEquivalent(expected, mapper.readValue(input, DiscreteProfileExpression.class));
  }

  @Test
  public void testParseTransition() throws JsonProcessingException {
    final var input = """
        {
            "kind": ".Transition",
            "profile": {
                "kind": ".DiscreteResource",
                "name": "ResA"
            },
            "oldState": "old",
            "newState": "new"
        }
        """;

    final var expected =
        new Transition(
            new DiscreteResource("ResA"),
            SerializedValue.of("old"),
            SerializedValue.of("new")
        );

    assertEquivalent(expected, mapper.readValue(input, WindowsExpression.class));
  }

  @Test
  public void testParseRealParameter() throws JsonProcessingException {
    final var input = """
        {
            "kind": ".RealParameter",
            "activityAlias": "act",
            "parameterName": "pJones"
        }
        """;

    final var expected =
        new RealParameter("act", "pJones");

    assertEquivalent(expected, mapper.readValue(input, LinearProfileExpression.class));
  }

  @Test
  public void testParseRealValue() throws JsonProcessingException {
    final var input = """
        {
            "kind": ".RealValue",
            "value": 3.4
        }
        """;

    final var expected =
        new RealValue(3.4);

    assertEquivalent(expected, mapper.readValue(input, LinearProfileExpression.class));
  }

  @Test
  public void testParseRealResource() throws JsonProcessingException {
    final var input = """
        {
            "kind": ".RealResource",
            "name": "ResA"
        }
        """;

    final var expected =
        new RealResource("ResA");

    assertEquivalent(expected, mapper.readValue(input, LinearProfileExpression.class));
  }

  @Test
  public void testParsePlus() throws JsonProcessingException {
    final var input = """
        {
            "kind": ".Plus",
            "left": {
                "kind": ".RealResource",
                "name": "ResA"
            },
            "right": {
                "kind": ".RealResource",
                "name": "ResB"
            }
        }
        """;

    final var expected =
        new Plus(
            new RealResource("ResA"),
            new RealResource("ResB")
        );

    assertEquivalent(expected, mapper.readValue(input, LinearProfileExpression.class));
  }

  @Test
  public void testParseTimes() throws JsonProcessingException {
    final var input = """
        {
            "kind": ".Times",
            "profile": {
                "kind": ".RealResource",
                "name": "ResA"
            },
            "multiplier": 2.7
        }
        """;

    final var expected =
        new Times(
            new RealResource("ResA"),
            2.7
        );

    assertEquivalent(expected, mapper.readValue(input, LinearProfileExpression.class));
  }

  @Test
  public void testParseRate() throws JsonProcessingException {
    final var input = """
        {
            "kind": ".Rate",
            "profile": {
                "kind": ".RealResource",
                "name": "ResA"
            }
        }
        """;

    final var expected =
        new Rate(
            new RealResource("ResA")
        );

    assertEquivalent(expected, mapper.readValue(input, LinearProfileExpression.class));
  }

  @Test
  public void testParseDuring() throws JsonProcessingException {
    final var input = """
        {
            "kind": ".ActivityWindow",
            "activityAlias": "TEST"
        }
        """;

    final var expected =
        new ActivityWindow("TEST");

    assertEquivalent(expected, mapper.readValue(input, WindowsExpression.class));
  }

  @Test
  public void testParseStartOf() throws JsonProcessingException {
    final var input = """
        {
            "kind": ".StartOf",
            "activityAlias": "TEST"
        }
        """;

    final var expected =
        new StartOf("TEST");

    assertEquivalent(expected, mapper.readValue(input, WindowsExpression.class));
  }

  @Test
  public void testParseEndOf() throws JsonProcessingException {
    final var input = """
        {
            "kind": ".EndOf",
            "activityAlias": "TEST"
        }
        """;

    final var expected =
        new EndOf("TEST");

    assertEquivalent(expected, mapper.readValue(input, WindowsExpression.class));
  }

  @Test
  public void testParseParameter() throws JsonProcessingException {
    final var input = """
        {
            "kind": ".DiscreteParameter",
            "activityAlias": "TEST",
            "parameterName": "paramesan"
        }
        """;

    final var expected =
        new DiscreteParameter("TEST", "paramesan");

    assertEquivalent(expected, mapper.readValue(input, DiscreteProfileExpression.class));
  }

  @Test
  public void testParseEqual() throws JsonProcessingException {
    final var input = """
        {
            "kind": ".Equal",
            "left": {
                "kind": ".RealResource",
                "name": "ResA"
            },
            "right": {
                "kind": ".RealResource",
                "name": "ResB"
            }
        }
        """;

    final var expected =
        new Equal<>(
            new RealResource("ResA"),
            new RealResource("ResB")
        );

    assertEquivalent(expected, mapper.readValue(input, WindowsExpression.class));
  }

  @Test
  public void testParseNotEqual() throws JsonProcessingException {
    final var input = """
        {
            "kind": ".NotEqual",
            "left": {
                "kind": ".DiscreteResource",
                "name": "ResA"
            },
            "right": {
                "kind": ".DiscreteResource",
                "name": "ResB"
            }
        }
        """;

    final var expected =
        new NotEqual<>(
            new DiscreteResource("ResA"),
            new DiscreteResource("ResB")
        );

    assertEquivalent(expected, mapper.readValue(input, WindowsExpression.class));
  }

  @Test
  public void testParseLessThan() throws JsonProcessingException {
    final var input = """
        {
            "kind": ".LessThan",
            "left": {
                "kind": ".RealResource",
                "name": "ResA"
            },
            "right": {
                "kind": ".RealResource",
                "name": "ResB"
            }
        }
        """;

    final var expected =
        new LessThan(
            new RealResource("ResA"),
            new RealResource("ResB")
        );

    assertEquivalent(expected, mapper.readValue(input, WindowsExpression.class));
  }

  @Test
  public void testParseLessThanOrEqual() throws JsonProcessingException {
    final var input = """
        {
            "kind": ".LessThanOrEqual",
            "left": {
                "kind": ".RealResource",
                "name": "ResA"
            },
            "right": {
                "kind": ".RealResource",
                "name": "ResB"
            }
        }
        """;

    final var expected =
        new LessThanOrEqual(
            new RealResource("ResA"),
            new RealResource("ResB")
        );

    assertEquivalent(expected, mapper.readValue(input, WindowsExpression.class));
  }

  @Test
  public void testParseGreaterThan() throws JsonProcessingException {
    final var input = """
        {
            "kind": ".GreaterThan",
            "left": {
                "kind": ".RealResource",
                "name": "ResA"
            },
            "right": {
                "kind": ".RealResource",
                "name": "ResB"
            }
        }
        """;

    final var expected =
        new GreaterThan(
            new RealResource("ResA"),
            new RealResource("ResB")
        );

    assertEquivalent(expected, mapper.readValue(input, WindowsExpression.class));
  }

  @Test
  public void testParseGreaterThanOrEqual() throws JsonProcessingException {
    final var input = """
        {
            "kind": ".GreaterThanOrEqual",
            "left": {
                "kind": ".RealResource",
                "name": "ResA"
            },
            "right": {
                "kind": ".RealResource",
                "name": "ResB"
            }
        }
        """;

    final var expected =
        new GreaterThanOrEqual(
            new RealResource("ResA"),
            new RealResource("ResB")
        );

    assertEquivalent(expected, mapper.readValue(input, WindowsExpression.class));
  }

  @Test
  public void testParseAll() throws JsonProcessingException {
    final var input = """
        {
            "kind": ".All",
            "expressions": [
                {
                    "kind": ".ActivityWindow",
                    "activityAlias": "A"
                },
                {
                    "kind": ".ActivityWindow",
                    "activityAlias": "B"
                }
            ]
        }
        """;

    final var expected =
        new All(
            new ActivityWindow("A"),
            new ActivityWindow("B")
        );

    assertEquivalent(expected, mapper.readValue(input, WindowsExpression.class));
  }

  @Test
  public void testParseAny() throws JsonProcessingException {
    final var input = """
        {
            "kind": ".Any",
            "expressions": [
                {
                    "kind": ".ActivityWindow",
                    "activityAlias": "A"
                },
                {
                    "kind": ".ActivityWindow",
                    "activityAlias": "B"
                }
            ]
        }
        """;

    final var expected =
        new Any(
            new ActivityWindow("A"),
            new ActivityWindow("B")
        );

    assertEquivalent(expected, mapper.readValue(input, WindowsExpression.class));
  }

  @Test
  public void testParseInvert() throws JsonProcessingException {
    final var input = """
        {
            "kind": ".Invert",
            "expression": {
                "kind": ".ActivityWindow",
                "activityAlias": "A"
            }
        }
        """;

    final var expected =
        new Invert(
            new ActivityWindow("A")
        );

    assertEquivalent(expected, mapper.readValue(input, WindowsExpression.class));
  }

  @Test
  public void testForEachActivity() throws JsonProcessingException {
    final var input = """
        {
            "kind": ".ForEachActivity",
            "activityType": "TypeA",
            "alias": "A",
            "expression": {
                "kind": ".ViolationsOf",
                "expression": {
                    "kind": ".ActivityWindow",
                    "activityAlias": "A"
                }
            }
        }
        """;

    final var expected =
        new ForEachActivity(
            "TypeA",
            "A",
            new ViolationsOf(
                new ActivityWindow("A")));

    assertEquivalent(expected, mapper.readValue(input, ConstraintExpression.class));
  }

  @Test
  public void testParseMassiveExpression() throws JsonProcessingException {
    final var input = """
        {
           "kind": ".ForEachActivity",
           "activityType": "TypeA",
           "alias": "A",
           "expression": {
               "kind": ".ForEachActivity",
               "activityType": "TypeB",
               "alias": "B",
               "expression": {
                   "kind": ".ViolationsOf",
                   "expression": {
                       "kind": ".Any",
                       "expressions": [
                           {
                               "kind": ".Any",
                               "expressions": [
                                   {
                                       "kind": ".Invert",
                                       "expression": {
                                           "kind": ".LessThan",
                                           "left": {
                                               "kind": ".Times",
                                               "profile": {
                                                   "kind": ".RealResource",
                                                   "name": "ResC"
                                               },
                                               "multiplier": 2
                                           },
                                           "right": {
                                               "kind": ".RealParameter",
                                               "activityAlias": "A",
                                               "parameterName": "a"
                                           }
                                       }
                                   },
                                   {
                                       "kind": ".Equal",
                                       "left": {
                                           "kind": ".DiscreteValue",
                                           "value": false
                                       },
                                       "right": {
                                           "kind": ".DiscreteParameter",
                                           "activityAlias": "B",
                                           "parameterName": "b"
                                       }
                                   }
                               ]
                           },
                           {
                               "kind": ".Invert",
                               "expression": {
                                   "kind": ".ActivityWindow",
                                   "activityAlias": "A"
                               }
                           },
                           {
                               "kind": ".Invert",
                               "expression": {
                                   "kind": ".ActivityWindow",
                                   "activityAlias": "B"
                               }
                           }
                       ]
                   }
               }
           }
        }
        """;

    final var expected = new ForEachActivity(
        "TypeA",
        "A",
        new ForEachActivity(
            "TypeB",
            "B",
            new ViolationsOf(
                new Any(
                    new Any(
                        new Invert(
                            new LessThan(
                                new Times(
                                    new RealResource("ResC"),
                                    2),
                                new RealParameter("A", "a"))),
                        new Equal<>(
                            new DiscreteValue(SerializedValue.of(false)),
                            new DiscreteParameter("B", "b"))),
                    new Invert(new ActivityWindow("A")),
                    new Invert(new ActivityWindow("B"))))));

    assertEquivalent(expected, mapper.readValue(input, ConstraintExpression.class));
  }
}
