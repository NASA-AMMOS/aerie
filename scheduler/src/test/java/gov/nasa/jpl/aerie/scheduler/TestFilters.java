package gov.nasa.jpl.aerie.scheduler;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;

public class TestFilters {

  @Test
  public void testLatchFilters() {

    Range<Time> horizon = new Range<>(new Time(0), new Time(50));

    MockState<Boolean> smallState1 = new SmallState1(horizon);
    MockState<Boolean> smallState2 = new SmallState2(horizon);
    drawHorizon(horizon);
    smallState1.draw();
    smallState2.draw();

    StateConstraintExpression ste = new StateConstraintExpression.Builder()
        .equal(smallState1, true)
        .build();

    StateConstraintExpression ste2 = new StateConstraintExpression.Builder()
        .andBuilder()
        .equal(smallState1, true)
        .equal(smallState2, true)
        .build();

    TimeRangeExpression tre = new TimeRangeExpression.Builder()
        .from(ste)
        .build();


    TimeWindowsFilter filter = new Filters.LatchingBuilder()
        .withinEach(tre)
        .filterFirstBy(Filters.minDuration(new Duration(3)))
        .thenFilterBy(Filters.maxDuration(new Duration(1)))
        .build();

    TimeRangeExpression tre2 = new TimeRangeExpression.Builder()
        .from(ste2)
        .thenFilter(filter)
        .build();

    TimeWindows res = tre2.computeRange(null, TimeWindows.of(horizon));

    assert (res != null);


  }

  public class SmallState1 extends MockState<Boolean> {

    public SmallState1(Range<Time> horizon) {
      values = new LinkedHashMap<Range<Time>, Boolean>() {{
        put(new Range<Time>(horizon.getMinimum(), new Time(20)), true);
        put(new Range<Time>(new Time(20), new Time(25)), false);
        put(new Range<Time>(new Time(25), horizon.getMaximum()), true);

      }};
    }

  }

  public void drawHorizon(Range<Time> horizon) {
    int start = (int) horizon.getMinimum().toEpochMilliseconds() / 1000;
    int end = (int) horizon.getMaximum().toEpochMilliseconds() / 1000;

    for (int i = start; i < end; i++) {
      System.out.print(i + " ");
      if (i < 10) {
        System.out.print(" ");
      }
    }
    System.out.println();
  }

  public class SmallState2 extends MockState<Boolean> {

    public SmallState2(Range<Time> horizon) {
      values = new LinkedHashMap<Range<Time>, Boolean>() {{
        put(new Range<Time>(horizon.getMinimum(), new Time(2)), true);
        put(new Range<Time>(new Time(1), new Time(3)), false);
        put(new Range<Time>(new Time(4), new Time(6)), true);
        put(new Range<Time>(new Time(6), new Time(7)), false);
        put(new Range<Time>(new Time(7), new Time(10)), true);
        put(new Range<Time>(new Time(10), new Time(11)), false);
        put(new Range<Time>(new Time(11), new Time(15)), true);
        put(new Range<Time>(new Time(15), new Time(22)), false);
        put(new Range<Time>(new Time(22), horizon.getMaximum()), true);
      }};
    }

  }

  @Test
  public void testMaxGapAfter() {
    FilterSequenceMaxGapAfter fsm = new FilterSequenceMaxGapAfter(new Duration(1));

    Range<Time> r1 = new Range<Time>(new Time(1), new Time(3));
    Range<Time> r2 = new Range<Time>(new Time(5), new Time(6));
    Range<Time> r3 = new Range<Time>(new Time(6), new Time(7));
    Range<Time> r4 = new Range<Time>(new Time(7), new Time(10));
    Range<Time> r5 = new Range<Time>(new Time(10), new Time(11));
    Range<Time> r6 = new Range<Time>(new Time(11), new Time(15));
    Range<Time> r7 = new Range<Time>(new Time(15), new Time(22));

    Collection<Range<Time>> ranges = new ArrayList<Range<Time>>();
    ranges.add(r1);
    ranges.add(r2);
    ranges.add(r3);
    ranges.add(r4);
    ranges.add(r5);
    ranges.add(r6);
    ranges.add(r7);

    TimeWindows tw = TimeWindows.of(ranges, true);

    TimeWindows res = fsm.filter(null, tw);
    System.out.println(res);
  }

  @Test
  public void testMinGapAfter() {
    FilterSequenceMinGapAfter fsm = new FilterSequenceMinGapAfter(new Duration(1));

    Range<Time> r1 = new Range<Time>(new Time(1), new Time(3));
    Range<Time> r2 = new Range<Time>(new Time(5), new Time(6));
    Range<Time> r3 = new Range<Time>(new Time(6), new Time(7));
    Range<Time> r4 = new Range<Time>(new Time(7), new Time(10));
    Range<Time> r5 = new Range<Time>(new Time(10), new Time(11));
    Range<Time> r6 = new Range<Time>(new Time(11), new Time(15));
    Range<Time> r7 = new Range<Time>(new Time(15), new Time(22));

    Collection<Range<Time>> ranges = new ArrayList<Range<Time>>();
    ranges.add(r1);
    ranges.add(r2);
    ranges.add(r3);
    ranges.add(r4);
    ranges.add(r5);
    ranges.add(r6);
    ranges.add(r7);

    TimeWindows tw = TimeWindows.of(ranges, true);

    TimeWindows res = fsm.filter(null, tw);
    System.out.println(res);

  }


}
