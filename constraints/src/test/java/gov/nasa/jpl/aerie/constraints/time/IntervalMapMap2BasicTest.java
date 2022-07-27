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
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IntervalMapMap2BasicTest {

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

    right.set(Window.between(Duration.of(2, SECONDS), Inclusive, Duration.of(4, SECONDS), Inclusive), "b"); //when there is overlap like this between inclusive and exclusive intervals,
    // it automatically handles it where there is overlap. so if it goes [---] and that shares an end with [--], it becomes [--)[--)

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
  public void map2basicExclusiveStart() {
    IntervalMap<String> left = new IntervalMap<>(new Windows.WindowAlgebra());
    IntervalMap<String> right = new IntervalMap<>(new Windows.WindowAlgebra());

    left.set(Window.between(3, 4, SECONDS), "a"); //note, this is [3,4], not [3,4)
    left.set(Window.between(4, 5, SECONDS), "b"); //note, this is [4,5], not [4,5) -> multivalued at 4, it takes the value that appears latest. so the value at 4 is b

    right.set(Window.between(Duration.of(2, SECONDS), Exclusive, Duration.of(4, SECONDS), Inclusive), "b");

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
    expected.set(Window.between(Duration.MIN_VALUE, Inclusive, Duration.of(2, SECONDS), Inclusive), "NN");
    expected.set(Window.between(Duration.of(2, SECONDS), Exclusive, Duration.of(3, SECONDS), Exclusive), "Nb");
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
  public void map2emptyleftAndCoalesce() {
    IntervalMap<String> left = new IntervalMap<>(new Windows.WindowAlgebra());
    IntervalMap<String> right = new IntervalMap<>(new Windows.WindowAlgebra());

    right.set(Window.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "b");
    right.set(Window.between(4, 5, SECONDS), "b"); //should not coalesce

    right.set(Window.between(Duration.of(7, SECONDS), Exclusive, Duration.of(8, SECONDS), Exclusive), "b"); //should not coalesce with 3-5 above, does not in set
    right.set(Window.between(Duration.of(8, SECONDS), Exclusive, Duration.of(9, SECONDS), Exclusive), "b"); //should not coalesce with 7-8 above, does not in set

    try {
      IntervalMap<String> mapped = IntervalMap.map2(
          left,
          right,
          (a$, b$) -> {
            if (a$.isPresent() && b$.isPresent()) {
              return Optional.of(a$.get() + b$.get());
            } else if (a$.isPresent() && !b$.isPresent()) {
              return Optional.of(a$.get() + "N");
            } else if (b$.isPresent() && !a$.isPresent()) {
              return Optional.of("N" + b$.get());
            } else {
              return Optional.of("NN");
            }
          });

      for (var i : mapped.ascendingOrder()) {
        System.out.println(i);
      }

      IntervalMap<String> expected = new IntervalMap<>(new Windows.WindowAlgebra());
      expected.set(Window.between(Duration.MIN_VALUE, Inclusive, Duration.of(3, SECONDS), Inclusive), "NN");
      expected.set(Window.between(Duration.of(3, SECONDS), Exclusive, Duration.of(5, SECONDS), Inclusive), "Nb");
      expected.set(Window.between(Duration.of(5, SECONDS), Exclusive, Duration.of(7, SECONDS), Inclusive), "NN");
      expected.set(Window.between(Duration.of(7, SECONDS), Exclusive, Duration.of(8, SECONDS), Exclusive), "Nb");
      expected.set(Window.at(Duration.of(8, SECONDS)), "NN");
      expected.set(Window.between(Duration.of(8, SECONDS), Exclusive, Duration.of(9, SECONDS), Exclusive), "Nb");
      expected.set(Window.between(Duration.of(9, SECONDS), Inclusive, Duration.MAX_VALUE, Inclusive), "NN");

      var mappedIter = StreamSupport.stream(mapped.ascendingOrder().spliterator(), false).collect(Collectors.toList());
      var expectedIter = StreamSupport
          .stream(expected.ascendingOrder().spliterator(), false)
          .collect(Collectors.toList());
      for (int i = 0; i < expectedIter.size(); i++) {
        assertEquals(mappedIter.get(i), expectedIter.get(i));
      }
    }
    catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  @Test
  public void map2emptyrightAndCoalesce() {
    IntervalMap<String> left = new IntervalMap<>(new Windows.WindowAlgebra());
    IntervalMap<String> right = new IntervalMap<>(new Windows.WindowAlgebra());

    left.set(Window.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "b");
    left.set(Window.between(4, 5, SECONDS), "b"); //coalesces successfully!

    left.set(Window.between(Duration.of(7, SECONDS), Exclusive, Duration.of(8, SECONDS), Exclusive), "b");
    left.set(Window.between(Duration.of(8, SECONDS), Exclusive, Duration.of(9, SECONDS), Exclusive), "b");

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
    expected.set(Window.between(Duration.MIN_VALUE, Inclusive, Duration.of(3, SECONDS), Inclusive), "NN");
    expected.set(Window.between(Duration.of(3, SECONDS), Exclusive, Duration.of(5, SECONDS), Inclusive), "bN");
    expected.set(Window.between(Duration.of(5, SECONDS), Exclusive, Duration.of(7, SECONDS), Inclusive), "NN");
    expected.set(Window.between(Duration.of(7, SECONDS), Exclusive, Duration.of(8, SECONDS), Exclusive), "bN");
    expected.set(Window.at(Duration.of(8, SECONDS)), "NN");
    expected.set(Window.between(Duration.of(8, SECONDS), Exclusive, Duration.of(9, SECONDS), Exclusive), "bN");
    expected.set(Window.between(Duration.of(9, SECONDS), Inclusive, Duration.MAX_VALUE, Inclusive), "NN");

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
    expected.set(Window.between(Duration.MIN_VALUE, Inclusive, Duration.MAX_VALUE, Inclusive), "NN");

    var mappedIter = StreamSupport.stream(mapped.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    var expectedIter = StreamSupport.stream(expected.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    for (int i = 0; i < expectedIter.size(); i++) {
      assertEquals(mappedIter.get(i), expectedIter.get(i));
    }
  }

}
