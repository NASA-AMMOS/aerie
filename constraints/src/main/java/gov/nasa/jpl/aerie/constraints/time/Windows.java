package gov.nasa.jpl.aerie.constraints.time;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Inclusive;

public final class Windows implements Iterable<Window> {
  private final IntervalSet<WindowAlgebra, Window> windows = new IntervalSet<>(new WindowAlgebra());

  public Windows() {}

  public Windows(final Windows other) {
    this.windows.addAll(other.windows);
  }

  public Windows(final List<Window> windows) {
    for (final var window : windows) this.add(window);
  }

  public Windows(final Window... windows) {
    for (final var window : windows) this.add(window);
  }


  public void add(final Window window) {
    this.windows.add(window);
  }

  public void addAll(final Windows other) {
    this.windows.addAll(other.windows);
  }

  public void addPoint(final long quantity, final Duration unit) {
    this.add(Window.at(quantity, unit));
  }

  public static Windows union(final Windows left, final Windows right) {
    final var result = new Windows(left);
    result.addAll(right);
    return result;
  }


  public void subtract(final Window window) {
    this.windows.subtract(window);
  }

  public void subtractAll(final Windows other) {
    this.windows.subtractAll(other.windows);
  }

  public void subtract(final long start, final long end, final Duration unit) {
    this.subtract(Window.between(start, end, unit));
  }

  public void subtractPoint(final long quantity, final Duration unit) {
    this.subtract(Window.at(quantity, unit));
  }

  public static Windows minus(final Windows left, final Windows right) {
    final var result = new Windows(left);
    result.subtractAll(right);
    return result;
  }

  public void intersectWith(final Window window) {
    this.windows.intersectWith(window);
  }

  public void intersectWith(final Windows other) {
    this.windows.intersectWithAll(other.windows);
  }

  public void intersectWith(final long start, final long end, final Duration unit) {
    this.intersectWith(Window.between(start, end, unit));
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
        .forEach((x)-> ret.add(Window.between(x.start.plus(fromStart), x.startInclusivity, x.end.plus(fromEnd), x.endInclusivity)));
    return ret;
  }

  public Windows removeFirstAndLast(){
    List<Window> actualList = StreamSupport
        .stream(windows.ascendingOrder().spliterator(), false)
        .collect(Collectors.toList());
    if(actualList.size()>0)
      actualList.remove(0);
    if(actualList.size()>0)
      actualList.remove(actualList.size()-1);
    return new Windows(actualList);
  }

  public Windows subsetContained(Window gate){
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
    return new Windows(Window.FOREVER);
  }


  public static Windows subtract(Window x, Window y){
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


  public boolean includes(final Window probe) {
    return this.windows.includes(probe);
  }

  public boolean includes(final Windows other) {
    return this.windows.includesAll(other.windows);
  }

  public boolean includes(final long start, final long end, final Duration unit) {
    return this.includes(Window.between(start, end, unit));
  }

  public boolean includesPoint(final long quantity, final Duration unit) {
    return this.includes(Window.at(quantity, unit));
  }


  @Override
  public Iterator<Window> iterator() {
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

  private static class WindowAlgebra implements IntervalAlgebra<WindowAlgebra, Window> {
    @Override
    public final boolean isEmpty(Window x) {
      return x.isEmpty();
    }

    @Override
    public final Window unify(final Window x, final Window y) {
      return Window.unify(x, y);
    }

    @Override
    public final Window intersect(final Window x, final Window y) {
      return Window.intersect(x, y);
    }

    @Override
    public final Window lowerBoundsOf(final Window x) {
      if (x.isEmpty()) return Window.FOREVER;
      return Window.between(
          Duration.MIN_VALUE,
          Inclusive, x.start,
          x.startInclusivity.opposite()
      );
    }

    @Override
    public final Window upperBoundsOf(final Window x) {
      if (x.isEmpty()) return Window.FOREVER;
      return Window.between(
          x.end,
          x.endInclusivity.opposite(), Duration.MAX_VALUE,
          Inclusive
      );
    }
  }
}
