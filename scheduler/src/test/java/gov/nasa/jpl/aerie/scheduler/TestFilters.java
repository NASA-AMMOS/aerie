package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.model.DiscreteProfile;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfilePiece;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.All;
import gov.nasa.jpl.aerie.constraints.tree.DiscreteResource;
import gov.nasa.jpl.aerie.constraints.tree.DiscreteValue;
import gov.nasa.jpl.aerie.constraints.tree.Equal;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.constraints.TimeRangeExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.filters.FilterSequenceMaxGapAfter;
import gov.nasa.jpl.aerie.scheduler.constraints.filters.FilterSequenceMinGapAfter;
import gov.nasa.jpl.aerie.scheduler.constraints.filters.Filters;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Inclusive;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFilters {

  @Test
  public void testLatchFilters() {
    final var horizon = Window.between(Duration.of(0, Duration.SECONDS), Duration.of(50, Duration.SECONDS));
    final var horizonW = new Windows(horizon);
    final var simResults = new SimulationResults(
        Window.between(0, 20, SECONDS),
        List.of(),
        Map.of(),
        Map.of(
            "smallState1", smallState1(horizon),
            "smallState2", smallState2(horizon)
        )
    );

    final var ste = new Equal<>(new DiscreteResource("smallState1"), new DiscreteValue(SerializedValue.of(true)));
    final var ste2 = new All(
        new Equal<>(new DiscreteResource("smallState1"), new DiscreteValue(SerializedValue.of(true))),
        new Equal<>(new DiscreteResource("smallState2"), new DiscreteValue(SerializedValue.of(true)))
    );

    final var tre = new TimeRangeExpression.Builder()
        .from(ste)
        .name("withinEach")
        .build();

    final var filter = new Filters.LatchingBuilder()
        .withinEach(tre)
        .filterFirstBy(Filters.minDuration(Duration.of(3, Duration.SECONDS)))
        .thenFilterBy(Filters.minDuration(Duration.of(4, Duration.SECONDS)))
        .build();

    final var tre2 = new TimeRangeExpression.Builder()
        .name("tre2")
        .from(ste2)
        .thenFilter(filter)
        .build();

    final var res = tre2.computeRange(simResults, null, horizonW);

    assertEquals(
        new Windows(
            Window.betweenClosedOpen(
                Duration.of(3, Duration.SECONDS),
                Duration.of(6, Duration.SECONDS)),
            Window.betweenClosedOpen(
                Duration.of(11, Duration.SECONDS),
                Duration.of(15, Duration.SECONDS))),
        res);
  }

  public DiscreteProfile smallState1(final Window horizon) {
    return new DiscreteProfile(List.of(
        new DiscreteProfilePiece(Window.between(horizon.start.in(SECONDS), Inclusive, 20, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Window.between(20, Inclusive, 25, Exclusive, SECONDS), SerializedValue.of(false)),
        new DiscreteProfilePiece(Window.between(25, Inclusive, horizon.end.in(SECONDS), Exclusive, SECONDS), SerializedValue.of(true))
    ));
  }

  public DiscreteProfile smallState2(final Window horizon) {
    return new DiscreteProfile(List.of(
        new DiscreteProfilePiece(Window.between(horizon.start.in(SECONDS), Inclusive, 2, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Window.between(2, Inclusive, 3, Exclusive, SECONDS), SerializedValue.of(false)),
        new DiscreteProfilePiece(Window.between(3, Inclusive, 6, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Window.between(6, Inclusive, 7, Exclusive, SECONDS), SerializedValue.of(false)),
        new DiscreteProfilePiece(Window.between(7, Inclusive, 10, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Window.between(10, Inclusive, 11, Exclusive, SECONDS), SerializedValue.of(false)),
        new DiscreteProfilePiece(Window.between(11, Inclusive, 15, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Window.between(15, Inclusive, 22, Exclusive, SECONDS), SerializedValue.of(false)),
        new DiscreteProfilePiece(Window.between(22, Inclusive, horizon.end.in(SECONDS), Exclusive, SECONDS), SerializedValue.of(false))
        ));
  }

  @Test
  public void testMaxGapAfter() {
    FilterSequenceMaxGapAfter fsm = new FilterSequenceMaxGapAfter(Duration.of(1, Duration.SECONDS));


    Window r1 = Window.betweenClosedOpen(Duration.of(1, Duration.SECONDS), Duration.of(3, Duration.SECONDS));
    Window r2 = Window.betweenClosedOpen(Duration.of(5, Duration.SECONDS), Duration.of(6, Duration.SECONDS));
    Window r3 = Window.betweenClosedOpen(Duration.of(7, Duration.SECONDS), Duration.of(10, Duration.SECONDS));
    Window r5 = Window.betweenClosedOpen(Duration.of(11, Duration.SECONDS), Duration.of(15, Duration.SECONDS));
    Window r7 = Window.betweenClosedOpen(Duration.of(18, Duration.SECONDS), Duration.of(22, Duration.SECONDS));

    Windows tw = new Windows(Arrays.asList(r1,r2,r3,r5,r7));

    Windows res = fsm.filter(null, null, tw);

    var expected = new Windows(Arrays.asList(r2,r3));
    assertEquals(res, expected);
  }

  @Test
  public void testMinGapAfter() {
    FilterSequenceMinGapAfter fsm = new FilterSequenceMinGapAfter(Duration.of(2, Duration.SECONDS));

    Window r1 = Window.betweenClosedOpen(Duration.of(1, Duration.SECONDS), Duration.of(3, Duration.SECONDS));
    Window r2 = Window.betweenClosedOpen(Duration.of(5, Duration.SECONDS), Duration.of(6, Duration.SECONDS));
    Window r3 = Window.betweenClosedOpen(Duration.of(7, Duration.SECONDS), Duration.of(10, Duration.SECONDS));
    Window r5 = Window.betweenClosedOpen(Duration.of(11, Duration.SECONDS), Duration.of(15, Duration.SECONDS));
    Window r7 = Window.betweenClosedOpen(Duration.of(18, Duration.SECONDS), Duration.of(22, Duration.SECONDS));

    List<Window> ranges = new ArrayList<>();
    ranges.add(r1);
    ranges.add(r2);
    ranges.add(r3);
    ranges.add(r5);
    ranges.add(r7);

    Windows tw = new Windows(ranges);

    Windows expected = new Windows(Arrays.asList(r1,r5,r7));
    Windows res = fsm.filter(null, null, tw);
    assertEquals(res, expected);

  }


}
