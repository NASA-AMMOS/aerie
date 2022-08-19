package gov.nasa.jpl.aerie.constraints.time;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Inclusive;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

public class IntervalMapMap2OverlapTest {

  @Test
  public void map2overlapBothEnds() {
    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra());
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra());

    left.set(Interval.between(1, 3, SECONDS), "a");
    left.set(Interval.between(4, 7, SECONDS), "b");

    right.set(Interval.between(2, 5, SECONDS), "b");

    IntervalMap<String> mapped = IntervalMap.map2(left,
                                                  right,
                                                  (a$, b$) -> {
                                                    if (a$.isPresent() && b$.isPresent()) {
                                                      return Optional.of(a$.get() + b$.get());
                                                    }
                                                    else if (a$.isPresent() && !b$.isPresent()) {
                                                      return Optional.of(a$.get() + "N");
                                                    }
                                                    else if (b$.isPresent() && !a$.isPresent()) {
                                                      return Optional.of("N" + b$.get());
                                                    }
                                                    else {
                                                      return Optional.of("NN");
                                                    }
                                                  });

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra());
    expected.set(Interval.between(Duration.MIN_VALUE, Inclusive, Duration.of(1, SECONDS), Exclusive), "NN");
    expected.set(Interval.between(Duration.of(1, SECONDS), Inclusive, Duration.of(2, SECONDS), Exclusive), "aN");
    expected.set(Interval.between(Duration.of(2, SECONDS), Inclusive, Duration.of(3, SECONDS), Inclusive), "ab");
    expected.set(Interval.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "Nb");
    expected.set(Interval.between(Duration.of(4, SECONDS), Inclusive, Duration.of(5, SECONDS), Inclusive), "bb");
    expected.set(Interval.between(Duration.of(5, SECONDS), Exclusive, Duration.of(7, SECONDS), Inclusive), "bN");
    expected.set(Interval.between(Duration.of(7, SECONDS), Exclusive, Duration.MAX_VALUE, Inclusive), "NN");

    assertIterableEquals(expected, mapped);
  }

  @Test
  public void map2overlapTotal() {
    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra());
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra());

    left.set(Interval.between(1, 5, SECONDS), "a");

    right.set(Interval.between(2, 4, SECONDS), "b");

    IntervalMap<String> mapped = IntervalMap.map2(left,
                                                  right,
                                                  (a$, b$) -> {
                                                    if (a$.isPresent() && b$.isPresent()) {
                                                      return Optional.of(a$.get() + b$.get());
                                                    }
                                                    else if (a$.isPresent() && !b$.isPresent()) {
                                                      return Optional.of(a$.get() + "N");
                                                    }
                                                    else if (b$.isPresent() && !a$.isPresent()) {
                                                      return Optional.of("N" + b$.get());
                                                    }
                                                    else {
                                                      return Optional.of("NN");
                                                    }
                                                  });

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra());
    expected.set(Interval.between(Duration.MIN_VALUE, Inclusive, Duration.of(1, SECONDS), Exclusive), "NN");
    expected.set(Interval.between(Duration.of(1, SECONDS), Inclusive, Duration.of(2, SECONDS), Exclusive), "aN");
    expected.set(Interval.between(Duration.of(2, SECONDS), Inclusive, Duration.of(4, SECONDS), Inclusive), "ab");
    expected.set(Interval.between(Duration.of(4, SECONDS), Exclusive, Duration.of(5, SECONDS), Inclusive), "aN");
    expected.set(Interval.between(Duration.of(5, SECONDS), Exclusive, Duration.MAX_VALUE, Inclusive), "NN");

    assertIterableEquals(expected, mapped);
  }

  @Test
  public void map2overlapEndMeetIncl() {
    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra());
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra());

    left.set(Interval.between(1, 5, SECONDS), "a");

    right.set(Interval.between(2, 5, SECONDS), "b");

    IntervalMap<String> mapped = IntervalMap.map2(left,
                                                  right,
                                                  (a$, b$) -> {
                                                    if (a$.isPresent() && b$.isPresent()) {
                                                      return Optional.of(a$.get() + b$.get());
                                                    }
                                                    else if (a$.isPresent() && !b$.isPresent()) {
                                                      return Optional.of(a$.get() + "N");
                                                    }
                                                    else if (b$.isPresent() && !a$.isPresent()) {
                                                      return Optional.of("N" + b$.get());
                                                    }
                                                    else {
                                                      return Optional.of("NN");
                                                    }
                                                  });

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra());
    expected.set(Interval.between(Duration.MIN_VALUE, Inclusive, Duration.of(1, SECONDS), Exclusive), "NN");
    expected.set(Interval.between(Duration.of(1, SECONDS), Inclusive, Duration.of(2, SECONDS), Exclusive), "aN");
    expected.set(Interval.between(Duration.of(2, SECONDS), Inclusive, Duration.of(5, SECONDS), Inclusive), "ab");
    expected.set(Interval.between(Duration.of(5, SECONDS), Exclusive, Duration.MAX_VALUE, Inclusive), "NN");

    assertIterableEquals(expected, mapped);
  }

  @Test
  public void map2overlapEndMeetExcl() {
    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra());
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra());

    left.set(Interval.between(1, 5, SECONDS), "a");

    right.set(Interval.between(Duration.of(2, SECONDS), Inclusive, Duration.of(5, SECONDS), Exclusive), "b");

    IntervalMap<String> mapped = IntervalMap.map2(left,
                                                  right,
                                                  (a$, b$) -> {
                                                    if (a$.isPresent() && b$.isPresent()) {
                                                      return Optional.of(a$.get() + b$.get());
                                                    }
                                                    else if (a$.isPresent() && !b$.isPresent()) {
                                                      return Optional.of(a$.get() + "N");
                                                    }
                                                    else if (b$.isPresent() && !a$.isPresent()) {
                                                      return Optional.of("N" + b$.get());
                                                    }
                                                    else {
                                                      return Optional.of("NN");
                                                    }
                                                  });

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra());
    expected.set(Interval.between(Duration.MIN_VALUE, Inclusive, Duration.of(1, SECONDS), Exclusive), "NN");
    expected.set(Interval.between(Duration.of(1, SECONDS), Inclusive, Duration.of(2, SECONDS), Exclusive), "aN");
    expected.set(Interval.between(Duration.of(2, SECONDS), Inclusive, Duration.of(5, SECONDS), Exclusive), "ab");
    expected.set(Interval.at(Duration.of(5, SECONDS)), "aN");
    expected.set(Interval.between(Duration.of(5, SECONDS), Exclusive, Duration.MAX_VALUE, Inclusive), "NN");

    assertIterableEquals(expected, mapped);
  }

  @Test
  public void map2overlapEndMeetBothExcl() {
    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra());
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra());

    left.set(Interval.between(Duration.of(1, SECONDS), Inclusive, Duration.of(5, SECONDS), Exclusive), "a");

    right.set(Interval.between(Duration.of(2, SECONDS), Inclusive, Duration.of(5, SECONDS), Exclusive), "b");

    IntervalMap<String> mapped = IntervalMap.map2(left,
                                                  right,
                                                  (a$, b$) -> {
                                                    if (a$.isPresent() && b$.isPresent()) {
                                                      return Optional.of(a$.get() + b$.get());
                                                    }
                                                    else if (a$.isPresent() && !b$.isPresent()) {
                                                      return Optional.of(a$.get() + "N");
                                                    }
                                                    else if (b$.isPresent() && !a$.isPresent()) {
                                                      return Optional.of("N" + b$.get());
                                                    }
                                                    else {
                                                      return Optional.of("NN");
                                                    }
                                                  });

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra());
    expected.set(Interval.between(Duration.MIN_VALUE, Inclusive, Duration.of(1, SECONDS), Exclusive), "NN");
    expected.set(Interval.between(Duration.of(1, SECONDS), Inclusive, Duration.of(2, SECONDS), Exclusive), "aN");
    expected.set(Interval.between(Duration.of(2, SECONDS), Inclusive, Duration.of(5, SECONDS), Exclusive), "ab");
    expected.set(Interval.between(Duration.of(5, SECONDS), Inclusive, Duration.MAX_VALUE, Inclusive), "NN");

    assertIterableEquals(expected, mapped);
  }

  @Test
  public void map2overlapStartMeetIncl() {
    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra());
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra());

    left.set(Interval.between(1, 5, SECONDS), "a");

    right.set(Interval.between(1, 3, SECONDS), "b");

    IntervalMap<String> mapped = IntervalMap.map2(left,
                                                  right,
                                                  (a$, b$) -> {
                                                    if (a$.isPresent() && b$.isPresent()) {
                                                      return Optional.of(a$.get() + b$.get());
                                                    }
                                                    else if (a$.isPresent() && !b$.isPresent()) {
                                                      return Optional.of(a$.get() + "N");
                                                    }
                                                    else if (b$.isPresent() && !a$.isPresent()) {
                                                      return Optional.of("N" + b$.get());
                                                    }
                                                    else {
                                                      return Optional.of("NN");
                                                    }
                                                  });

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra());
    expected.set(Interval.between(Duration.MIN_VALUE, Inclusive, Duration.of(1, SECONDS), Exclusive), "NN");
    expected.set(Interval.between(Duration.of(1, SECONDS), Inclusive, Duration.of(3, SECONDS), Inclusive), "ab");
    expected.set(Interval.between(Duration.of(3, SECONDS), Exclusive, Duration.of(5, SECONDS), Inclusive), "aN");
    expected.set(Interval.between(Duration.of(5, SECONDS), Exclusive, Duration.MAX_VALUE, Inclusive), "NN");

    assertIterableEquals(expected, mapped);
  }

  @Test
  public void map2overlapStartMeetExcl() {
    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra());
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra());

    left.set(Interval.between(1, 5, SECONDS), "a");

    right.set(Interval.between(Duration.of(1, SECONDS), Exclusive, Duration.of(3, SECONDS), Exclusive), "b");

    IntervalMap<String> mapped = IntervalMap.map2(left,
                                                  right,
                                                  (a$, b$) -> {
                                                    if (a$.isPresent() && b$.isPresent()) {
                                                      return Optional.of(a$.get() + b$.get());
                                                    }
                                                    else if (a$.isPresent() && !b$.isPresent()) {
                                                      return Optional.of(a$.get() + "N");
                                                    }
                                                    else if (b$.isPresent() && !a$.isPresent()) {
                                                      return Optional.of("N" + b$.get());
                                                    }
                                                    else {
                                                      return Optional.of("NN");
                                                    }
                                                  });

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra());
    expected.set(Interval.between(Duration.MIN_VALUE, Inclusive, Duration.of(1, SECONDS), Exclusive), "NN");
    expected.set(Interval.at(Duration.of(1, SECONDS)), "aN");
    expected.set(Interval.between(Duration.of(1, SECONDS), Exclusive, Duration.of(3, SECONDS), Exclusive), "ab");
    expected.set(Interval.between(Duration.of(3, SECONDS), Inclusive, Duration.of(5, SECONDS), Inclusive), "aN");
    expected.set(Interval.between(Duration.of(5, SECONDS), Exclusive, Duration.MAX_VALUE, Inclusive), "NN");

    assertIterableEquals(expected, mapped);
  }

  @Test
  public void map2overlapStartMeetBothExcl() {
    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra());
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra());

    left.set(Interval.between(Duration.of(1, SECONDS), Exclusive, Duration.of( 5, SECONDS), Inclusive), "a");

    right.set(Interval.between(Duration.of(1, SECONDS), Exclusive, Duration.of(3, SECONDS), Exclusive), "b");

    IntervalMap<String> mapped = IntervalMap.map2(left,
                                                  right,
                                                  (a$, b$) -> {
                                                    if (a$.isPresent() && b$.isPresent()) {
                                                      return Optional.of(a$.get() + b$.get());
                                                    }
                                                    else if (a$.isPresent() && !b$.isPresent()) {
                                                      return Optional.of(a$.get() + "N");
                                                    }
                                                    else if (b$.isPresent() && !a$.isPresent()) {
                                                      return Optional.of("N" + b$.get());
                                                    }
                                                    else {
                                                      return Optional.of("NN");
                                                    }
                                                  });

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra());
    expected.set(Interval.between(Duration.MIN_VALUE, Inclusive, Duration.of(1, SECONDS), Inclusive), "NN");
    expected.set(Interval.between(Duration.of(1, SECONDS), Exclusive, Duration.of(3, SECONDS), Exclusive), "ab");
    expected.set(Interval.between(Duration.of(3, SECONDS), Inclusive, Duration.of(5, SECONDS), Inclusive), "aN");
    expected.set(Interval.between(Duration.of(5, SECONDS), Exclusive, Duration.MAX_VALUE, Inclusive), "NN");

    assertIterableEquals(expected, mapped);
  }

  @Test
  public void map2overlapBothOffsetForward() {
    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra());
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra());

    left.set(Interval.between(3, 5, SECONDS), "a");

    right.set(Interval.between(1, 4, SECONDS), "b");

    IntervalMap<String> mapped = IntervalMap.map2(left,
                                                  right,
                                                  (a$, b$) -> {
                                                    if (a$.isPresent() && b$.isPresent()) {
                                                      return Optional.of(a$.get() + b$.get());
                                                    }
                                                    else if (a$.isPresent() && !b$.isPresent()) {
                                                      return Optional.of(a$.get() + "N");
                                                    }
                                                    else if (b$.isPresent() && !a$.isPresent()) {
                                                      return Optional.of("N" + b$.get());
                                                    }
                                                    else {
                                                      return Optional.of("NN");
                                                    }
                                                  });

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra());
    expected.set(Interval.between(Duration.MIN_VALUE, Inclusive, Duration.of(1, SECONDS), Exclusive), "NN");
    expected.set(Interval.between(Duration.of(1, SECONDS), Inclusive, Duration.of(3, SECONDS), Exclusive), "Nb");
    expected.set(Interval.between(Duration.of(3, SECONDS), Inclusive, Duration.of(4, SECONDS), Inclusive), "ab");
    expected.set(Interval.between(Duration.of(4, SECONDS), Exclusive, Duration.of(5, SECONDS), Inclusive), "aN");
    expected.set(Interval.between(Duration.of(5, SECONDS), Exclusive, Duration.MAX_VALUE, Inclusive), "NN");

    assertIterableEquals(expected, mapped);
  }

  @Test
  public void map2overlapBothEndsMeetIncl() {
    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra());
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra());

    left.set(Interval.between(2, 3, SECONDS), "a");
    left.set(Interval.between(5, 6, SECONDS), "b");

    right.set(Interval.between(2, 6, SECONDS), "b");

    IntervalMap<String> mapped = IntervalMap.map2(left,
                                                  right,
                                                  (a$, b$) -> {
                                                    if (a$.isPresent() && b$.isPresent()) {
                                                      return Optional.of(a$.get() + b$.get());
                                                    }
                                                    else if (a$.isPresent() && !b$.isPresent()) {
                                                      return Optional.of(a$.get() + "N");
                                                    }
                                                    else if (b$.isPresent() && !a$.isPresent()) {
                                                      return Optional.of("N" + b$.get());
                                                    }
                                                    else {
                                                      return Optional.of("NN");
                                                    }
                                                  });

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra());
    expected.set(Interval.between(Duration.MIN_VALUE, Inclusive, Duration.of(2, SECONDS), Exclusive), "NN");
    expected.set(Interval.between(Duration.of(2, SECONDS), Inclusive, Duration.of(3, SECONDS), Inclusive), "ab");
    expected.set(Interval.between(Duration.of(3, SECONDS), Exclusive, Duration.of(5, SECONDS), Exclusive), "Nb");
    expected.set(Interval.between(Duration.of(5, SECONDS), Inclusive, Duration.of(6, SECONDS), Inclusive), "bb");
    expected.set(Interval.between(Duration.of(6, SECONDS), Exclusive, Duration.MAX_VALUE, Inclusive), "NN");

    assertIterableEquals(expected, mapped);
  }

  @Test
  public void map2overlapBothEndsMeetExcl() {
    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra());
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra());

    left.set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(3, SECONDS), Exclusive), "a");
    left.set(Interval.between(Duration.of(5, SECONDS), Exclusive, Duration.of(6, SECONDS), Exclusive), "b");

    right.set(Interval.between(2, 6, SECONDS), "b");

    IntervalMap<String> mapped = IntervalMap.map2(left,
                                                  right,
                                                  (a$, b$) -> {
                                                    if (a$.isPresent() && b$.isPresent()) {
                                                      return Optional.of(a$.get() + b$.get());
                                                    }
                                                    else if (a$.isPresent() && !b$.isPresent()) {
                                                      return Optional.of(a$.get() + "N");
                                                    }
                                                    else if (b$.isPresent() && !a$.isPresent()) {
                                                      return Optional.of("N" + b$.get());
                                                    }
                                                    else {
                                                      return Optional.of("NN");
                                                    }
                                                  });

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra());
    expected.set(Interval.between(Duration.MIN_VALUE, Inclusive, Duration.of(2, SECONDS), Exclusive), "NN");
    expected.set(Interval.at(Duration.of(2, SECONDS)), "Nb");
    expected.set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(3, SECONDS), Exclusive), "ab");
    expected.set(Interval.between(Duration.of(3, SECONDS), Inclusive, Duration.of(5, SECONDS), Inclusive), "Nb");
    expected.set(Interval.between(Duration.of(5, SECONDS), Exclusive, Duration.of(6, SECONDS), Exclusive), "bb");
    expected.set(Interval.at(Duration.of(6, SECONDS)), "Nb");
    expected.set(Interval.between(Duration.of(6, SECONDS), Exclusive, Duration.MAX_VALUE, Inclusive), "NN");

    assertIterableEquals(expected, mapped);
  }

  @Test
  public void map2overlapBothEndsMeetBothExcl() {
    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra());
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra());

    left.set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(3, SECONDS), Exclusive), "a");
    left.set(Interval.between(Duration.of(5, SECONDS), Exclusive, Duration.of(6, SECONDS), Exclusive), "b");

    right.set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(6, SECONDS), Exclusive), "b");

    IntervalMap<String> mapped = IntervalMap.map2(left,
                                                  right,
                                                  (a$, b$) -> {
                                                    if (a$.isPresent() && b$.isPresent()) {
                                                      return Optional.of(a$.get() + b$.get());
                                                    }
                                                    else if (a$.isPresent() && !b$.isPresent()) {
                                                      return Optional.of(a$.get() + "N");
                                                    }
                                                    else if (b$.isPresent() && !a$.isPresent()) {
                                                      return Optional.of("N" + b$.get());
                                                    }
                                                    else {
                                                      return Optional.of("NN");
                                                    }
                                                  });

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra());
    expected.set(Interval.between(Duration.MIN_VALUE, Inclusive, Duration.of(2, SECONDS), Inclusive), "NN");
    expected.set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(3, SECONDS), Exclusive), "ab");
    expected.set(Interval.between(Duration.of(3, SECONDS), Inclusive, Duration.of(5, SECONDS), Inclusive), "Nb");
    expected.set(Interval.between(Duration.of(5, SECONDS), Exclusive, Duration.of(6, SECONDS), Exclusive), "bb");
    expected.set(Interval.between(Duration.of(6, SECONDS), Inclusive, Duration.MAX_VALUE, Inclusive), "NN");

    assertIterableEquals(expected, mapped);
  }

  @Test
  public void map2overlapAdjacencies() {
    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra());
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra());

    left.set(Interval.between(Duration.of(0, SECONDS), Exclusive, Duration.of(8, SECONDS), Exclusive), "a");
    left.set(Interval.between(Duration.of(9, SECONDS), Inclusive, Duration.of(13, SECONDS), Exclusive), "b");
    left.set(Interval.between(Duration.of(16, SECONDS), Inclusive, Duration.of(18, SECONDS), Exclusive), "a");

    right.set(Interval.between(Duration.of(1, SECONDS), Exclusive, Duration.of(3, SECONDS), Exclusive), "b");
    right.set(Interval.between(Duration.of(3, SECONDS), Inclusive, Duration.of(4, SECONDS), Exclusive), "b"); //should coalesce
    right.set(Interval.between(Duration.of(4, SECONDS), Exclusive, Duration.of(5, SECONDS), Exclusive), "b"); //shouldn't coalesce, should have null gap between
    right.set(Interval.between(Duration.of(5, SECONDS), Inclusive, Duration.of(6, SECONDS), Exclusive), "a");
    right.set(Interval.between(Duration.of(7, SECONDS), Inclusive, Duration.of(12, SECONDS), Exclusive), "a");
    right.set(Interval.between(Duration.of(12, SECONDS), Inclusive, Duration.of(13, SECONDS), Exclusive), "a");//coalesce
    right.set(Interval.between(Duration.of(13, SECONDS), Inclusive, Duration.of(14, SECONDS), Exclusive), "b");
    right.set(Interval.between(Duration.of(16, SECONDS), Inclusive, Duration.of(20, SECONDS), Exclusive), "b");

    IntervalMap<String> mapped = IntervalMap.map2(left,
                                                  right,
                                                  (a$, b$) -> {
                                                    if (a$.isPresent() && b$.isPresent()) {
                                                      return Optional.of(a$.get() + b$.get());
                                                    }
                                                    else if (a$.isPresent() && !b$.isPresent()) {
                                                      return Optional.of(a$.get() + "N");
                                                    }
                                                    else if (b$.isPresent() && !a$.isPresent()) {
                                                      return Optional.of("N" + b$.get());
                                                    }
                                                    else {
                                                      return Optional.of("NN");
                                                    }
                                                  });

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra());
    expected.set(Interval.between(Duration.MIN_VALUE, Inclusive, Duration.of(0, SECONDS), Inclusive), "NN");
    expected.set(Interval.between(Duration.of(0, SECONDS), Exclusive, Duration.of(1, SECONDS), Inclusive), "aN");
    expected.set(Interval.between(Duration.of(1, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "ab");
    expected.set(Interval.at(Duration.of(4, SECONDS)), "aN");
    expected.set(Interval.between(Duration.of(4, SECONDS), Exclusive, Duration.of(5, SECONDS), Exclusive), "ab");
    expected.set(Interval.between(Duration.of(5, SECONDS), Inclusive, Duration.of(6, SECONDS), Exclusive), "aa");
    expected.set(Interval.between(Duration.of(6, SECONDS), Inclusive, Duration.of(7, SECONDS), Exclusive), "aN");
    expected.set(Interval.between(Duration.of(7, SECONDS), Inclusive, Duration.of(8, SECONDS), Exclusive), "aa");
    expected.set(Interval.between(Duration.of(8, SECONDS), Inclusive, Duration.of(9, SECONDS), Exclusive), "Na");
    expected.set(Interval.between(Duration.of(9, SECONDS), Inclusive, Duration.of(13, SECONDS), Exclusive), "ba");
    expected.set(Interval.between(Duration.of(13, SECONDS), Inclusive, Duration.of(14, SECONDS), Exclusive), "Nb");
    expected.set(Interval.between(Duration.of(14, SECONDS), Inclusive, Duration.of(16, SECONDS), Exclusive), "NN");
    expected.set(Interval.between(Duration.of(16, SECONDS), Inclusive, Duration.of(18, SECONDS), Exclusive), "ab");
    expected.set(Interval.between(Duration.of(18, SECONDS), Inclusive, Duration.of(20, SECONDS), Exclusive), "Nb");
    expected.set(Interval.between(Duration.of(20, SECONDS), Inclusive, Duration.MAX_VALUE, Inclusive), "NN");

    assertIterableEquals(expected, mapped);
  }

  @Test
  public void map2overlapAtsIncl() {
    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra());
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra());

    left.set(Interval.between(Duration.of(2, SECONDS), Inclusive, Duration.of(10, SECONDS), Inclusive), "a");

    right.set(Interval.at(Duration.of(2, SECONDS)), "b");
    right.set(Interval.at(Duration.of(5, SECONDS)), "b");
    right.set(Interval.at(Duration.of(10, SECONDS)), "b");

    IntervalMap<String> mapped = IntervalMap.map2(left,
                                                  right,
                                                  (a$, b$) -> {
                                                    if (a$.isPresent() && b$.isPresent()) {
                                                      return Optional.of(a$.get() + b$.get());
                                                    }
                                                    else if (a$.isPresent() && !b$.isPresent()) {
                                                      return Optional.of(a$.get() + "N");
                                                    }
                                                    else if (b$.isPresent() && !a$.isPresent()) {
                                                      return Optional.of("N" + b$.get());
                                                    }
                                                    else {
                                                      return Optional.of("NN");
                                                    }
                                                  });

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra());
    expected.set(Interval.between(Duration.MIN_VALUE, Inclusive, Duration.of(2, SECONDS), Exclusive), "NN");
    expected.set(Interval.at(Duration.of(2, SECONDS)), "ab");
    expected.set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(5, SECONDS), Exclusive), "aN");
    expected.set(Interval.at(Duration.of(5, SECONDS)), "ab");
    expected.set(Interval.between(Duration.of(5, SECONDS), Exclusive, Duration.of(10, SECONDS), Exclusive), "aN");
    expected.set(Interval.at(Duration.of(10, SECONDS)), "ab");
    expected.set(Interval.between(Duration.of(10, SECONDS), Exclusive, Duration.MAX_VALUE, Inclusive), "NN");

    assertIterableEquals(expected, mapped);
  }

  @Test
  public void map2overlapAtsExcl() {
    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra());
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra());

    left.set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(10, SECONDS), Exclusive), "a");

    right.set(Interval.at(Duration.of(2, SECONDS)), "b");
    right.set(Interval.at(Duration.of(5, SECONDS)), "b");
    right.set(Interval.at(Duration.of(10, SECONDS)), "b");

    IntervalMap<String> mapped = IntervalMap.map2(left,
                                                  right,
                                                  (a$, b$) -> {
                                                    if (a$.isPresent() && b$.isPresent()) {
                                                      return Optional.of(a$.get() + b$.get());
                                                    }
                                                    else if (a$.isPresent() && !b$.isPresent()) {
                                                      return Optional.of(a$.get() + "N");
                                                    }
                                                    else if (b$.isPresent() && !a$.isPresent()) {
                                                      return Optional.of("N" + b$.get());
                                                    }
                                                    else {
                                                      return Optional.of("NN");
                                                    }
                                                  });

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra());
    expected.set(Interval.between(Duration.MIN_VALUE, Inclusive, Duration.of(2, SECONDS), Exclusive), "NN");
    expected.set(Interval.at(Duration.of(2, SECONDS)), "Nb");
    expected.set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(5, SECONDS), Exclusive), "aN");
    expected.set(Interval.at(Duration.of(5, SECONDS)), "ab");
    expected.set(Interval.between(Duration.of(5, SECONDS), Exclusive, Duration.of(10, SECONDS), Exclusive), "aN");
    expected.set(Interval.at(Duration.of(10, SECONDS)), "Nb");
    expected.set(Interval.between(Duration.of(10, SECONDS), Exclusive, Duration.MAX_VALUE, Inclusive), "NN");

    assertIterableEquals(expected, mapped);
  }
}
