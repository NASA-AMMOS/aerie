package gov.nasa.jpl.aerie.scheduler;

import java.util.*;

/**
 * a set of temporal intervals expressing a specific temporal domain
 *
 * this container class manages a disjoint set of concrete contiguous temporal
 * intervals (which might be discrete time points)
 *
 * the container compresses its representation by semantically merging any
 * overlapping intervals
 *
 * example use cases include expression of times over which a set of
 * constraints are satisfied
 */
//TODO: should inherit from any normal container api (set is a stretch)
//TODO: abstract so can have as window of generic numeric domain type
public class TimeWindows {

  public boolean doNotMergeAdjacent = false;

  public void doNotMergeAdjacent() {
    this.doNotMergeAdjacent = true;
  }

  public static Time createMinTimepoint() {
    return new Time(0.);
  }

  public static Time createMaxTimepoint() {
    return new Time(Double.MAX_VALUE);
  }

  public static Time startHorizon = createMinTimepoint();
  public static Time endHorizon = createMaxTimepoint();

  public static void setHorizon(Time start, Time end) {
    startHorizon = start;
    endHorizon = end;
  }

  public static TimeWindows spanMax() {
    return TimeWindows.of(new Range<Time>(startHorizon, endHorizon));
  }

  /**
   * ctor creates a new empty set of windows
   */
  public TimeWindows() { }

  public TimeWindows(boolean doNotMergeAdjacent) { if (doNotMergeAdjacent) doNotMergeAdjacent();}


  /**
   * copy ctor duplicates intervals expressed by input windows
   *
   * @param o IN the set of time windows to duplicate
   */
  public TimeWindows(TimeWindows o) {
    this.startToEnd = new TreeMap<>(o.startToEnd);
  }

  public TimeWindows subsetFullyContained(Range<Time> interval) {
    return TimeWindows.of(interval.subsetFullyContained(this.getRangeSet()), true);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TimeWindows that = (TimeWindows) o;
    return startToEnd.equals(that.startToEnd);
  }


  @Override
  public int hashCode() {
    return Objects.hash(startToEnd);
  }

  /**
   * creates a new set of just the single given window
   *
   * @param range IN the single time range to include in the new window set
   * @return a new set containing just the single provided time range
   */
  public static TimeWindows of(Range<Time> range) {
    final var windows = new TimeWindows();
    assert range.getMinimum().compareTo(range.getMaximum()) <= 0;
    windows.startToEnd.put(range.getMinimum(), range.getMaximum());
    return windows;
  }

  /**
   * creates a new set of just the single given window
   *
   * @param range IN the single time range to include in the new window set
   * @param doNotMergeAdjacent x
   * @return a new set containing just the single provided time range
   */
  public static TimeWindows of(Range<Time> range, boolean doNotMergeAdjacent) {
    final var windows = new TimeWindows();
    if (doNotMergeAdjacent) {
      windows.doNotMergeAdjacent();
    }
    assert range.getMinimum().compareTo(range.getMaximum()) <= 0;
    windows.startToEnd.put(range.getMinimum(), range.getMaximum());
    return windows;
  }


  public static TimeWindows of(Collection<Range<Time>> ranges) {
    final var windows = new TimeWindows();
    for (final var range : ranges) {
      windows.union(range);
    }
    return windows;
  }

  public static TimeWindows of(Collection<Range<Time>> ranges, boolean doNotMergeAdjacent) {
    final var windows = new TimeWindows();
    if (doNotMergeAdjacent) {
      windows.doNotMergeAdjacent();
    }
    for (final var range : ranges) {
      windows.union(range);
    }
    return windows;
  }

  public List<ActivityInstance> toWindowActInst(String prefix) {
    List<ActivityInstance> list = new ArrayList<ActivityInstance>();
    int i = 0;
    var at = new ActivityType("Window");
    for (var win : startToEnd.entrySet()) {
      list.add(new ActivityInstance(prefix + i, at, win.getKey(), win.getValue().minus(win.getKey())));
      i += 1;
    }
    return list;
  }

  /**
   * creates a new set of just the single instant
   *
   * @param instant IN the single time point to include in the new window set
   * @return a new set containing just the single provided time point
   */
  public static TimeWindows of(Time instant) {
    final var windows = new TimeWindows();
    windows.startToEnd.put(instant, instant);
    return windows;
  }


  /**
   * creates a new set of just the single instant
   *
   * @param instant IN the single time point to include in the new window set
   * @param doNotMergeAdjacent x
   * @return a new set containing just the single provided time point
   */
  public static TimeWindows of(Time instant, boolean doNotMergeAdjacent) {
    TimeWindows windows = of(instant);
    if (doNotMergeAdjacent) {
      windows.doNotMergeAdjacent();
    }
    return windows;
  }


  /**
   * shortens all individual windows in the set by given offsets
   *
   * any windows that are shortened to less than zero duration are
   * removed from the window set
   *
   * inputs must be positive or zero! (TODO: for now)
   *
   * @param front IN the duration by which to shorten the start of each,
   *     ie the distance to bring each start forward toward the future
   * @param back IN the duration by which to shorten the end of each window,
   *     ie the distance to bring each end back toward the past
   */
  //REVIEW: should this be non-mutating and just return the contracted set?
  public void contractBy(Duration front, Duration back) {

    //TODO: for now can only contract (not extend) since haven't implemented
    //window merging functionality yet
    assert front.compareTo(Duration.ofZero()) >= 0;
    assert back.compareTo(Duration.ofZero()) >= 0;

    final var oldStartToEnd = startToEnd;
    startToEnd = new TreeMap<>();

    //loop over all windows and contract each
    for (final var entry : oldStartToEnd.entrySet()) {
      final var newStart = entry.getKey().plus(front);
      final var newEnd = entry.getValue().minus(back);
      if (newEnd.compareTo(newStart) >= 0) {
        startToEnd.put(newStart, newEnd);
      }
    }

    //NB: no merging needed since only contracted (for now!)
  }

  public void removeFirst() {
    if (startToEnd.size() > 0) {
      startToEnd.remove(startToEnd.firstKey());
    }
  }

  public void removeLast() {
    if (startToEnd.size() > 0) {
      startToEnd.remove(startToEnd.lastKey());
    }
  }

  public void removeFirstLast() {
    removeFirst();
    removeLast();
  }

  public void complement() {

    final var oldStartToEnd = startToEnd;

    startToEnd = new TreeMap<>();
    if (oldStartToEnd.size() == 0) {
      startToEnd.put(createMinTimepoint(), createMaxTimepoint());
    } else {

      var entrySet = oldStartToEnd.entrySet();
      var it = entrySet.iterator();
      Map.Entry<Time, Time> entry = it.next();

      startToEnd.put(createMinTimepoint(), entry.getKey());

      while (it.hasNext()) {
        final var newStart = entry.getValue();
        entry = it.next();
        final var newEnd = entry.getKey();
        startToEnd.put(newStart, newEnd);
      }
      startToEnd.put(entry.getValue(), createMaxTimepoint());
    }
  }

  /**
   * creates a new set of windows from merging two input timewindows
   *
   * @param tw1 IN a set of windows
   * @param tw2 IN a set of windows
   * @return a new set containing a merging of the two input sets
   */
  public static TimeWindows of(TimeWindows tw1, TimeWindows tw2) {
    final var windows = new TimeWindows(tw1);
    windows.union(tw2);
    return windows;
  }

  public static TimeWindows of(TimeWindows tw1, TimeWindows tw2, boolean doNotMergeAdjacent) {
    final var windows = new TimeWindows(tw1);
    if (doNotMergeAdjacent) {
      windows.doNotMergeAdjacent();
    }
    windows.union(tw2);
    return windows;
  }

  /**
   * Merges windows
   *
   * @param windows IN windows to be merged with
   */
  public void union(TimeWindows windows) {
    for (Range<Time> window : windows.getRangeSet()) {
      this.union(window);
    }
  }

  public boolean containsInf() {
    for (var range : startToEnd.entrySet()) {
      if (range.getValue().equals(Time.ofMax())) {
        return true;
      }
    }
    return false;
  }

  public static void showDifference(TimeWindows tw1, TimeWindows tw2) {
    if (tw1.startToEnd.size() != tw2.startToEnd.size()) {
      System.out.println("Difference in size = (" + tw1.startToEnd.size() + "," + tw2.startToEnd.size() + ")");
      return;
    }
    int i = 0;
    for (var t1 : tw1.getRangeSet()) {
      var t2 = tw2.getRangeSet().get(i);
      if (!(t1.equals(t2))) {
        System.out.println("Interval at pos " + i + " are different : " + t1 + " != " + t2);
      }
      i++;
    }
  }


  /**
   * Merges a time interval with the current set of intervals
   *
   * @param window IN the time interval to merge
   *     TODO: consider https://www.wikiwand.com/en/Interval_tree to speed up
   *     make use of  treemap structure rather than going linear : submap()
   */
  public void union(Range<Time> window) {

    Time begin = window.getMinimum();
    Time end = window.getMaximum();

    //special case : instant range
    //if(window.isSingleton())
    //  return;

    //if there are no windows or the inserted window is completely disjoint, we just insert it
    if (startToEnd.size() == 0 || (end.compareTo(this.getMinimum()) < 0 || begin.compareTo(this.getMaximum()) > 0)) {
      startToEnd.put(begin, end);
    }

    ArrayList<Map.Entry<Time, Time>> intervals = new ArrayList<Map.Entry<Time, Time>>();

    // for all windows, overlapping ones are recorded
    for (final var entry : startToEnd.entrySet()) {

      if (!doNotMergeAdjacent) {
        //if begin of current window is greater than end of merging window, break
        if (end.compareTo(entry.getKey()) < 0) {
          break;
        }
        // if begin of merging window is lower than end of current window, add current window to record
        if (begin.compareTo(entry.getValue()) <= 0) {
          intervals.add(entry);
        }

        //if end of merging window is contained in current window, add current window to record and break
        if (end.compareTo(entry.getKey()) >= 0 && end.compareTo(entry.getValue()) <= 0) {
          intervals.add(entry);
          break;
        }
      } else {
        //if begin of current window is greater than end of merging window, break
        if (end.compareTo(entry.getKey()) < 0) {
          break;
        }
        // otherwise, end >= start of entry. Then,if begin of merging window is lower than end of current window, add current window to record
        if (begin.compareTo(entry.getValue()) < 0) {
          intervals.add(entry);
        }

        //completely contained
        if (begin.compareTo(entry.getKey()) >= 0 && end.compareTo(entry.getValue()) <= 0) {
          intervals.add(entry);
          break;
        }

        //if end of merging window is contained in current window, add current window to record and break
        if (end.compareTo(entry.getKey()) > 0 && end.compareTo(entry.getValue()) < 0) {
          intervals.add(entry);
          break;
        }
      }
    }
    //determine merged window begin and end
    Time newBegin, newEnd;
    if (intervals.size() > 0) {
      newBegin = Time.min(begin, intervals.get(0).getKey());
    } else {
      newBegin = begin;
    }
    if (intervals.size() > 0) {
      newEnd = Time.max(end, intervals.get(intervals.size() - 1).getValue());
    } else {
      newEnd = end;
    }

    // and remove overlapping windows
    for (var entryToDelete : intervals) {
      startToEnd.remove(entryToDelete.getKey(), entryToDelete.getValue());
    }
    startToEnd.put(newBegin, newEnd);

  }

  public void intersection(TimeWindows other) {
    intersection(other, false);
  }

  /* intersect this set of windows with another
   */
  public void intersection(TimeWindows other, boolean noInstant) {

    List<Range<Time>> toInsert = new ArrayList<Range<Time>>();

    for (Range<Time> window : other.getRangeSet()) {

      for (final var entry : startToEnd.entrySet()) {
        Range<Time> range = new Range<Time>(entry.getKey(), entry.getValue());
        var intersection = range.intersect(window);
        //if intersection exists and is not single timepoint
        if (intersection != null) {
          if (!noInstant || (noInstant
                             && intersection.getMaximum().minus(intersection.getMinimum()).compareTo(Duration.ofZero())
                                > 0)) {
            toInsert.add(intersection);
          }
        }
      }
    }

    startToEnd.clear();
    for (var ran : toInsert) {
      this.union(ran);
      //startToEnd.put(ran.getMinimum(), ran.getMaximum());
    }
  }

  /**
   * substracts another set of windows from this one
   *
   * @param tw the other set of windows
   */
  public void substraction(TimeWindows tw) {
    TimeWindows complTw = new TimeWindows(tw);
    complTw.complement();
    intersection(complTw, true);
  }

  /**
   * substracts another set of windows from this one
   *
   * @param window the window to remove from this one
   */
  public void substraction(Range<Time> window) {
    substraction(TimeWindows.of(window));
  }

  /**
   * Intersects a time interval with the this set of windows
   *
   * @param window IN the time interval to merge
   */
  public void intersection(Range<Time> window) {
    this.intersection(TimeWindows.of(window));
  }

  /**
   * Intersects a time interval with the this set of windows
   *
   * @param window IN the time interval to merge
   * @return x
   */
  public TimeWindows intersectionNew(Range<Time> window) {
    TimeWindows ret = new TimeWindows(this);
    ret.intersection(TimeWindows.of(window));
    return ret;
  }

  public boolean intersects(Range<Time> window) {
    return !(intersectionNew(window).isEmpty());
  }

  /**
   * swaps the contents of this window set with another set
   *
   * @param o IN/OUT the other set of windows to swap contents with
   */
  public void swap(TimeWindows o) {
    final var tmp = o.startToEnd;
    o.startToEnd = this.startToEnd;
    this.startToEnd = tmp;
  }


  /**
   * returns true if there are no windows in the window set
   *
   * @return true iff there are no windows in the window set
   */
  public boolean isEmpty() {
    return startToEnd.isEmpty();
  }


  /**
   * returns the earliest time that is within any window in the set
   *
   * @return the earliest time within any window in the window set
   */
  public Time getMinimum() {
    if (startToEnd.isEmpty()) {
      return null;
    } else {
      return startToEnd.firstKey();
    }
  }


  /**
   * returns the latest time that is within any window in the set
   *
   * @return the latest time within any window in the window set
   */
  public Time getMaximum() {
    if (startToEnd.isEmpty()) {
      return null;
    } else {
      return startToEnd.lastEntry().getValue();
    }
  }


  public void filterByDuration(Duration minDur, Duration maxDur) {
    Iterator<Map.Entry<Time, Time>> iter = startToEnd.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry<Time, Time> cur = iter.next();
      Duration durWin = cur.getValue().minus(cur.getKey());
      if (maxDur != null && durWin.compareTo(maxDur) > 0) {
        iter.remove();
        continue;
      }
      if (minDur != null && durWin.compareTo(minDur) < 0) {
        iter.remove();
      }
    }

  }


  /**
   * returns a view of the window set as a collection of ranges
   *
   * the view is invalidated if the window set is modified
   *
   * @return an unmodifiable collection of time ranges representing the
   *     windows in the set
   */
  public List<Range<Time>> getRangeSet() {
    //TODO: there are some tricky ways to get actual view to work with eg
    //      iterator transformers
    final var ranges = new ArrayList<Range<Time>>(startToEnd.size());
    for (var entry : startToEnd.entrySet()) {
      ranges.add(new Range<>(entry.getKey(), entry.getValue()));
    }
    return Collections.unmodifiableList(ranges);
  }


  /**
   * serialize the window set to (barely) human readable format
   *
   * @return a textual representation of the window set
   */
  @Override
  public String toString() {
    String out = "{";
    for (final var interval : startToEnd.entrySet()) {
      out += "[" + interval.getKey() + "," + interval.getValue() + "]";
    }
    out += "}";
    return out;
  }

  public int size() {
    return startToEnd.size();
  }

  /**
   * the set of windows in the set as a start-to-end time mapping
   *
   * the map is maintained to be non-overlapping, ie no end time (value) is
   * greater than any subsequent start time (key)
   *
   * this representation allows for easy navigation and minimal duplication
   */
  private TreeMap<Time, Time> startToEnd
      = new TreeMap<>();


}
