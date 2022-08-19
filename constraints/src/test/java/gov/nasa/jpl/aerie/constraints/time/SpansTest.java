package gov.nasa.jpl.aerie.constraints.time;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static gov.nasa.jpl.aerie.constraints.Assertions.assertEquivalent;
import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Inclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.at;
import static gov.nasa.jpl.aerie.constraints.time.Interval.interval;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MILLISECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

public class SpansTest {
  @Test
  public void addEmpty() {
    final var spans = new Spans();
    spans.add(Interval.EMPTY);

    final var expected = new Spans();

    assertEquivalent(spans, expected);
  }

  @Test
  public void addOpenPoint() {
    final var spans = new Spans();
    spans.add(interval(1, Exclusive, 1, Exclusive, MICROSECONDS));

    final var expected = new Spans();

    assertEquivalent(spans, expected);
  }

  @Test
  public void doNotCoalesceAdjacent() {
    final var spans = new Spans();
    spans.add(interval(0, Inclusive, 1, Exclusive, MICROSECONDS));
    spans.add(interval(1, Inclusive, 2, Inclusive, MICROSECONDS));

    final var expected = List.of(
        interval(0, Inclusive, 1, Exclusive, MICROSECONDS),
        interval(1, Inclusive, 2, Inclusive, MICROSECONDS)
    );

    assertIterableEquals(expected, spans);
  }

  @Test
  public void doNotCoalesceOverlap() {
    final var spans = new Spans(
        interval(0, Inclusive, 1, Inclusive, SECONDS),
        interval(500, Inclusive, 2000, Inclusive, MILLISECONDS)
    );

    final var expected = List.of(
        interval(0, Inclusive, 1, Inclusive, SECONDS),
        interval(500, Inclusive, 2000, Inclusive, MILLISECONDS)
    );

    assertIterableEquals(expected, spans);
  }

  @Test
  public void filter() {
    final var spans = new Spans(
        interval(0, 1, SECONDS),
        interval(0, 2, SECONDS),
        interval(0, 3, SECONDS)
    ).filter($ -> $.duration().shorterThan(Duration.of(2, SECONDS)) || $.duration().longerThan(Duration.of(2, SECONDS)));

    final var expected = List.of(
        interval(0, 1, SECONDS),
        interval(0, 3, SECONDS)
    );

    assertIterableEquals(expected, spans);
  }

  @Test
  public void map() {
    final var spans = new Spans(
        interval(0, 2, SECONDS),
        interval(0, 3, SECONDS)
    ).map($ -> interval($.start, $.start.plus(SECOND)));

    final var expected = List.of(
        interval(0, 1, SECOND),
        interval(0, 1, SECOND)
    );

    assertIterableEquals(expected, spans);
  }

  @Test
  public void mapFiltersEmpty() {
    final var spans = new Spans(
        interval(0, Inclusive, 1, Exclusive, SECONDS),
        interval(0, 3, SECONDS)
    ).map($ -> interval($.start, $.startInclusivity, $.end.minus(SECOND), $.endInclusivity));

    final var expected = List.of(
        interval(0, 2, SECOND)
    );

    assertIterableEquals(expected, spans);
  }

  @Test
  public void flatMap() {
    final var spans = new Spans(
        interval(0, 1, SECONDS),
        interval(0, 3, SECONDS)
    ).flatMap($ -> {
      if ($.duration().noLongerThan(SECOND)) {
        return Stream.of();
      } else {
        return Stream.of(
            interval($.end, $.end),
            interval($.end, $.end.plus(SECOND))
        );
      }
    });

    final var expected = List.of(
        interval(3, 3, SECONDS),
        interval(3, 4, SECONDS)
    );

    assertIterableEquals(expected, spans);
  }

  @Test
  public void flatMapFiltersEmpty() {
    final var spans = new Spans(
        interval(0, Inclusive, 1, Exclusive, SECONDS),
        interval(0, 3, SECONDS)
    ).flatMap($ -> Stream.of(interval($.start, $.startInclusivity, $.end.minus(SECOND), $.endInclusivity)));

    final var expected = List.of(
        interval(0, 2, SECOND)
    );

    assertIterableEquals(expected, spans);
  }

  @Test
  public void intoWindows() {
    final var intervals = new Spans(
        interval(0, 2, SECONDS),
        interval(1, 3, SECONDS),
        interval(5, 5, SECONDS)
    ).intoWindows();

    final var expected = List.of(
        Pair.of(interval(Duration.MIN_VALUE, Inclusive, Duration.ZERO, Exclusive), false),
        Pair.of(interval(0, 3, SECONDS), true),
        Pair.of(interval(3, Exclusive, 5, Exclusive, SECONDS), false),
        Pair.of(at(5, SECONDS), true),
        Pair.of(interval(Duration.of(5, SECONDS), Exclusive, Duration.MAX_VALUE, Inclusive), false)
    );

    assertIterableEquals(expected, intervals);
  }
}
