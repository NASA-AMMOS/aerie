
package gov.nasa.jpl.aerie.constraints.time;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class Windows implements Iterable<Interval> {
  private final IntervalSet windows = new IntervalSet(new IntervalAlgebra()); //TODO: restricting the algebra's horizon like we normally do for IntervalMap causes tests to fail

  public Windows() {}

  public Windows(final Windows other) {
    this.windows.addAll(other.windows);
  }

  public Windows(final List<Interval> intervals) {
    for (final var window : intervals) this.add(window);
  }

  public Windows(final Interval... intervals) {
    for (final var window : intervals) this.add(window);
  }


  public void add(final Interval interval) {
    this.windows.add(interval);
  }

  public void addAll(final Windows other) {
    this.windows.addAll(other.windows);
  }

  public void addPoint(final long quantity, final Duration unit) {
    this.add(Interval.at(quantity, unit));
  }

  public static Windows union(final Windows left, final Windows right) {
    final var result = new Windows(left);
    result.addAll(right);
    return result;
  }


  public void subtract(final Interval interval) {
    this.windows.subtract(interval);
  }

  public void subtractAll(final Windows other) {
    this.windows.subtractAll(other.windows);
  }

  public void subtract(final long start, final long end, final Duration unit) {
    this.subtract(Interval.between(start, end, unit));
  }

  public void subtractPoint(final long quantity, final Duration unit) {
    this.subtract(Interval.at(quantity, unit));
  }

  public static Windows minus(final Windows left, final Windows right) {
    final var result = new Windows(left);
    result.subtractAll(right);
    return result;
  }

  public void intersectWith(final Interval interval) {
    this.windows.intersectWith(interval);
  }

  public void intersectWith(final Windows other) {
    this.windows.intersectWithAll(other.windows);
  }

  public void intersectWith(final long start, final long end, final Duration unit) {
    this.intersectWith(Interval.between(start, end, unit));
  }

  public Optional<Duration> minTimePoint(){
    if(!isEmpty()) {
      return Optional.of(this.windows.ascendingOrder().iterator().next().start);
    } else{
      return Optional.empty();
    }
  }
  public Optional<Duration> maxTimePoint(){
    if(!isEmpty()) {
      return Optional.of(this.windows.descendingOrder().iterator().next().end);
    } else{
      return Optional.empty();
    }
  }

  public Windows complement(){
    var ret = Windows.forever();
    ret.subtractAll(this);
    return ret;
  }

  public Windows filterByDuration(Duration minDur, Duration maxDur){
    final var ret = new Windows();
    StreamSupport
        .stream(windows.ascendingOrder().spliterator(), false)
        .filter(win -> win.duration().noShorterThan(minDur) && win.duration().noLongerThan(maxDur))
        .forEach(ret::add);
    return ret;
  }

  public Windows removeFirst(){
    var actualList = StreamSupport
        .stream(windows.ascendingOrder().spliterator(), false)
        .collect(Collectors.toList());
    if(!actualList.isEmpty())
      actualList.remove(0);
    return new Windows(actualList);
  }

  public Windows removeLast(){
    var actualList = StreamSupport
        .stream(windows.ascendingOrder().spliterator(), false)
        .collect(Collectors.toList());
    if(!actualList.isEmpty())
      actualList.remove(actualList.size()-1);
    return new Windows(actualList);
  }

  public Windows shiftBy(Duration fromStart, Duration fromEnd){
    Windows ret = new Windows();
    StreamSupport.stream(windows.ascendingOrder().spliterator(), false)
                 .forEach((x)-> ret.add(Interval.between(x.start.plus(fromStart), x.startInclusivity, x.end.plus(fromEnd), x.endInclusivity)));
    return ret;
  }

  public Windows removeFirstAndLast(){
    List<Interval> actualList = StreamSupport
        .stream(windows.ascendingOrder().spliterator(), false)
        .collect(Collectors.toList());
    if(actualList.size()>0)
      actualList.remove(0);
    if(actualList.size()>0)
      actualList.remove(actualList.size()-1);
    return new Windows(actualList);
  }

  public Windows subsetContained(Interval gate){
    Windows ret = new Windows();
    for(var win : windows.ascendingOrder()){
      if(gate.contains(win)){
        ret.add(win);
      }
    }
    return ret;
  }

  public int size(){
    return windows.size();
  }

  public static Windows forever(){
    return new Windows(Interval.FOREVER);
  }


  public static Windows subtract(Interval x, Interval y){
    var tmp = new Windows(y);
    tmp.subtract(x);
    return tmp;
  }

  public static Windows intersection(final Windows left, final Windows right) {
    final var result = new Windows(left);
    result.intersectWith(right);
    return result;
  }


  public boolean isEmpty() {
    return this.windows.isEmpty();
  }


  public boolean includes(final Interval probe) {
    return this.windows.includes(probe);
  }

  public boolean includes(final Windows other) {
    return this.windows.includesAll(other.windows);
  }

  public boolean includes(final long start, final long end, final Duration unit) {
    return this.includes(Interval.between(start, end, unit));
  }

  public boolean includesPoint(final long quantity, final Duration unit) {
    return this.includes(Interval.at(quantity, unit));
  }


  @Override
  public Iterator<Interval> iterator() {
    return this.windows.ascendingOrder().iterator();
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof Windows)) return false;
    final var other = (Windows) obj;

    return Objects.equals(this.windows, other.windows);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.windows);
  }

  @Override
  public String toString() {
    return this.windows.toString();
  }
}
