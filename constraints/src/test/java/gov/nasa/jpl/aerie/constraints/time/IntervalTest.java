package gov.nasa.jpl.aerie.constraints.time;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static gov.nasa.jpl.aerie.constraints.Assertions.assertEquivalent;
import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Inclusive;
import static gov.nasa.jpl.aerie.constraints.time.Window.window;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IntervalTest {

  @Test
  public void map2basic() {
    IntervalMap<String> left = new IntervalMap<>(new Windows.WindowAlgebra());
    IntervalMap<String> right = new IntervalMap<>(new Windows.WindowAlgebra());

    left.set(Window.between(3, 4, SECONDS), "a"); //note, this is [3,4], not [3,4)
    left.set(Window.between(4, 5, SECONDS), "b"); //note, this is [4,5], not [4,5) -> multivalued at 4, it takes the value that appears latest. so the value at 4 is b

    right.set(Window.between(Duration.of(2, SECONDS), Inclusive, Duration.of(4, SECONDS), Exclusive), "b");

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

    IntervalMap<String> expected = new IntervalMap<>(new Windows.WindowAlgebra());
    expected.set(Window.between(Duration.MIN_VALUE, Inclusive, Duration.of(2, SECONDS), Exclusive), "NN");
    expected.set(Window.between(Duration.of(2, SECONDS), Inclusive, Duration.of(3, SECONDS), Exclusive), "Nb");
    expected.set(Window.between(Duration.of(3, SECONDS), Inclusive, Duration.of(4, SECONDS), Exclusive), "ab");
    expected.set(Window.between(Duration.of(4, SECONDS), Inclusive, Duration.of(5, SECONDS), Inclusive), "bN");
    expected.set(Window.between(Duration.of(5, SECONDS), Exclusive, Duration.MAX_VALUE, Inclusive), "NN");

    var mappedIter = StreamSupport.stream(mapped.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    var expectedIter = StreamSupport.stream(expected.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    for (int i = 0; i < expectedIter.size(); i++) {
      assertEquals(mappedIter.get(i), expectedIter.get(i));
    }
  }

  @Test
  public void map2basicInclusiveEnd() {
    IntervalMap<String> left = new IntervalMap<>(new Windows.WindowAlgebra());
    IntervalMap<String> right = new IntervalMap<>(new Windows.WindowAlgebra());

    left.set(Window.between(3, 4, SECONDS), "a"); //note, this is [3,4], not [3,4)
    left.set(Window.between(4, 5, SECONDS), "b"); //note, this is [4,5], not [4,5) -> multivalued at 4, it takes the value that appears latest. so the value at 4 is b

    right.set(Window.between(Duration.of(2, SECONDS), Inclusive, Duration.of(4, SECONDS), Inclusive), "b");

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

    for (var i : mapped.ascendingOrder()) {
      System.out.println(i);
    }

    IntervalMap<String> expected = new IntervalMap<>(new Windows.WindowAlgebra());
    expected.set(Window.between(Duration.MIN_VALUE, Inclusive, Duration.of(2, SECONDS), Exclusive), "NN");
    expected.set(Window.between(Duration.of(2, SECONDS), Inclusive, Duration.of(3, SECONDS), Exclusive), "Nb");
    expected.set(Window.between(Duration.of(3, SECONDS), Inclusive, Duration.of(4, SECONDS), Exclusive), "ab");
    expected.set(Window.at(Duration.of(4, SECONDS)), "bb");
    expected.set(Window.between(Duration.of(4, SECONDS), Exclusive, Duration.of(5, SECONDS), Inclusive), "bN");
    expected.set(Window.between(Duration.of(5, SECONDS), Exclusive, Duration.MAX_VALUE, Inclusive), "NN");

    var mappedIter = StreamSupport.stream(mapped.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    var expectedIter = StreamSupport.stream(expected.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    for (int i = 0; i < expectedIter.size(); i++) {
      assertEquals(mappedIter.get(i), expectedIter.get(i));
    }
  }

  @Test
  public void map2emptyleft() {
    IntervalMap<String> left = new IntervalMap<>(new Windows.WindowAlgebra());
    IntervalMap<String> right = new IntervalMap<>(new Windows.WindowAlgebra());

    right.set(Window.between(3, 4, SECONDS), "a");
    right.set(Window.between(4, 5, SECONDS), "b");

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

    for (var i : mapped.ascendingOrder()) {
      System.out.println(i);
    }

    IntervalMap<String> expected = new IntervalMap<>(new Windows.WindowAlgebra());
    expected.set(Window.between(Duration.MIN_VALUE, Inclusive, Duration.of(2, SECONDS), Exclusive), "NN");
    expected.set(Window.between(Duration.of(2, SECONDS), Inclusive, Duration.of(3, SECONDS), Exclusive), "Nb");
    expected.set(Window.between(Duration.of(3, SECONDS), Inclusive, Duration.of(4, SECONDS), Exclusive), "ab");
    expected.set(Window.at(Duration.of(4, SECONDS)), "bb");
    expected.set(Window.between(Duration.of(4, SECONDS), Exclusive, Duration.of(5, SECONDS), Inclusive), "bN");
    expected.set(Window.between(Duration.of(5, SECONDS), Exclusive, Duration.MAX_VALUE, Inclusive), "NN");

    var mappedIter = StreamSupport.stream(mapped.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    var expectedIter = StreamSupport.stream(expected.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    for (int i = 0; i < expectedIter.size(); i++) {
      assertEquals(mappedIter.get(i), expectedIter.get(i));
    }
  }

  @Test
  public void map2emptyright() {
    IntervalMap<String> left = new IntervalMap<>(new Windows.WindowAlgebra());
    IntervalMap<String> right = new IntervalMap<>(new Windows.WindowAlgebra());

    left.set(Window.between(3, 4, SECONDS), "a");
    left.set(Window.between(4, 5, SECONDS), "b");

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

    for (var i : mapped.ascendingOrder()) {
      System.out.println(i);
    }

    IntervalMap<String> expected = new IntervalMap<>(new Windows.WindowAlgebra());
    expected.set(Window.between(Duration.MIN_VALUE, Inclusive, Duration.of(2, SECONDS), Exclusive), "NN");
    expected.set(Window.between(Duration.of(2, SECONDS), Inclusive, Duration.of(3, SECONDS), Exclusive), "Nb");
    expected.set(Window.between(Duration.of(3, SECONDS), Inclusive, Duration.of(4, SECONDS), Exclusive), "ab");
    expected.set(Window.at(Duration.of(4, SECONDS)), "bb");
    expected.set(Window.between(Duration.of(4, SECONDS), Exclusive, Duration.of(5, SECONDS), Inclusive), "bN");
    expected.set(Window.between(Duration.of(5, SECONDS), Exclusive, Duration.MAX_VALUE, Inclusive), "NN");

    var mappedIter = StreamSupport.stream(mapped.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    var expectedIter = StreamSupport.stream(expected.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    for (int i = 0; i < expectedIter.size(); i++) {
      assertEquals(mappedIter.get(i), expectedIter.get(i));
    }
  }

  @Test
  public void map2emptyall() {
    IntervalMap<String> left = new IntervalMap<>(new Windows.WindowAlgebra());
    IntervalMap<String> right = new IntervalMap<>(new Windows.WindowAlgebra());

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

    for (var i : mapped.ascendingOrder()) {
      System.out.println(i);
    }

    IntervalMap<String> expected = new IntervalMap<>(new Windows.WindowAlgebra());
    expected.set(Window.between(Duration.MIN_VALUE, Inclusive, Duration.of(2, SECONDS), Exclusive), "NN");
    expected.set(Window.between(Duration.of(2, SECONDS), Inclusive, Duration.of(3, SECONDS), Exclusive), "Nb");
    expected.set(Window.between(Duration.of(3, SECONDS), Inclusive, Duration.of(4, SECONDS), Exclusive), "ab");
    expected.set(Window.at(Duration.of(4, SECONDS)), "bb");
    expected.set(Window.between(Duration.of(4, SECONDS), Exclusive, Duration.of(5, SECONDS), Inclusive), "bN");
    expected.set(Window.between(Duration.of(5, SECONDS), Exclusive, Duration.MAX_VALUE, Inclusive), "NN");

    var mappedIter = StreamSupport.stream(mapped.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    var expectedIter = StreamSupport.stream(expected.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    for (int i = 0; i < expectedIter.size(); i++) {
      assertEquals(mappedIter.get(i), expectedIter.get(i));
    }
  }

  @Test
  public void map2allvaluesNullStartNullEnd() {
    //if we have 2 values (note, this implementation of intervalmap is agnostic of the number of possible values, we are
    // just doing this so its easy to enumerate and test everything), we have following cases (left, right):
    // (N, N), (a, N), (a, a), (a, b), (b, N), (b, a), (b, b), (N, a), (N, b)

    //also worth testing intervals that overlap to make sure they get split right
    //as a result, our intervals will be (a is "a", b is "b", - is NULL, @ is a in a Window.at):

    //           NN B   AANBB N
    //           NA A   ABBBN A
    //left:   ...---bb--aa-bbb---bb--@-b--bb--@--@-...
    //right:  ...-aaaaaaabbb-aa----b-a-@----@--@---...
    //    t:  0123456789012345678901234567890123456789
    //                                            (cutoff here, at 35)


    Window horizon = Window.between(0, Inclusive, 35, Inclusive, SECONDS);
    System.out.println(horizon);
    IntervalMap<String> left = new IntervalMap<>(new Windows.WindowAlgebra(horizon));
    IntervalMap<String> right = new IntervalMap<>(new Windows.WindowAlgebra(horizon));



    left.set(Window.between(6, 7, SECONDS), "b");
    left.set(Window.between(10, 11, SECONDS), "a");
    left.set(Window.between(13, 15, SECONDS), "b");
    left.set(Window.between(19, 20, SECONDS), "b");
    left.set(Window.at(Duration.of(23, SECONDS)), "a");
    left.set(Window.between(25, 26, SECONDS), "b");
    left.set(Window.between(28, 29, SECONDS), "b");
    left.set(Window.at(Duration.of(32, SECONDS)), "a");
    left.set(Window.at(Duration.of(35, SECONDS)), "a");

    right.set(Window.between(4,10, SECONDS), "a");
    right.set(Window.between(11,13, SECONDS), "b");
    right.set(Window.between(15,16, SECONDS), "a");
    right.set(Window.at(Duration.of(21, SECONDS)), "b");
    right.set(Window.at(Duration.of(23, SECONDS)), "a");
    right.set(Window.at(Duration.of(25, SECONDS)), "b");
    right.set(Window.at(Duration.of(29, SECONDS)), "a");
    right.set(Window.at(Duration.of(33, SECONDS)), "b");


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

    for (var i : mapped.ascendingOrder()) {
      System.out.println(i);
    }
  }

  @Test
  public void map2allvaluesAtStartAtEnd() {
    //if we have 2 values (note, this implementation of intervalmap is agnostic of the number of possible values, we are
    // just doing this so its easy to enumerate and test everything), we have following cases (left, right):
    // (N, N), (a, N), (a, a), (a, b), (b, N), (b, a), (b, b), (N, a), (N, b)

    //also worth testing intervals that overlap to make sure they get split right
    //as a result, our intervals will be (a is "a", b is "b", - is NULL, @ is a in a Window.at):

    //           NN B   AANBB N
    //           NA A   ABBBN A
    //left:   ...---bb--aa-bbb---bb--@-b--bb--@--@-...
    //right:  ...-aaaaaaabbb-aa----b-a-@----@--@---...
    //    t:  0123456789012345678901234567890123456789
    //                                            (cutoff here, at 35)


    Window horizon = Window.between(0, Inclusive, 35, Inclusive, SECONDS);
    System.out.println(horizon);
    IntervalMap<String> left = new IntervalMap<>(new Windows.WindowAlgebra(horizon));
    IntervalMap<String> right = new IntervalMap<>(new Windows.WindowAlgebra(horizon));



    left.set(Window.between(6, 7, SECONDS), "b");
    left.set(Window.between(10, 11, SECONDS), "a");
    left.set(Window.between(13, 15, SECONDS), "b");
    left.set(Window.between(19, 20, SECONDS), "b");
    left.set(Window.at(Duration.of(23, SECONDS)), "a");
    left.set(Window.between(25, 26, SECONDS), "b");
    left.set(Window.between(28, 29, SECONDS), "b");
    left.set(Window.at(Duration.of(32, SECONDS)), "a");
    left.set(Window.at(Duration.of(35, SECONDS)), "a");

    right.set(Window.between(4,10, SECONDS), "a");
    right.set(Window.between(11,13, SECONDS), "b");
    right.set(Window.between(15,16, SECONDS), "a");
    right.set(Window.at(Duration.of(21, SECONDS)), "b");
    right.set(Window.at(Duration.of(23, SECONDS)), "a");
    right.set(Window.at(Duration.of(25, SECONDS)), "b");
    right.set(Window.at(Duration.of(29, SECONDS)), "a");
    right.set(Window.at(Duration.of(33, SECONDS)), "b");


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

    for (var i : mapped.ascendingOrder()) {
      System.out.println(i);
    }
  }

  @Test
  public void map2allvaluesIntervalStartIntervalEnd() {
    //if we have 2 values (note, this implementation of intervalmap is agnostic of the number of possible values, we are
    // just doing this so its easy to enumerate and test everything), we have following cases (left, right):
    // (N, N), (a, N), (a, a), (a, b), (b, N), (b, a), (b, b), (N, a), (N, b)

    //also worth testing intervals that overlap to make sure they get split right
    //as a result, our intervals will be (a is "a", b is "b", - is NULL, @ is a in a Window.at):

    //           NN B   AANBB N
    //           NA A   ABBBN A
    //left:   ...---bb--aa-bbb---bb--@-b--bb--@--@-...
    //right:  ...-aaaaaaabbb-aa----b-a-@----@--@---...
    //    t:  0123456789012345678901234567890123456789
    //                                            (cutoff here, at 35)


    Window horizon = Window.between(0, Inclusive, 35, Inclusive, SECONDS);
    System.out.println(horizon);
    IntervalMap<String> left = new IntervalMap<>(new Windows.WindowAlgebra(horizon));
    IntervalMap<String> right = new IntervalMap<>(new Windows.WindowAlgebra(horizon));



    left.set(Window.between(6, 7, SECONDS), "b");
    left.set(Window.between(10, 11, SECONDS), "a");
    left.set(Window.between(13, 15, SECONDS), "b");
    left.set(Window.between(19, 20, SECONDS), "b");
    left.set(Window.at(Duration.of(23, SECONDS)), "a");
    left.set(Window.between(25, 26, SECONDS), "b");
    left.set(Window.between(28, 29, SECONDS), "b");
    left.set(Window.at(Duration.of(32, SECONDS)), "a");
    left.set(Window.at(Duration.of(35, SECONDS)), "a");

    right.set(Window.between(4,10, SECONDS), "a");
    right.set(Window.between(11,13, SECONDS), "b");
    right.set(Window.between(15,16, SECONDS), "a");
    right.set(Window.at(Duration.of(21, SECONDS)), "b");
    right.set(Window.at(Duration.of(23, SECONDS)), "a");
    right.set(Window.at(Duration.of(25, SECONDS)), "b");
    right.set(Window.at(Duration.of(29, SECONDS)), "a");
    right.set(Window.at(Duration.of(33, SECONDS)), "b");


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

    for (var i : mapped.ascendingOrder()) {
      System.out.println(i);
    }
  }

  @Test
  public void map2allvaluesIntervalStartAtStartIntervalEndAtEnd() {
    //if we have 2 values (note, this implementation of intervalmap is agnostic of the number of possible values, we are
    // just doing this so its easy to enumerate and test everything), we have following cases (left, right):
    // (N, N), (a, N), (a, a), (a, b), (b, N), (b, a), (b, b), (N, a), (N, b)

    //also worth testing intervals that overlap to make sure they get split right
    //as a result, our intervals will be (a is "a", b is "b", - is NULL, @ is a in a Window.at):

    //           NN B   AANBB N
    //           NA A   ABBBN A
    //left:   ...---bb--aa-bbb---bb--@-b--bb--@--@-...
    //right:  ...-aaaaaaabbb-aa----b-a-@----@--@---...
    //    t:  0123456789012345678901234567890123456789
    //                                            (cutoff here, at 35)


    Window horizon = Window.between(0, Inclusive, 35, Inclusive, SECONDS);
    System.out.println(horizon);
    IntervalMap<String> left = new IntervalMap<>(new Windows.WindowAlgebra(horizon));
    IntervalMap<String> right = new IntervalMap<>(new Windows.WindowAlgebra(horizon));



    left.set(Window.between(6, 7, SECONDS), "b");
    left.set(Window.between(10, 11, SECONDS), "a");
    left.set(Window.between(13, 15, SECONDS), "b");
    left.set(Window.between(19, 20, SECONDS), "b");
    left.set(Window.at(Duration.of(23, SECONDS)), "a");
    left.set(Window.between(25, 26, SECONDS), "b");
    left.set(Window.between(28, 29, SECONDS), "b");
    left.set(Window.at(Duration.of(32, SECONDS)), "a");
    left.set(Window.at(Duration.of(35, SECONDS)), "a");

    right.set(Window.between(4,10, SECONDS), "a");
    right.set(Window.between(11,13, SECONDS), "b");
    right.set(Window.between(15,16, SECONDS), "a");
    right.set(Window.at(Duration.of(21, SECONDS)), "b");
    right.set(Window.at(Duration.of(23, SECONDS)), "a");
    right.set(Window.at(Duration.of(25, SECONDS)), "b");
    right.set(Window.at(Duration.of(29, SECONDS)), "a");
    right.set(Window.at(Duration.of(33, SECONDS)), "b");


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

    for (var i : mapped.ascendingOrder()) {
      System.out.println(i);
    }
  }
}
