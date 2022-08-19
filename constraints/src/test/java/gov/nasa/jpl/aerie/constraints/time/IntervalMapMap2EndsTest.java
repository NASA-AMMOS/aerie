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

public class IntervalMapMap2EndsTest {

  @Test
  public void map2nonoverlap() {
    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra());
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra());



    left.set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "a");
    left.set(Interval.between(Duration.of(8, SECONDS), Exclusive, Duration.of(9, SECONDS), Exclusive), "a");


    right.set(Interval.between(Duration.of(6, SECONDS), Exclusive, Duration.of(7, SECONDS), Exclusive), "b");

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
    expected.set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "aN");
    expected.set(Interval.between(Duration.of(4, SECONDS), Inclusive, Duration.of(6, SECONDS), Inclusive), "NN");
    expected.set(Interval.between(Duration.of(6, SECONDS), Exclusive, Duration.of(7, SECONDS), Exclusive), "Nb");
    expected.set(Interval.between(Duration.of(7, SECONDS), Inclusive, Duration.of(8, SECONDS), Inclusive), "NN");
    expected.set(Interval.between(Duration.of(8, SECONDS), Exclusive, Duration.of(9, SECONDS), Exclusive), "aN");
    expected.set(Interval.between(Duration.of(9, SECONDS), Inclusive, Duration.MAX_VALUE, Inclusive), "NN");

    assertIterableEquals(expected, mapped);
  }

  @Test
  public void map2bothStartAt() {

    Interval horizon = Interval.between(0, Inclusive, 4, Inclusive, SECONDS);
    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra(horizon));
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra(horizon));

    left.set(Interval.at(Duration.of(0, SECONDS)), "a");
    left.set(Interval.between(1, 3, SECONDS), "b");

    right.set(Interval.at(Duration.of(0, SECONDS)), "b");
    right.set(Interval.between(1, 2, SECONDS), "a");


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
    expected.set(Interval.at(Duration.of(0, SECONDS)), "ab");
    expected.set(Interval.between(Duration.of(0, SECONDS), Exclusive, Duration.of(1, SECONDS), Exclusive), "NN");
    expected.set(Interval.between(Duration.of(1, SECONDS), Inclusive, Duration.of(2, SECONDS), Inclusive), "ba");
    expected.set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(3, SECONDS), Inclusive), "bN");
    expected.set(Interval.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Inclusive), "NN");

    assertIterableEquals(expected, mapped);
  }

  @Test
  public void map2bothEndAt() {

    Interval horizon = Interval.between(0, Inclusive, 4, Inclusive, SECONDS);
    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra(horizon));
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra(horizon));

    left.set(Interval.between(1, 3, SECONDS), "b");
    left.set(Interval.at(Duration.of(4, SECONDS)), "a");

    right.set(Interval.between(1, 2, SECONDS), "a");
    right.set(Interval.at(Duration.of(4, SECONDS)), "b");


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
    expected.set(Interval.between(Duration.of(0, SECONDS), Inclusive, Duration.of(1, SECONDS), Exclusive), "NN");
    expected.set(Interval.between(Duration.of(1, SECONDS), Inclusive, Duration.of(2, SECONDS), Inclusive), "ba");
    expected.set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(3, SECONDS), Inclusive), "bN");
    expected.set(Interval.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Inclusive), "NN");
    expected.set(Interval.at(Duration.of(4, SECONDS)), "ab");

    assertIterableEquals(expected, mapped);
  }

  @Test
  public void map2oneStartAtOneEndAt() {

    Interval horizon = Interval.between(0, Inclusive, 4, Inclusive, SECONDS);
    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra(horizon));
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra(horizon));

    left.set(Interval.between(1, 3, SECONDS), "b");
    left.set(Interval.at(Duration.of(4, SECONDS)), "a");

    right.set(Interval.between(1, 2, SECONDS), "a");
    right.set(Interval.at(Duration.of(0, SECONDS)), "b");


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
    expected.set(Interval.at(Duration.of(0, SECONDS)), "Nb");
    expected.set(Interval.between(Duration.of(0, SECONDS), Exclusive, Duration.of(1, SECONDS), Exclusive), "NN");
    expected.set(Interval.between(Duration.of(1, SECONDS), Inclusive, Duration.of(2, SECONDS), Inclusive), "ba");
    expected.set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(3, SECONDS), Inclusive), "bN");
    expected.set(Interval.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "NN");
    expected.set(Interval.at(Duration.of(4, SECONDS)), "aN");

    assertIterableEquals(expected, mapped);
  }

  @Test
  public void map2bothStartInt() {

    Interval horizon = Interval.between(0, Inclusive, 4, Inclusive, SECONDS);
    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra(horizon));
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra(horizon));

    left.set(Interval.between(0, 3, SECONDS), "b");

    right.set(Interval.between(0, 2, SECONDS), "a");


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
    expected.set(Interval.between(Duration.of(0, SECONDS), Inclusive, Duration.of(2, SECONDS), Inclusive), "ba");
    expected.set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(3, SECONDS), Inclusive), "bN");
    expected.set(Interval.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Inclusive), "NN");

    assertIterableEquals(expected, mapped);
  }

  @Test
  public void map2bothEndInt() {

    Interval horizon = Interval.between(0, Inclusive, 4, Inclusive, SECONDS);
    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra(horizon));
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra(horizon));

    left.set(Interval.between(2, 4, SECONDS), "b");

    right.set(Interval.between(1, 4, SECONDS), "a");


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
    expected.set(Interval.between(Duration.of(0, SECONDS), Inclusive, Duration.of(1, SECONDS), Exclusive), "NN");
    expected.set(Interval.between(Duration.of(1, SECONDS), Inclusive, Duration.of(2, SECONDS), Exclusive), "Na");
    expected.set(Interval.between(Duration.of(2, SECONDS), Inclusive, Duration.of(4, SECONDS), Inclusive), "ba");

    assertIterableEquals(expected, mapped);
  }

  @Test
  public void map2oneStartIntOneEndInt() {

    Interval horizon = Interval.between(0, Inclusive, 4, Inclusive, SECONDS);
    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra(horizon));
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra(horizon));

    left.set(Interval.between(0, 3, SECONDS), "b");

    right.set(Interval.between(2, 4, SECONDS), "a");


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
    expected.set(Interval.between(Duration.of(0, SECONDS), Inclusive, Duration.of(2, SECONDS), Exclusive), "bN");
    expected.set(Interval.between(Duration.of(2, SECONDS), Inclusive, Duration.of(3, SECONDS), Inclusive), "ba");
    expected.set(Interval.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Inclusive), "Na");

    assertIterableEquals(expected, mapped);
  }

  @Test
  public void map2oneEndIntOneNullEnd() {

    Interval horizon = Interval.between(0, Inclusive, 4, Inclusive, SECONDS);
    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra(horizon));
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra(horizon));

    left.set(Interval.between(2, 4, SECONDS), "b");

    right.set(Interval.between(1, 3, SECONDS), "a");


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
    expected.set(Interval.between(Duration.of(0, SECONDS), Inclusive, Duration.of(1, SECONDS), Exclusive), "NN");
    expected.set(Interval.between(Duration.of(1, SECONDS), Inclusive, Duration.of(2, SECONDS), Exclusive), "Na");
    expected.set(Interval.between(Duration.of(2, SECONDS), Inclusive, Duration.of(3, SECONDS), Inclusive), "ba");
    expected.set(Interval.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Inclusive), "bN");

    assertIterableEquals(expected, mapped);
  }

  @Test
  public void map2oneStartIntOneNullStart() {

    Interval horizon = Interval.between(0, Inclusive, 4, Inclusive, SECONDS);
    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra(horizon));
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra(horizon));

    left.set(Interval.between(0, 2, SECONDS), "b");

    right.set(Interval.between(1, 3, SECONDS), "a");


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
    expected.set(Interval.between(Duration.of(0, SECONDS), Inclusive, Duration.of(1, SECONDS), Exclusive), "bN");
    expected.set(Interval.between(Duration.of(1, SECONDS), Inclusive, Duration.of(2, SECONDS), Inclusive), "ba");
    expected.set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(3, SECONDS), Inclusive), "Na");
    expected.set(Interval.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Inclusive), "NN");

    assertIterableEquals(expected, mapped);
  }

  @Test
  public void map2oneEndIntOneEndAt() {

    Interval horizon = Interval.between(0, Inclusive, 4, Inclusive, SECONDS);
    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra(horizon));
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra(horizon));

    left.set(Interval.between(2, 4, SECONDS), "b");

    right.set(Interval.between(1, 3, SECONDS), "a");
    right.set(Interval.at(Duration.of(4, SECONDS)), "b");


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
    expected.set(Interval.between(Duration.of(0, SECONDS), Inclusive, Duration.of(1, SECONDS), Exclusive), "NN");
    expected.set(Interval.between(Duration.of(1, SECONDS), Inclusive, Duration.of(2, SECONDS), Exclusive), "Na");
    expected.set(Interval.between(Duration.of(2, SECONDS), Inclusive, Duration.of(3, SECONDS), Inclusive), "ba");
    expected.set(Interval.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "bN");
    expected.set(Interval.at(Duration.of(4, SECONDS)), "bb");

    assertIterableEquals(expected, mapped);
  }

  @Test
  public void map2oneStartIntStartAt() {

    Interval horizon = Interval.between(0, Inclusive, 4, Inclusive, SECONDS);
    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra(horizon));
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra(horizon));

    left.set(Interval.between(0, 2, SECONDS), "b");

    right.set(Interval.at(Duration.of(0, SECONDS)), "b");
    right.set(Interval.between(1, 3, SECONDS), "a");


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
    expected.set(Interval.at(Duration.of(0, SECONDS)), "bb");
    expected.set(Interval.between(Duration.of(0, SECONDS), Exclusive, Duration.of(1, SECONDS), Exclusive), "bN");
    expected.set(Interval.between(Duration.of(1, SECONDS), Inclusive, Duration.of(2, SECONDS), Inclusive), "ba");
    expected.set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(3, SECONDS), Inclusive), "Na");
    expected.set(Interval.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Inclusive), "NN");

    assertIterableEquals(expected, mapped);
  }

  @Test
  public void map2alternateStartEndAtInt() {

    Interval horizon = Interval.between(0, Inclusive, 4, Inclusive, SECONDS);
    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra(horizon));
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra(horizon));

    left.set(Interval.between(0, 2, SECONDS), "b");
    left.set(Interval.at(Duration.of(4, SECONDS)), "a");

    right.set(Interval.at(Duration.of(0, SECONDS)), "b");
    right.set(Interval.between(1, 4, SECONDS), "a");


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
    expected.set(Interval.at(Duration.of(0, SECONDS)), "bb");
    expected.set(Interval.between(Duration.of(0, SECONDS), Exclusive, Duration.of(1, SECONDS), Exclusive), "bN");
    expected.set(Interval.between(Duration.of(1, SECONDS), Inclusive, Duration.of(2, SECONDS), Inclusive), "ba");
    expected.set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "Na");
    expected.set(Interval.at(Duration.of(4, SECONDS)), "aa");

    assertIterableEquals(expected, mapped);
  }

  @Test
  public void map2nullStartEnd() {

    Interval horizon = Interval.between(0, Inclusive, 4, Inclusive, SECONDS);
    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra(horizon));
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra(horizon));

    left.set(Interval.between(Duration.of(0, SECONDS), Exclusive, Duration.of(3, SECONDS), Exclusive), "a");

    right.set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "b");


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
    expected.set(Interval.at(Duration.of(0, SECONDS)), "NN");
    expected.set(Interval.between(Duration.of(0, SECONDS), Exclusive, Duration.of(2, SECONDS), Inclusive), "aN");
    expected.set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(3, SECONDS), Exclusive), "ab");
    expected.set(Interval.between(Duration.of(3, SECONDS), Inclusive, Duration.of(4, SECONDS), Exclusive), "Nb");
    expected.set(Interval.at(Duration.of(4, SECONDS)), "NN");

    assertIterableEquals(expected, mapped);
  }

  // behold the oss-o-matic 3000
  @Test
  public void map2allvaluesMassive() {
    //if we have 2 values (note, this implementation of intervalmap is agnostic of the number of possible values, we are
    // just doing this so its easy to enumerate and test everything), we have following cases (left, right):
    // (N, N), (a, N), (a, a), (a, b), (b, N), (b, a), (b, b), (N, a), (N, b)

    //also worth testing intervals that overlap to make sure they get split right
    //as a result, our intervals will be (a is "a", b is "b", - is NULL, @ is a in a Interval.at):

    //           NN B   AANBB N
    //           NA A   ABBBN A
    //left:   ...---bb--aa-bbb---bb--@-b--bb--@--@-...
    //right:  ...-aaaaaaabbb-aa----b-a-@----@--@---...
    //    t:  0123456789012345678901234567890123456789
    //                                            (cutoff here, at 35)


    Interval horizon = Interval.between(0, Inclusive, 35, Inclusive, SECONDS);
    System.out.println(horizon);
    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra(horizon));
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra(horizon));



    left.set(Interval.between(6, 7, SECONDS), "b");
    left.set(Interval.between(10, 11, SECONDS), "a");
    left.set(Interval.between(13, 15, SECONDS), "b");
    left.set(Interval.between(19, 20, SECONDS), "b");
    left.set(Interval.at(Duration.of(23, SECONDS)), "a");
    left.set(Interval.between(25, 26, SECONDS), "b");
    left.set(Interval.between(28, 29, SECONDS), "b");
    left.set(Interval.at(Duration.of(32, SECONDS)), "a");
    left.set(Interval.at(Duration.of(35, SECONDS)), "a");

    right.set(Interval.between(4,10, SECONDS), "a");
    right.set(Interval.between(11,13, SECONDS), "b");
    right.set(Interval.between(15,16, SECONDS), "a");
    right.set(Interval.at(Duration.of(21, SECONDS)), "b");
    right.set(Interval.at(Duration.of(23, SECONDS)), "a");
    right.set(Interval.at(Duration.of(25, SECONDS)), "b");
    right.set(Interval.at(Duration.of(29, SECONDS)), "a");
    right.set(Interval.at(Duration.of(33, SECONDS)), "b");


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
    expected.set(Interval.between(Duration.of(0, SECONDS), Inclusive, Duration.of(4, SECONDS), Exclusive), "NN");
    expected.set(Interval.between(Duration.of(4, SECONDS), Inclusive, Duration.of(6, SECONDS), Exclusive), "Na");
    expected.set(Interval.between(Duration.of(6, SECONDS), Inclusive, Duration.of(7, SECONDS), Inclusive), "ba");
    expected.set(Interval.between(Duration.of(7, SECONDS), Exclusive, Duration.of(10, SECONDS), Exclusive), "Na");
    expected.set(Interval.at(Duration.of(10, SECONDS)), "aa");
    expected.set(Interval.between(Duration.of(10, SECONDS), Exclusive, Duration.of(11, SECONDS), Exclusive), "aN");
    expected.set(Interval.at(Duration.of(11, SECONDS)), "ab");
    expected.set(Interval.between(Duration.of(11, SECONDS), Exclusive, Duration.of(13, SECONDS), Exclusive), "Nb");
    expected.set(Interval.at(Duration.of(13, SECONDS)), "bb");
    expected.set(Interval.between(Duration.of(13, SECONDS), Exclusive, Duration.of(15, SECONDS), Exclusive), "bN");
    expected.set(Interval.at(Duration.of(15, SECONDS)), "ba");
    expected.set(Interval.between(Duration.of(15, SECONDS), Exclusive, Duration.of(16, SECONDS), Inclusive), "Na");
    expected.set(Interval.between(Duration.of(16, SECONDS), Exclusive, Duration.of(19, SECONDS), Exclusive), "NN");
    expected.set(Interval.between(Duration.of(19, SECONDS), Inclusive, Duration.of(20, SECONDS), Inclusive), "bN");
    expected.set(Interval.between(Duration.of(20, SECONDS), Exclusive, Duration.of(21, SECONDS), Exclusive), "NN");
    expected.set(Interval.at(Duration.of(21, SECONDS)), "Nb");
    expected.set(Interval.between(Duration.of(21, SECONDS), Exclusive, Duration.of(23, SECONDS), Exclusive), "NN");
    expected.set(Interval.at(Duration.of(23, SECONDS)), "aa");
    expected.set(Interval.between(Duration.of(23, SECONDS), Exclusive, Duration.of(25, SECONDS), Exclusive), "NN");
    expected.set(Interval.at(Duration.of(25, SECONDS)), "bb");
    expected.set(Interval.between(Duration.of(25, SECONDS), Exclusive, Duration.of(26, SECONDS), Inclusive), "bN");
    expected.set(Interval.between(Duration.of(26, SECONDS), Exclusive, Duration.of(28, SECONDS), Exclusive), "NN");
    expected.set(Interval.between(Duration.of(28, SECONDS), Inclusive, Duration.of(29, SECONDS), Exclusive), "bN");
    expected.set(Interval.at(Duration.of(29, SECONDS)), "ba");
    expected.set(Interval.between(Duration.of(29, SECONDS), Exclusive, Duration.of(32, SECONDS), Exclusive), "NN");
    expected.set(Interval.at(Duration.of(32, SECONDS)), "aN");
    expected.set(Interval.between(Duration.of(32, SECONDS), Exclusive, Duration.of(33, SECONDS), Exclusive), "NN");
    expected.set(Interval.at(Duration.of(33, SECONDS)), "Nb");
    expected.set(Interval.between(Duration.of(33, SECONDS), Exclusive, Duration.of(35, SECONDS), Exclusive), "NN");
    expected.set(Interval.at(Duration.of(35, SECONDS)), "aN");

    assertIterableEquals(expected, mapped);
  }
}
