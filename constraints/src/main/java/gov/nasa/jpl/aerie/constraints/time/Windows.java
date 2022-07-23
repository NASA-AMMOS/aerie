package gov.nasa.jpl.aerie.constraints.time;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Inclusive;

public final class Windows implements Iterable<Pair<Window, Boolean>> {
  private final IntervalMap<Boolean> windows = new IntervalMap<>(new WindowAlgebra());

  public Windows() {}

  public Windows(final Windows other) {
    this.windows.setAll(other.windows);
  }

  public Windows(final List<Pair<Window, Boolean>> windows) { //keep behavior the same, just change the backend. for now, pretend gaps are false, we will fix windows and upstream after IntervalMap is in place, change on a case-by-case basis for gaps
    for (final var window : windows) this.add(window.getKey(), window.getValue());
  }

  public static Windows defaultTrueWindows(List<Window> windows) {
    Windows toReturn = new Windows();
    for (final var window: windows) toReturn.add(window, true);
    return toReturn;
  }

  public Windows(final Pair<Window, Boolean>... windows) {
    for (final var window: windows) this.add(window.getKey(), window.getValue());
  }

  public static Windows defaultTrueWindows(final Window... windows) {
    Windows toReturn = new Windows();
    for (final var window: windows) toReturn.add(window, true);
    return toReturn;
  }

  public void add(final Window window, final Boolean value) {

  }

  public void addAll(final Windows other) {

  }

  public void set(final Window window, final Boolean value) {
    this.windows.set(window, value);
  }

  public void setAll(final Windows other) { //implement in terms of map2
    this.windows.setAll(other.windows);
  }

  public void addPoint(final long quantity, final Duration unit) {
    this.add(Window.at(quantity, unit), true);
  }

  public void addPoint(final long quantity, final Duration unit, final boolean value) {
    this.add(Window.at(quantity, unit), value);
  }

  public static Windows union(final Windows left, final Windows right) {
    final var result = new Windows(left);
    result.addAll(right);
    return result;
  }

  public void clear() {
    ArrayList<Pair<Window, Boolean>> allCurrentIntervals = new ArrayList<Pair<Window, Boolean>>();
    this.windows.ascendingOrder().forEach(allCurrentIntervals::add);
    this.windows.unsetAll(allCurrentIntervals);
  }

  public void nullifyInRange(final List<Window> other) { //instead of unsetAll, new name
    for (var w : other) {
      this.windows.unset(w);
    }
  }
  public void nullifyInRange(final Windows other) {
    for (var w : other) {
      this.windows.unset(w.getKey());
    }
  }

  public void subtract(final Window window) {
    subtractAll(new Windows(List.of(Pair.of(window, false))));
  }

  public void subtractAll(final Windows other) {
    final var intervals = other.windows;
    final var newWindows = IntervalMap.map2(this.windows,
                                            intervals,
                                            (a$, b$) -> b$.isPresent() ? Optional.empty() : a$);
    this.windows.clear();
    this.windows.setAll(newWindows);
    //TODO: make this work such that windows is final
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

  public void intersectWith(final Window window, final boolean value) {
    intersectWith(new Windows(List.of(Pair.of(window, value))));
  }

  public void intersectWith(final Windows other) {
    final var intervals = other.windows;
    IntervalMap<Boolean> newWindows = IntervalMap.map2(
        this.windows,
        intervals,
        (a$, b$) -> { //authored by Jonathan
          if (a$.isPresent()) {
            return (a$.get()) ? b$ : a$; //if a has a value, i.e. there is a window there, the intersection should return b's value (if no b window, then should be null, else a's value of not null)
          }
          else {
            return (b$.orElse(true)) ? a$ : b$; //if b has no value, then it is true and intersection returns b null
            // if b has value, then return a if null or not, implicitly check b.ispresent
          }
        });

    this.windows.clear();
    this.windows.setAll(newWindows);
  }

  public void intersectWith(final long start, final long end, final Duration unit, final boolean value) {
    this.intersectWith(Window.between(start, end, unit), value);
  }

  public Optional<Duration> minTimePoint(){
    if(!isEmpty()) {
      return Optional.of(this.windows.ascendingOrder().iterator().next().getKey().start);
    } else{
      return Optional.empty();
    }
  }
  public Optional<Duration> maxTimePoint(){
    if(!isEmpty()) {
      return Optional.of(this.windows.descendingOrder().iterator().next().getKey().end);
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
        .filter(win -> win.getKey().duration().noShorterThan(minDur) && win.getKey().duration().noLongerThan(maxDur))
        .forEach(window -> ret.add(window.getKey(), window.getValue()));
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
        .forEach((x)-> ret.add(Window.between(x.getKey().start.plus(fromStart), x.getKey().startInclusivity,
                                              x.getKey().end.plus(fromEnd), x.getKey().endInclusivity), x.getValue()));
    return ret;
  }

  public Windows removeFirstAndLast(){
    List<Pair<Window, Boolean>> actualList = StreamSupport
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
      if(gate.contains(win.getKey())){
        ret.add(win.getKey(), win.getValue());
      }
    }
    return ret;
  }

  public int size(){
    return windows.size();
  }

  public static Windows forever(){
    return new Windows(List.of(Pair.of(Window.FOREVER, true)));
  }

  public static Windows forever(boolean value){
    return new Windows(List.of(Pair.of(Window.FOREVER, value)));
  }

  public static Windows subtract(Window x, Window y, boolean value){
    var tmp = new Windows(List.of(Pair.of(y, value)));
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


  public boolean includes(final Window probe, final boolean value) {
    return this.windows.includes(probe, value);
  }

  public boolean includes(final Windows other) {
    //this.windows.
    throw new NotImplementedException();
  }

  public boolean includes(final long start, final long end, final Duration unit, final boolean value) { //TODO: should value defualt to true?
    return this.includes(Window.between(start, end, unit), value);
  }

  public boolean includesPoint(final long quantity, final Duration unit, final boolean value) {
    return this.includes(Window.at(quantity, unit), value);
  }


  @Override
  public Iterator<Pair<Window, Boolean>> iterator() {
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

  public static class WindowAlgebra implements IntervalAlgebra<WindowAlgebra, Window> {

    private Window horizon;

    public WindowAlgebra() {
      horizon = Window.between(Duration.MIN_VALUE, Duration.MAX_VALUE);
    }

    public WindowAlgebra(Window horizon) {
      this.horizon = horizon;
    }

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

    @Override
    public final Window bottom() {
      return Window.between(horizon.start, Exclusive, horizon.start, Exclusive);
    }
  }
}
