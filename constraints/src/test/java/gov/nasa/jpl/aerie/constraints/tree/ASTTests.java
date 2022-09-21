package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.InputMismatchException;
import gov.nasa.jpl.aerie.constraints.UnsplittableIntervalException;
import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfile;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfilePiece;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.LinearProfilePiece;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Segment;
import gov.nasa.jpl.aerie.constraints.time.Spans;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static gov.nasa.jpl.aerie.constraints.Assertions.assertEquivalent;
import static gov.nasa.jpl.aerie.constraints.time.Interval.FOREVER;
import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Inclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.at;
import static gov.nasa.jpl.aerie.constraints.time.Interval.interval;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class ASTTests {

  @Test
  public void testNot() {
    final var simResults = new SimulationResults(
        Interval.between(0, 20, SECONDS),
        List.of(),
        Map.of(),
        Map.of()
    );

    final var windows = new Windows()
        .set(Interval.between(0, Inclusive, 5, Exclusive, SECONDS), true)
        .set(Interval.between(10, Exclusive, 15, Exclusive, SECONDS), false)
        .set(Interval.at(20, SECONDS), true);

    final var result = new Invert(Supplier.of(windows)).evaluate(simResults, Map.of());

    final var expected = new Windows()
        .set(Interval.between(0, Inclusive, 5, Exclusive, SECONDS), false)
        .set(Interval.between(10, Exclusive, 15, Exclusive, SECONDS), true)
        .set(Interval.at(20, SECONDS), false);

    assertEquivalent(expected, result);
  }

  @Test
  public void testSplitWindows() {
    final var simResults = new SimulationResults(
        Window.between(0, 20, SECONDS),
        List.of(),
        Map.of(),
        Map.of()
    );

    final var windows = new Windows();
    windows.add(Window.between(0, Inclusive, 5, Exclusive, SECONDS));
    windows.add(Window.between(10000000, Exclusive, 15000001, Exclusive, MICROSECONDS));

    final var result = new Split<Windows>(Supplier.of(windows), 3).evaluate(simResults, Map.of());

    final var expected = new Windows();
    expected.add(Window.between(0, Inclusive, 1666666, Exclusive, MICROSECONDS));
    expected.add(Window.between(1666666, Exclusive, 3333332, Exclusive, MICROSECONDS));
    expected.add(Window.between(3333332, Exclusive, 5000000, Exclusive, MICROSECONDS));

    expected.add(Window.between(10000000, Exclusive, 11666667, Exclusive, MICROSECONDS));
    expected.add(Window.between(11666667, Exclusive, 13333334, Exclusive, MICROSECONDS));
    expected.add(Window.between(13333334, Exclusive, 15000001, Exclusive, MICROSECONDS));

    assertEquivalent(expected, result);
  }

  @Test
  public void testSplitSpans() {
    final var simResults = new SimulationResults(
        Window.between(0, 20, SECONDS),
        List.of(),
        Map.of(),
        Map.of()
    );

    final var spans = new Spans();
    spans.add(Window.between(0, Inclusive, 5, Exclusive, SECONDS));
    spans.add(Window.between(0, Exclusive, 5000001, Exclusive, MICROSECONDS));

    final var result = new Split<Spans>(Supplier.of(spans), 3).evaluate(simResults, Map.of());

    final var expected = new Spans();
    expected.add(Window.between(0, Inclusive, 1666666, Exclusive, MICROSECONDS));
    expected.add(Window.between(1666666, Exclusive, 3333332, Exclusive, MICROSECONDS));
    expected.add(Window.between(3333332, Exclusive, 5000000, Exclusive, MICROSECONDS));

    expected.add(Window.between(0, Exclusive, 1666667, Exclusive, MICROSECONDS));
    expected.add(Window.between(1666667, Exclusive, 3333334, Exclusive, MICROSECONDS));
    expected.add(Window.between(3333334, Exclusive, 5000001, Exclusive, MICROSECONDS));

    assertEquivalent(expected, result);
  }

  @Test
  public void testUnsplittableInterval() {
    final var simResults = new SimulationResults(
        Window.between(0, 20, SECONDS),
        List.of(),
        Map.of(),
        Map.of()
    );

    final var spans = new Spans(
        Window.at(5, SECONDS)
    );

    assertThrows(UnsplittableIntervalException.class, () -> new Split<Spans>(Supplier.of(spans), 3).evaluate(simResults, Map.of()));
  }

  @Test
  public void testAnd() {
    final var simResults = new SimulationResults(
        Interval.between(0, 20, SECONDS),
        List.of(),
        Map.of(),
        Map.of()
    );

    final var left = new Windows()
        .set(Interval.between(0, Inclusive, 5, Exclusive, SECONDS), true)
        .set(Interval.between(6, Inclusive, 7, Inclusive, SECONDS), true)
        .set(Interval.between(8, Exclusive, 9, Exclusive, SECONDS), true)
        .set(Interval.between(10, Exclusive, 15, Exclusive, SECONDS), true)
        .set(Interval.at(20, SECONDS), true);

    final var right = new Windows()
        .set(Interval.between(0, Inclusive, 5, Inclusive, SECONDS), true)
        .set(Interval.between(7, Inclusive, 8, Exclusive, SECONDS), true)
        .set(Interval.between(10, Inclusive, 12, Inclusive, SECONDS), true)
        .set(Interval.between(15, Inclusive, 20, Exclusive, SECONDS), true);

    final var result = new All(Supplier.of(left), Supplier.of(right)).evaluate(simResults, Map.of());

    final var expected = new Windows()
        .set(Interval.between( 0, Inclusive,  5, Exclusive, SECONDS), true)
        .set(Interval.at(7, SECONDS), true)
        .set(Interval.between( 10, Exclusive,  12, Inclusive, SECONDS), true);

    assertEquivalent(expected, result);
  }

  @Test
  public void testOr() {
    final var simResults = new SimulationResults(
        Interval.between(0, 20, SECONDS),
        List.of(),
        Map.of(),
        Map.of()
    );

    final var left = new Windows()
        .set(Interval.between(0, Inclusive, 5, Exclusive, SECONDS), true)
        .set(Interval.between(6, Inclusive, 7, Inclusive, SECONDS), true)
        .set(Interval.between(8, Exclusive, 9, Exclusive, SECONDS), true)
        .set(Interval.between(10, Exclusive, 15, Exclusive, SECONDS), true)
        .set(Interval.at(20, SECONDS), true);

    final var right = new Windows()
        .set(Interval.between(0, Inclusive, 5, Inclusive, SECONDS), true)
        .set(Interval.between(7, Inclusive, 8, Exclusive, SECONDS), true)
        .set(Interval.between(10, Inclusive, 12, Inclusive, SECONDS), true)
        .set(Interval.between(15, Inclusive, 20, Exclusive, SECONDS), true);

    final var result = new Any(Supplier.of(left), Supplier.of(right)).evaluate(simResults, Map.of());

    final var expected = new Windows()
        .set(Interval.between(  0, Inclusive,   5, Inclusive, SECONDS), true)
        .set(Interval.between(  6, Inclusive,   8, Exclusive, SECONDS), true)
        .set(Interval.between(  8, Exclusive,   9, Exclusive, SECONDS), true)
        .set(Interval.between( 10, Inclusive,  20, Inclusive, SECONDS), true);

    assertEquivalent(expected, result);
  }

  @Test
  public void testExpandBy() {
    final var simResults = new SimulationResults(
        Interval.between(0, 20, SECONDS),
        List.of(),
        Map.of(),
        Map.of()
    );

    final var left = new Windows()
        .set(Interval.between(0, Inclusive, 5, Exclusive, SECONDS), true)
        .set(Interval.between(6, Inclusive, 7, Inclusive, SECONDS), true)
        .set(Interval.between(8, Exclusive, 9, Exclusive, SECONDS), true)
        .set(Interval.between(10, Exclusive, 15, Exclusive, SECONDS), true)
        .set(Interval.at(20, SECONDS), true)
        .set(interval(22, 23, SECONDS), false);

    final var expandByFromStart = Duration.of(-1, SECONDS);
    final var expandByFromEnd = Duration.of(0, SECONDS);

    final var result = new ShiftBy(Supplier.of(left), expandByFromStart, expandByFromEnd).evaluate(simResults, Map.of());

    final var expected = new Windows()
        .set(Interval.between(-1, Inclusive, 7, Inclusive, SECONDS), true)
        .set(Interval.between(7, Exclusive, 9, Exclusive, SECONDS), true)
        .set(Interval.between(9, Exclusive, 15, Exclusive, SECONDS), true)
        .set(Interval.between(19, 20, SECONDS), true)
        .set(at(22, SECONDS), false);

    assertIterableEquals(expected, result);
  }

  @Test
  public void testShrink() {
    final var simResults = new SimulationResults(
        Interval.between(0, 20, SECONDS),
        List.of(),
        Map.of(),
        Map.of()
    );

    final var left = new Windows()
        .set(Interval.between(0, Inclusive, 5, Exclusive, SECONDS), true)
        .set(Interval.between(6, Inclusive, 7, Inclusive, SECONDS), true)
        .set(Interval.between(8, Exclusive, 9, Exclusive, SECONDS), true)
        .set(Interval.between(10, Exclusive, 15, Exclusive, SECONDS), true)
        .set(Interval.at(20, SECONDS), true)
        .set(interval(22, 23, SECONDS), false);

    final var clampFromStart = Duration.of(1, SECONDS);
    final var clampFromEnd = Duration.negate(Duration.of(1, SECONDS));

    final var result = new ShiftBy(Supplier.of(left), clampFromStart, clampFromEnd).evaluate(simResults, Map.of());

    final var expected = new Windows()
        .set(Interval.between(1, Inclusive, 4, Exclusive, SECONDS), true)
        .set(Interval.between(7, Inclusive, 6, Inclusive, SECONDS), true)
        .set(Interval.between(9, Exclusive, 8, Exclusive, SECONDS), true)
        .set(Interval.between(11, Exclusive, 14, Exclusive, SECONDS), true)
        .set(interval(21, 24, SECONDS), false);

    assertIterableEquals(expected, result);
  }

  @Test
  public void testRealValue() {
    final var simResults = new SimulationResults(
        Interval.between(0, 20, SECONDS),
        List.of(),
        Map.of(),
        Map.of()
    );

    final var result = new RealValue(7).evaluate(simResults, Map.of());

    final var expected = new LinearProfile(
        new LinearProfilePiece(simResults.bounds, 7, 0));

    assertEquivalent(expected, result);
  }

  @Test
  public void testDiscreteValue() {
    final var simResults = new SimulationResults(
        Interval.between(0, 20, SECONDS),
        List.of(),
        Map.of(),
        Map.of()
    );

    final var result = new DiscreteValue(SerializedValue.of("IDLE")).evaluate(simResults, Map.of());

    final var expected = new DiscreteProfile(
        List.of(
            new DiscreteProfilePiece(simResults.bounds, SerializedValue.of("IDLE"))
        ));

    assertEquivalent(expected, result);
  }

  @Test
  public void testRealParameter() {
    final var act = new ActivityInstance(
        1,
        "typeA",
        Map.of("p1", SerializedValue.of(2)),
        Interval.between(0, 10, SECONDS));

    final var simResults = new SimulationResults(
        Interval.between(0, 20, SECONDS),
        List.of(act),
        Map.of(),
        Map.of()
    );
    final var environment = Map.of("act", act);

    final var result = new RealParameter("act", "p1").evaluate(simResults, environment);

    final var expected = new LinearProfile(
        new LinearProfilePiece(Interval.between(0, Inclusive, 20, Inclusive, SECONDS), 2, 0)
    );

    assertEquivalent(expected, result);
  }

  @Test
  public void testDiscreteResource() {
    final var simResults = new SimulationResults(
        Interval.between(0, 20, SECONDS),
        List.of(),
        Map.of(
            "real1", new LinearProfile(new LinearProfilePiece(Interval.at(1, SECONDS), 0, 1)),
            "real2", new LinearProfile(new LinearProfilePiece(Interval.at(2, SECONDS), 0, 1)),
            "real3", new LinearProfile(new LinearProfilePiece(Interval.at(3, SECONDS), 0, 1))
        ),
        Map.of(
            "discrete1", new DiscreteProfile(new DiscreteProfilePiece(Interval.at(4, SECONDS), SerializedValue.of("one"))),
            "discrete2", new DiscreteProfile(new DiscreteProfilePiece(Interval.at(5, SECONDS), SerializedValue.of("two"))),
            "discrete3", new DiscreteProfile(new DiscreteProfilePiece(Interval.at(6, SECONDS), SerializedValue.of("three")))
        )
    );

    final var result = new DiscreteResource("discrete2").evaluate(simResults, Map.of());

    final var expected = simResults.discreteProfiles.get("discrete2");

    assertEquivalent(expected, result);
  }

  @Test
  public void testRealResource() {
    final var simResults = new SimulationResults(
        Interval.between(0, 20, SECONDS),
        List.of(),
        Map.of(
            "real1", new LinearProfile(new LinearProfilePiece(Interval.at(1, SECONDS), 0, 1)),
            "real2", new LinearProfile(new LinearProfilePiece(Interval.at(2, SECONDS), 0, 1)),
            "real3", new LinearProfile(new LinearProfilePiece(Interval.at(3, SECONDS), 0, 1))
        ),
        Map.of(
            "discrete1", new DiscreteProfile(new DiscreteProfilePiece(Interval.at(4, SECONDS), SerializedValue.of("one"))),
            "discrete2", new DiscreteProfile(new DiscreteProfilePiece(Interval.at(5, SECONDS), SerializedValue.of(2))),
            "discrete3", new DiscreteProfile(new DiscreteProfilePiece(Interval.at(6, SECONDS), SerializedValue.of("three")))
        )
    );

    final var result = new RealResource("real2").evaluate(simResults, Map.of());

    final var expected = simResults.realProfiles.get("real2");

    assertEquivalent(expected, result);
  }

  @Test
  public void testRealResourceOnDiscrete() {
    final var simResults = new SimulationResults(
        Interval.between(0, 20, SECONDS),
        List.of(),
        Map.of(
            "real1", new LinearProfile(new LinearProfilePiece(Interval.at(1, SECONDS), 0, 1)),
            "real2", new LinearProfile(new LinearProfilePiece(Interval.at(2, SECONDS), 0, 1)),
            "real3", new LinearProfile(new LinearProfilePiece(Interval.at(3, SECONDS), 0, 1))
        ),
        Map.of(
            "discrete1", new DiscreteProfile(new DiscreteProfilePiece(Interval.at(4, SECONDS), SerializedValue.of("one"))),
            "discrete2", new DiscreteProfile(new DiscreteProfilePiece(Interval.at(5, SECONDS), SerializedValue.of(2))),
            "discrete3", new DiscreteProfile(new DiscreteProfilePiece(Interval.at(6, SECONDS), SerializedValue.of("three")))
        )
    );

    final var result = new RealResource("discrete2").evaluate(simResults, Map.of());

    final var expected = new LinearProfile(new LinearProfilePiece(Interval.at(5, SECONDS), 2, 0));

    assertEquivalent(expected, result);
  }

  @Test
  public void testRealResourceFailureOnDiscrete() {
    final var simResults = new SimulationResults(
        Interval.between(0, 20, SECONDS),
        List.of(),
        Map.of(
            "real1", new LinearProfile(new LinearProfilePiece(Interval.at(1, SECONDS), 0, 1)),
            "real2", new LinearProfile(new LinearProfilePiece(Interval.at(2, SECONDS), 0, 1)),
            "real3", new LinearProfile(new LinearProfilePiece(Interval.at(3, SECONDS), 0, 1))
        ),
        Map.of(
            "discrete1", new DiscreteProfile(new DiscreteProfilePiece(Interval.at(4, SECONDS), SerializedValue.of("one"))),
            "discrete2", new DiscreteProfile(new DiscreteProfilePiece(Interval.at(5, SECONDS), SerializedValue.of(2))),
            "discrete3", new DiscreteProfile(new DiscreteProfilePiece(Interval.at(6, SECONDS), SerializedValue.of("three")))
        )
    );

    try {
      new RealResource("discrete1").evaluate(simResults, Map.of());
    } catch (final InputMismatchException e) {
      return;
    }
    fail("Expected RealResource node to fail conversion of discrete resource to real resource");
  }

  @Test
  public void testRealResourceOnNonexistentResource() {
    final var simResults = new SimulationResults(
        Interval.between(0, 20, SECONDS),
        List.of(),
        Map.of(),
        Map.of()
    );

    try {
      new RealResource("does_not_exist").evaluate(simResults, Map.of());
    } catch (final InputMismatchException e) {
      return;
    }
    fail("Expected RealResource node to fail on non-existent resource");
  }

  @Test
  public void testForEachActivity() {
    final var simResults = new SimulationResults(
        Interval.between(0, 20, SECONDS),
        List.of(
            new ActivityInstance(1, "TypeA", Map.of(), Interval.between(4, 6, SECONDS)),
            new ActivityInstance(2, "TypeB", Map.of(), Interval.between(5, 7, SECONDS)),
            new ActivityInstance(3, "TypeA", Map.of(), Interval.between(9, 10, SECONDS))
        ),
        Map.of(),
        Map.of()
    );

    final var violation = new Violation(List.of(), List.of(), new Windows(interval(4, 6, SECONDS), true));
    final var result = new ForEachActivity(
        "TypeA",
        "act",
        new Supplier<>(List.of(violation))
    ).evaluate(simResults, Map.of());

    final var expected = List.of(
        new Violation(List.of(1L), List.of(), new Windows(interval(4, 6, SECONDS), true)),
        new Violation(List.of(3L), List.of(), new Windows(interval(4, 6, SECONDS), true)));

    assertIterableEquals(expected, result);
  }

  @Test
  public void testNestedForEachActivity() {
    final var simResults = new SimulationResults(
        Interval.between(0, 20, SECONDS),
        List.of(
            new ActivityInstance(1, "TypeA", Map.of(), Interval.between(4, 6, SECONDS)),
            new ActivityInstance(2, "TypeB", Map.of(), Interval.between(5, 7, SECONDS)),
            new  ActivityInstance(3, "TypeA", Map.of(), Interval.between(9, 10, SECONDS))
        ),
        Map.of(),
        Map.of()
    );

    final var violation = new Violation(List.of(), List.of(), new Windows(interval(4, 6, SECONDS), true));
    final var result = new ForEachActivity(
        "TypeA",
        "act",
        new ForEachActivity(
            "TypeB",
            "act",
            new Supplier<>(List.of(violation))
        )
    ).evaluate(simResults, Map.of());

    // We expect two violations because there are two activities of TypeA
    // The details of the violation will be the same, since we are using a supplier
    final var expected = List.of(
        new Violation(List.of(1L, 2L), List.of(), new Windows(interval(4, 6, SECONDS), true)),
        new Violation(List.of(3L, 2L), List.of(), new Windows(interval(4, 6, SECONDS), true)));

    assertEquivalent(expected, result);
  }

  @Test
  public void testViolationsOf() {
    final var simResults = new SimulationResults(
        Interval.between(0, 20, SECONDS),
        List.of(),
        Map.of(),
        Map.of()
    );

    final var windows = new Windows()
        .set(Interval.between(1, 4, SECONDS), true)
        .set(Interval.between(7,20, SECONDS), true);

    final var result = new ViolationsOf(new Supplier<>(windows)).evaluate(simResults, Map.of());

    final var expected = List.of(new Violation(windows.not().and(new Windows(simResults.bounds, true))));

    assertEquivalent(expected, result);
  }

  @Test
  public void testDuring() {
    final var simResults = new SimulationResults(
        Interval.between(0, 20, SECONDS),
        List.of(),
        Map.of(),
        Map.of()
    );

    final var environment = Map.of(
        "act",
        new ActivityInstance(
            1,
            "TypeA",
            Map.of(),
            Interval.between(4, 8, SECONDS))
    );

    final var result = new ActivityWindow("act").evaluate(simResults, environment);

    final var expected = new Windows(
        Segment.of(FOREVER, false),
        Segment.of(interval(4, 8, SECONDS), true)
    );

    assertEquivalent(expected, result);
  }

  @Test
  public void testStartOf() {
    final var simResults = new SimulationResults(
        Interval.between(0, 20, SECONDS),
        List.of(),
        Map.of(),
        Map.of()
    );

    final var environment = Map.of(
        "act",
        new ActivityInstance(
            1,
            "TypeA",
            Map.of(),
            Interval.between(4, 8, SECONDS))
    );

    final var result = new StartOf("act").evaluate(simResults, environment);

    final var expected = new Windows(
        Segment.of(FOREVER, false),
        Segment.of(at(4, SECONDS), true)
    );

    assertEquivalent(expected, result);
  }

  @Test
  public void testEndOf() {
    final var simResults = new SimulationResults(
        Interval.between(0, 20, SECONDS),
        List.of(),
        Map.of(),
        Map.of()
    );

    final var environment = Map.of(
        "act",
        new ActivityInstance(
            1,
            "TypeA",
            Map.of(),
            Interval.between(4, 8, SECONDS))
    );

    final var result = new EndOf("act").evaluate(simResults, environment);

    final var expected = new Windows(
        Segment.of(FOREVER, false),
        Segment.of(at(8, SECONDS), true)
    );

    assertEquivalent(expected, result);
  }

  @Test
  public void testLongerThan() {
    final var simResults = new SimulationResults(
        Interval.between(0, 20, SECONDS),
        List.of(),
        Map.of(),
        Map.of()
    );

    final var left = new Windows()
        .set(Interval.between(0, Inclusive, 5, Exclusive, SECONDS), true)
        .set(Interval.between(6, Inclusive, 7, Inclusive, SECONDS), true)
        .set(Interval.between(8, Exclusive, 9, Exclusive, SECONDS), true)
        .set(Interval.between(10, Exclusive, 15, Exclusive, SECONDS), true)
        .set(Interval.at(20, SECONDS), true)
        .set(interval(22, 23, SECONDS), false)
        .set(interval(24, 30, SECONDS), false);

    final var right = Duration.of(2, SECONDS);
    final var result = new LongerThan(Supplier.of(left), right).evaluate(simResults, Map.of());

    final var expected = new Windows()
        .set(Interval.between(0, Inclusive, 5, Exclusive, SECONDS), true)
        .set(Interval.between(6, Inclusive, 7, Inclusive, SECONDS), false)
        .set(Interval.between(8, Exclusive, 9, Exclusive, SECONDS), false)
        .set(Interval.between(10, Exclusive, 15, Exclusive, SECONDS), true)
        .set(Interval.at(20, SECONDS), false)
        .set(interval(22, 23, SECONDS), false)
        .set(interval(24, 30, SECONDS), false);

    assertEquivalent(expected, result);
  }

  @Test
  public void testShorterThan() {
    final var simResults = new SimulationResults(
        Interval.between(0, 20, SECONDS),
        List.of(),
        Map.of(),
        Map.of()
    );

    final var left = new Windows()
        .set(Interval.between(0, Inclusive, 5, Exclusive, SECONDS), true)
        .set(Interval.between(6, Inclusive, 7, Inclusive, SECONDS), true)
        .set(Interval.between(8, Exclusive, 9, Exclusive, SECONDS), true)
        .set(Interval.between(10, Exclusive, 15, Exclusive, SECONDS), true)
        .set(Interval.at(20, SECONDS), true)
        .set(interval(22, 23, SECONDS), false)
        .set(interval(24, 30, SECONDS), false);

    final var right = Duration.of(2, SECONDS);
    final var result = new ShorterThan(Supplier.of(left), right).evaluate(simResults, Map.of());

    final var expected = new Windows()
        .set(Interval.between(0, Inclusive, 5, Exclusive, SECONDS), false)
        .set(Interval.between(6, Inclusive, 7, Inclusive, SECONDS), true)
        .set(Interval.between(8, Exclusive, 9, Exclusive, SECONDS), true)
        .set(Interval.between(10, Exclusive, 15, Exclusive, SECONDS), false)
        .set(Interval.at(20, SECONDS), true)
        .set(interval(22, 23, SECONDS), false)
        .set(interval(24, 30, SECONDS), false);

    assertEquivalent(expected, result);
  }

  private static final class Supplier<T> implements Expression<T> {
    private final T value;

    public Supplier(final T value) {
      this.value = value;
    }


    @Override
    public T evaluate(final SimulationResults results, final Interval bounds, final Map<String, ActivityInstance> environment) {
      return this.value;
    }

    @Override
    public void extractResources(final Set<String> names) { }

    @Override
    public String prettyPrint(final String prefix) {
      return value.toString();
    }

    public static <V> Supplier<V> of(V value) {
      return new Supplier<>(value);
    }
  }
}
