package gov.nasa.jpl.aerie.scheduler;

import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TestTimeWindows {
  @Test
  @SuppressWarnings("unchecked")
  public void testComplement() {
    Range<Time> r1 = new Range<Time>(new Time(1), new Time(3));
    Range<Time> r2 = new Range<Time>(new Time(2), new Time(4));
    Range<Time> r3 = new Range<Time>(new Time(2), new Time(3));
    Range<Time> r4 = new Range<Time>(new Time(0), new Time(5));


    var tw1 = TimeWindows.of(List.of(new Range<Time>(new Time(1), new Time(2)),
                                     new Range<Time>(new Time(4), new Time(5)),
                                     new Range<Time>(new Time(6), new Time(7))));

    tw1.complement();

    var res1 = TimeWindows.of(List.of(
        new Range<Time>(TimeWindows.createMinTimepoint(), new Time(1)),
        new Range<Time>(new Time(2), new Time(4)),
        new Range<Time>(new Time(5), new Time(6)),
        new Range<Time>(new Time(7), TimeWindows.createMaxTimepoint())));
    assert (tw1.equals(res1));

    var tw2 = new TimeWindows();

    tw2.complement();
    assert (tw2.equals(TimeWindows.of(new Range<Time>(TimeWindows.createMinTimepoint(),
                                                      TimeWindows.createMaxTimepoint()))));

  }

  @Test
  public void testUnion1() {
    //let one timewindows, we set the following cases
    Range<Time> r1 = new Range<Time>(new Time(3), new Time(4));
    Range<Time> r2 = new Range<Time>(new Time(5), new Time(7));
    Range<Time> r3 = new Range<Time>(new Time(10), new Time(11));
    Range<Time> r4 = new Range<Time>(new Time(12), new Time(15));

    TimeWindows tw = TimeWindows.of(List.of(r1, r2, r3, r4));

    //one at very beginning disjointed
    Range<Time> r5 = new Range<Time>(new Time(1), new Time(2));
    tw.union(r5);
    assert (tw.getRangeSet().equals(List.of(r5, r1, r2, r3, r4)));

    //one at very end disjointed
    Range<Time> r6 = new Range<Time>(new Time(17), new Time(18));
    tw.union(r6);
    assert (tw.getRangeSet().equals(List.of(r5, r1, r2, r3, r4, r6)));

    //one at in the middle disjointed
    Range<Time> r7 = new Range<Time>(new Time(8), new Time(9));
    tw.union(r7);
    assert (tw.getRangeSet().equals(List.of(r5, r1, r2, r7, r3, r4, r6)));

    //one intersection end
    Range<Time> r8 = new Range<Time>(new Time(8), new Time(11));
    tw.union(r8);

    assert (tw.getRangeSet().equals(List.of(r5, r1, r2, r8, r4, r6)));

    //one intersection begin
    Range<Time> r9 = new Range<Time>(new Time(13), new Time(16));
    tw.union(r9);
    assert (tw.getRangeSet().equals(List.of(r5, r1, r2, r8, new Range<Time>(new Time(12), new Time(16)), r6)));


  }


  @Test
  public void whenUsingRangeSet_thenCorrect() {
    RangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(com.google.common.collect.Range
                     .open(1, 11));
    rangeSet.add(com.google.common.collect.Range
                     .open(9, 11));


    rangeSet.add(com.google.common.collect.Range.open(11, 12));
    System.out.println(rangeSet);

    var a = com.google.common.collect.Range.closed(1, 2);

  }

  public void union(TreeMap<Time, Time> map, Time t1, Time t2) {

    Map.Entry<Time, Time> floor = map.lowerEntry(t1);
    Map.Entry<Time, Time> ceil = map.higherEntry(t2);
    Time newBegin, newEnd;

    boolean takeFloor = false;
    boolean takeCeil = false;

    if (floor != null && ceil == null) {
      if (floor.getValue().compareTo(t1) > 0) {
        newBegin = Time.min(floor.getKey(), t1);
        takeFloor = true;
      } else {
        newBegin = t1;
      }

      newEnd = Time.max(t2, floor.getValue());

      for (var key : map.tailMap(floor.getKey(), takeFloor).navigableKeySet()) {
        map.remove(key);
      }
      map.put(newBegin, newEnd);


    } else if (floor != null && ceil != null) {
      if (floor.getValue().compareTo(t1) > 0) {
        newBegin = Time.min(floor.getKey(), t1);
        takeFloor = true;
      } else {
        newBegin = t1;
      }

      if (ceil.getKey().compareTo(t2) < 0) {
        newEnd = Time.max(ceil.getKey(), t2);
        takeCeil = true;
      } else {
        newEnd = t2;
      }

      for (var key : map.subMap(floor.getKey(), takeFloor, ceil.getKey(), takeCeil).navigableKeySet()) {
        map.remove(key);
      }
      map.put(newBegin, newEnd);

    } else if (floor == null && ceil != null) {

      newBegin = Time.min(t1, ceil.getKey());

      if (ceil.getKey().compareTo(t2) < 0) {
        newEnd = Time.max(ceil.getKey(), t2);
        takeCeil = true;
      } else {
        newEnd = t2;
      }

      for (var key : map.headMap(ceil.getKey(), takeCeil).navigableKeySet()) {
        map.remove(key);
      }
      map.put(newBegin, newEnd);

    } else if (floor == null && ceil == null) {

      newBegin = Time.min(map.firstKey(), t1);
      newEnd = Time.max(map.lastEntry().getValue(), t2);

      map.clear();
      map.put(newBegin, newEnd);
    }


  }

  @Test
  public void testNewUnion() {

    TreeMap<Time, Time> tw = new TreeMap<Time, Time>();

    Range<Time> r1 = new Range<Time>(new Time(1), new Time(3));
    Range<Time> r2 = new Range<Time>(new Time(2), new Time(4));
    Range<Time> r3 = new Range<Time>(new Time(2), new Time(3));
    Range<Time> r4 = new Range<Time>(new Time(0), new Time(5));
    tw.put(r1.getMinimum(), r1.getMaximum());

    union(tw, r2.getMinimum(), r2.getMaximum());
    System.out.println(tw);
    union(tw, r3.getMinimum(), r3.getMaximum());
    System.out.println(tw);

    union(tw, new Time(1), new Time(2));
    System.out.println(tw);


    union(tw, new Time(10), new Time(11));
    System.out.println(tw);


    union(tw, new Time(0), new Time(1));
    System.out.println(tw);

    //tw.put(r3.getMinimum(), r3.getMaximum());
    // tw.put(r4.getMinimum(), r4.getMaximum());


  }

  @Test
  @SuppressWarnings("unchecked")
  public void testSubstraction() {

    var tw1 = TimeWindows.of(List.of(new Range<Time>(new Time(1), new Time(3)),
                                     new Range<Time>(new Time(4), new Time(6)),
                                     new Range<Time>(new Time(7), new Time(15))));
    var tw2 = TimeWindows.of(List.of(new Range<Time>(new Time(1), new Time(5)),
                                     new Range<Time>(new Time(7), new Time(10))));

    tw1.substraction(tw2);

    var res1 = TimeWindows.of(List.of(new Range<Time>(new Time(5), new Time(6)),
                                      new Range<Time>(new Time(10), new Time(15))));
    assert (res1.equals(tw1));

  }


  public void testUnionDontMerge() {
    Range<Time> r1 = new Range<Time>(new Time(1), new Time(3));
    Range<Time> r2 = new Range<Time>(new Time(2), new Time(4));
    Range<Time> r3 = new Range<Time>(new Time(2), new Time(3));
    Range<Time> r4 = new Range<Time>(new Time(0), new Time(5));

    TimeWindows tw1 = TimeWindows.of(r1);
    tw1.doNotMergeAdjacent();

    tw1.union(r2);

    TimeWindows res1 = TimeWindows.of(new Range<Time>(new Time(1), new Time(4)));
    TimeWindows res2 = new TimeWindows(res1);
    TimeWindows res3 = TimeWindows.of(new Range<Time>(new Time(0), new Time(5)));

    assert (res1.equals(tw1));

    tw1.union(r3);
    assert (tw1.equals(res2));

    tw1.union(r4);
    assert (tw1.equals(res3));

  }

  @Test
  @SuppressWarnings("unchecked")
  public void testUnion() {
    Range<Time> r1 = new Range<Time>(new Time(1), new Time(3));
    Range<Time> r2 = new Range<Time>(new Time(2), new Time(4));
    Range<Time> r3 = new Range<Time>(new Time(2), new Time(3));
    Range<Time> r4 = new Range<Time>(new Time(0), new Time(5));

    TimeWindows tw1 = TimeWindows.of(r1);
    tw1.union(r2);

    TimeWindows res1 = TimeWindows.of(new Range<Time>(new Time(1), new Time(4)));
    TimeWindows res2 = new TimeWindows(res1);
    TimeWindows res3 = TimeWindows.of(new Range<Time>(new Time(0), new Time(5)));

    assert (res1.equals(tw1));

    tw1.union(r3);
    assert (tw1.equals(res2));

    tw1.union(r4);
    assert (tw1.equals(res3));

  }

  @Test
  @SuppressWarnings("unchecked")
  public void testConcat() {
    Range<Time> r1 = new Range<Time>(new Time(0), new Time(2));
    Range<Time> r2 = new Range<Time>(new Time(2), new Time(3));
    List<Range<Time>> a = List.of(r1, r2);
    TimeWindows tw1 = TimeWindows.of(a);
    TimeWindows res1 = TimeWindows.of(new Range<Time>(new Time(0), new Time(3)));

    assert (tw1.equals(res1));

  }

  @Test
  @SuppressWarnings("unchecked")
  public void testConcatDontMerge() {
    Range<Time> r1 = new Range<Time>(new Time(0), new Time(2));
    Range<Time> r2 = new Range<Time>(new Time(2), new Time(3));
    List<Range<Time>> a = new ArrayList<Range<Time>>(List.of(r1, r2));
    TimeWindows tw1 = TimeWindows.of(a, true);
    boolean b = new ArrayList<Range<Time>>(tw1.getRangeSet()).equals(a);
    assert (b);

  }


  @Test
  public void testIntersectionNotMerge() {
    Range<Time> r1 = new Range<Time>(new Time(2), new Time(4));
    Range<Time> r2 = new Range<Time>(new Time(1), new Time(2));
    Range<Time> r3 = new Range<Time>(new Time(2), new Time(3));
    Range<Time> r4 = new Range<Time>(new Time(3), new Time(4));
    Range<Time> r5 = new Range<Time>(new Time(4), new Time(5));

    TimeWindows tw1 = TimeWindows.of(r1, true);
    TimeWindows tw2 = TimeWindows.of(List.of(r2, r3, r4, r5), true);
    tw1.intersection(tw2);

    TimeWindows res = TimeWindows.of(List.of(r3, r4), true);
    assert (tw1.equals(res));


  }

  @Test
  @SuppressWarnings("unchecked")
  public void testIntersection() {
    Range<Time> r1 = new Range<Time>(new Time(0), new Time(2));
    Range<Time> r2 = new Range<Time>(new Time(1), new Time(5));
    Range<Time> r3 = new Range<Time>(new Time(4), new Time(8));
    Range<Time> r4 = new Range<Time>(new Time(6), new Time(7));
    Range<Time> r6 = new Range<Time>(new Time(1), new Time(6));


    TimeWindows tw1 = TimeWindows.of(r1);
    tw1.intersection(r2);
    TimeWindows res1 = TimeWindows.of(new Range<Time>(new Time(1), new Time(2)));
    assert (tw1.equals(res1));

    tw1 = TimeWindows.of(r3);
    tw1.intersection(r4);
    var res2 = TimeWindows.of(r4);
    assert (tw1.equals(res2));

    List<Range<Time>> ranges1 = List.of(r1, r3);
    List<Range<Time>> ranges2 = List.of(r2, r4);

    TimeWindows tw3 = TimeWindows.of(ranges1);
    TimeWindows tw4 = TimeWindows.of(ranges2);

    tw3.intersection(tw4);
    var res3 = TimeWindows.of(List.of(new Range<Time>(new Time(1), new Time(2)),
                                      new Range<Time>(new Time(4), new Time(5)),
                                      new Range<Time>(new Time(6), new Time(7))));
    assert (tw3.equals(res3));


    tw1 = TimeWindows.of(r2);
    tw3 = TimeWindows.of(r6);
    tw1.intersection(tw3);
    res1 = TimeWindows.of(new Range<Time>(new Time(1), new Time(5)));
    assert (tw1.equals(res1));

  }
}
