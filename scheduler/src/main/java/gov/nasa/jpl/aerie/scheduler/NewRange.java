package gov.nasa.jpl.aerie.scheduler;

import com.google.common.collect.Range;
import com.google.common.collect.TreeRangeSet;

import java.util.*;

/**
 * represents an inclusive interval of partially ordered values
 *
 * the semantics of a range include both of its endpoints, as well as any
 * value that compares greater than or equal to the minimum and less than or
 * equal to the maximum
 *
 * ranges may have the same minimum and maximum value, in which case it admits
 * only values that compare equal to those bounds
 */
//TODO: use some standard library type
//TODO: probably want some expression of open/half-open intervals too
public class NewRange<T extends Comparable<T>> implements Comparable<NewRange<T>> {

  Range<T> range;

  /**
   * creates a new range that reprsents only a single allowed value
   *
   * @param singleton IN the single value that is admitted by the new range
   */
  public NewRange(T singleton) {
    if (singleton == null) {
      throw new IllegalArgumentException(
          "creating range with null Rangesingleton value");
    }
    range = Range.closed(singleton, singleton);

  }

  public NewRange(Range<T> range) {
    if (range == null) {
      throw new IllegalArgumentException(
          "creating range with null Rangesingleton value");
    }
    this.range = range;

  }

  /**
   * creates a new range with given minimum and maximum bounds
   *
   * the range admits both endpoints and any value between them
   *
   * the minimum and maximum may be the same value
   *
   * @param minimum IN the inclusive minimum bound of the new range
   * @param maximum IN the inclusive maximum bound of the new range
   */
  public NewRange(T minimum, T maximum) {
    //TODO: consider allowing unbounded ranges via null min/max
    if (minimum == null) {
      throw new IllegalArgumentException(
          "creating range with null minimum");
    }
    if (maximum == null) {
      throw new IllegalArgumentException(
          "creating range with null maximum");
    }
    if (minimum.compareTo(maximum) > 0) {
      throw new IllegalArgumentException(
          "creating range with mis-ordered minimum=" + minimum
          + " maximum=" + maximum);
    }
    range = Range.closed(minimum, maximum);

  }


  /**
   * fetch the inclusive lower bound of the range
   *
   * @return the inclusive lower bound of the range
   */
  public T getMinimum() {
    return range.lowerEndpoint();
  }

  /**
   * fetch the inclusive upper bound of the range
   *
   * @return the inclusive upper bound of the range
   */
  public T getMaximum() {
    return range.upperEndpoint();
  }

  /**
   * determines if this range admits the queried value
   *
   * the range inclusively admits values equal to its upper and lower bounds
   * as well as any value between the two
   *
   * @param probe IN the value to test if it is in the range
   * @return true iff the queried value is within the inclusive range, false
   *     otherwise
   */
  public boolean contains(T probe) {

    return (probe.compareTo(range.lowerEndpoint()) >= 0)
           && (probe.compareTo(range.upperEndpoint()) <= 0);
  }

  /**
   * Returns new range from intersection with other range
   *
   * @param otherRange range to be intersected with
   * @return a new range from intersection with otherRange
   */
  public NewRange<T> intersect(NewRange<T> otherRange) {
    return new NewRange<T>(range.intersection(otherRange.range));

  }

  public List<NewRange<T>> subtract(NewRange<T> otherRange) {
    TreeRangeSet<T> a = TreeRangeSet.create();
    a.add(range);
    a.remove(otherRange.range);
    List<NewRange<T>> ret = new ArrayList<NewRange<T>>();
    for (var c : a.asRanges()) {
      ret.add(new NewRange<T>(c));
    }

    return ret;

  }

  public boolean isBefore(@NotNull NewRange<T> otherRange) {
    return this.getMaximum().compareTo(otherRange.getMinimum()) <= 0;
  }

  public boolean isAfter(NewRange<T> otherRange) {
    return this.getMinimum().compareTo(otherRange.getMaximum()) >= 0;
  }

  /**
   * return a range enveloping the two passed ranges
   *
   * @param <T> the value type of the ranges
   * @param range1 the first range to consider
   * @param range2 the second range to consider
   * @return a range enveloping the two passed ranges
   */
  public <T extends Comparable<T>> NewRange<T> envelop(@NotNull NewRange<T> range1, @NotNull NewRange<T> range2) {
    return new NewRange<T>(
        Collections.min(Arrays.asList(range1.getMinimum(), range2.getMinimum())),
        Collections.max(Arrays.asList(range1.getMaximum(), range2.getMaximum())));
  }

  public boolean contains(@NotNull NewRange<T> otherRange) {
    return otherRange.getMinimum().compareTo(this.getMinimum()) >= 0
           && otherRange.getMaximum().compareTo(this.getMaximum()) <= 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.getMinimum(), this.getMaximum());
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean equals(Object o) {
    if (o instanceof NewRange<?>) {
      return equalsRange((NewRange<T>) o);
    }
    return false;
  }

  public boolean equalsRange(NewRange<T> otherRange) {
    return this.getMinimum() == otherRange.getMinimum() && this.getMaximum() == otherRange.getMaximum();
  }

  /**
   * {@inheritDoc}
   *
   * gives a simple human-readable reprsentation of the closed interval range
   */
  @Override
  public String toString() {
    return "[" + getMinimum() + "," + getMaximum() + "]";
  }

  @Override
  public int compareTo(NewRange<T> o) {
    return this.getMinimum().compareTo(o.getMinimum());
  }
}
