package gov.nasa.jpl.aerie.constraints.time;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static gov.nasa.jpl.aerie.constraints.Assertions.assertEquivalent;
import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Inclusive;
import static gov.nasa.jpl.aerie.constraints.time.Window.window;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MILLISECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

public class SpansTest {
  @Test
  public void addEmpty() {
    final var spans = new Spans();
    spans.add(Window.EMPTY);

    final var expected = new Spans();

    assertEquivalent(spans, expected);
  }

  @Test
  public void addOpenPoint() {
    final var spans = new Spans();
    spans.add(window(1, Exclusive, 1, Exclusive, MICROSECONDS));

    final var expected = new Spans();

    assertEquivalent(spans, expected);
  }

  @Test
  public void doNotCoalesceAdjacent() {
    final var spans = new Spans();
    spans.add(window(0, Inclusive, 1, Exclusive, MICROSECONDS));
    spans.add(window(1, Inclusive, 2, Inclusive, MICROSECONDS));

    final var expected = List.of(
        window(0, Inclusive, 1, Exclusive, MICROSECONDS),
        window(1, Inclusive, 2, Inclusive, MICROSECONDS)
    );

    assertIterableEquals(expected, spans);
  }

  @Test
  public void doNotCoalesceOverlap() {
    final var spans = new Spans(
        window(0, Inclusive, 1, Inclusive, SECONDS),
        window(500, Inclusive, 2000, Inclusive, MILLISECONDS)
    );

    final var expected = List.of(
        window(0, Inclusive, 1, Inclusive, SECONDS),
        window(500, Inclusive, 2000, Inclusive, MILLISECONDS)
    );

    assertIterableEquals(expected, spans);
  }

  @Test
  public void filter() {
    final var spans = new Spans(
        window(0, 1, SECONDS),
        window(0, 2, SECONDS),
        window(0, 3, SECONDS)
    ).filter($ -> $.duration().shorterThan(Duration.of(2, SECONDS)) || $.duration().longerThan(Duration.of(2, SECONDS)));

    final var expected = List.of(
        window(0, 1, SECONDS),
        window(0, 3, SECONDS)
    );

    assertIterableEquals(expected, spans);
  }

  @Test
  public void map() {
    final var spans = new Spans(
        window(0, 2, SECONDS),
        window(0, 3, SECONDS)
    ).map($ -> window($.start, $.start.plus(SECOND)));

    final var expected = List.of(
        window(0, 1, SECOND),
        window(0, 1, SECOND)
    );

    assertIterableEquals(expected, spans);
  }

  @Test
  public void mapFiltersEmpty() {
    final var spans = new Spans(
        window(0, Inclusive, 1, Exclusive, SECONDS),
        window(0, 3, SECONDS)
    ).map($ -> window($.start, $.startInclusivity, $.end.minus(SECOND), $.endInclusivity));

    final var expected = List.of(
        window(0, 2, SECOND)
    );

    assertIterableEquals(expected, spans);
  }

  @Test
  public void flatMap() {
    final var spans = new Spans(
        window(0, 1, SECONDS),
        window(0, 3, SECONDS)
    ).flatMap($ -> {
      if ($.duration().noLongerThan(SECOND)) {
        return Stream.of();
      } else {
        return Stream.of(
            window($.end, $.end),
            window($.end, $.end.plus(SECOND))
        );
      }
    });

    final var expected = List.of(
        window(3, 3, SECONDS),
        window(3, 4, SECONDS)
    );

    assertIterableEquals(expected, spans);
  }

  @Test
  public void flatMapFiltersEmpty() {
    final var spans = new Spans(
        window(0, Inclusive, 1, Exclusive, SECONDS),
        window(0, 3, SECONDS)
    ).flatMap($ -> Stream.of(window($.start, $.startInclusivity, $.end.minus(SECOND), $.endInclusivity)));

    final var expected = List.of(
        window(0, 2, SECOND)
    );

    assertIterableEquals(expected, spans);
  }

  @Test
  public void intoWindows() {
    final var windows = new Spans(
        window(0, 2, SECONDS),
        window(1, 3, SECONDS),
        window(5, 5, SECONDS)
    ).intoWindows();

    final var expected = List.of(
        window(0, 3, SECONDS),
        window(5, 5, SECONDS)
    );

    assertIterableEquals(expected, windows);
  }
}
