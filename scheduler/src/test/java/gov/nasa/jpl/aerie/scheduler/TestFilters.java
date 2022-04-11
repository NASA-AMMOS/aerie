package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.constraints.TimeRangeExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.filters.FilterSequenceMaxGapAfter;
import gov.nasa.jpl.aerie.scheduler.constraints.filters.FilterSequenceMinGapAfter;
import gov.nasa.jpl.aerie.scheduler.constraints.filters.Filters;
import gov.nasa.jpl.aerie.scheduler.constraints.filters.TimeWindowsFilter;
import gov.nasa.jpl.aerie.scheduler.constraints.resources.StateConstraintExpression;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class TestFilters {

  @Test
  public void testLatchFilters() {
    Window horizon = Window.between(Duration.of(0, Duration.SECONDS), Duration.of(50, Duration.SECONDS));
    Windows horizonW = new Windows(horizon);
    MockState<Boolean> smallState1 = new SmallState1(horizon);
    MockState<Boolean> smallState2 = new SmallState2(horizon);
    drawHorizon(horizon);
    smallState1.draw();
    smallState2.draw();

    StateConstraintExpression ste = new StateConstraintExpression.Builder()
        .equal(smallState1, SerializedValue.of(true))
        .build();

    StateConstraintExpression ste2 = new StateConstraintExpression.Builder()
        .andBuilder()
        .equal(smallState1, SerializedValue.of(true))
        .equal(smallState2, SerializedValue.of(true))
        .build();

    TimeRangeExpression tre = new TimeRangeExpression.Builder()
        .from(ste)
        .build();

    var b = ste2.findWindows(null, horizonW);

    TimeWindowsFilter filter = new Filters.LatchingBuilder()
        .withinEach(tre)
        .filterFirstBy(Filters.minDuration(Duration.of(3, Duration.SECONDS)))
        .thenFilterBy(Filters.maxDuration(Duration.of(1, Duration.SECONDS)))
        .build();

    TimeRangeExpression tre2 = new TimeRangeExpression.Builder()
        .from(ste2)
        .thenFilter(filter)
        .build();

    Windows res = tre2.computeRange(null,horizonW);

    assert (res.equals(new Windows(Window.betweenClosedOpen(Duration.of(7, Duration.SECONDS), Duration.of(10, Duration.SECONDS)), Window.betweenClosedOpen(Duration.of(25, Duration.SECONDS), Duration.of(50, Duration.SECONDS)))));


  }

  public static class SmallState1 extends MockState<Boolean> {

    public SmallState1(Window horizon) {
      type = SupportedTypes.BOOLEAN;
      values = new LinkedHashMap<>() {{
        put(Window.betweenClosedOpen(horizon.start, Duration.of(20, Duration.SECONDS)), true);
        put(Window.betweenClosedOpen(Duration.of(20, Duration.SECONDS), Duration.of(25, Duration.SECONDS)), false);
        put(Window.betweenClosedOpen(Duration.of(25, Duration.SECONDS), horizon.end), true);

      }};
    }

  }

  public void drawHorizon(Window horizon) {
    int start = (int) horizon.start.in(Duration.SECONDS);
    int end = (int) horizon.end.in(Duration.SECONDS);

    for (int i = start; i < end; i++) {
      System.out.print(i + " ");
      if (i < 10) {
        System.out.print(" ");
      }
    }
    System.out.println();
  }

  public static class SmallState2 extends MockState<Boolean> {

    public SmallState2(Window horizon) {
      type = SupportedTypes.BOOLEAN;
      values = new LinkedHashMap<>() {{
        put(Window.betweenClosedOpen(horizon.start, Duration.of(2, Duration.SECONDS)), true);
        put(Window.betweenClosedOpen(Duration.of(1, Duration.SECONDS), Duration.of(3, Duration.SECONDS)), false);
        put(Window.betweenClosedOpen(Duration.of(4, Duration.SECONDS), Duration.of(6, Duration.SECONDS)), true);
        put(Window.betweenClosedOpen(Duration.of(6, Duration.SECONDS), Duration.of(7, Duration.SECONDS)), false);
        put(Window.betweenClosedOpen(Duration.of(7, Duration.SECONDS), Duration.of(10, Duration.SECONDS)), true);
        put(Window.betweenClosedOpen(Duration.of(10, Duration.SECONDS), Duration.of(11, Duration.SECONDS)), false);
        put(Window.betweenClosedOpen(Duration.of(11, Duration.SECONDS), Duration.of(15, Duration.SECONDS)), true);
        put(Window.betweenClosedOpen(Duration.of(15, Duration.SECONDS), Duration.of(22, Duration.SECONDS)), false);
        put(Window.betweenClosedOpen(Duration.of(22, Duration.SECONDS), horizon.end), true);
      }};
    }

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

    Windows res = fsm.filter(null, tw);

    var expected = new Windows(Arrays.asList(r2,r3));
    assert(res.equals(expected));
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
    Windows res = fsm.filter(null, tw);
    assert(res.equals(expected));

  }


}
