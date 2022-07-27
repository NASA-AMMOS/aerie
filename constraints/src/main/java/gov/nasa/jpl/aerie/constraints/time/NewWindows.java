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

public final class NewWindows implements Iterable<Pair<Window, Boolean>> {

  //impossible to specify a planning horizon that goes negative, so, should as well restrict the algebra as the algebra defines bottom() which is
  //  used often in IntervalMap computations.
  private final IntervalMap<Boolean> windows = new IntervalMap<Boolean>(new Windows.WindowAlgebra(Window.between(Duration.ZERO, Duration.MAX_VALUE)));

  public NewWindows() {}

  public NewWindows(final NewWindows other) {
    this.windows.setAll(other.windows);
  }

  public NewWindows(final List<Pair<Window, Boolean>> windows) { //keep behavior the same, just change the backend. for now, pretend gaps are false, we will fix windows and upstream after IntervalMap is in place, change on a case-by-case basis for gaps
    for (final var window : windows) this.set(window.getKey(), window.getValue());
  }

  public static NewWindows defaultTrueWindows(List<Window> windows) {
    NewWindows toReturn = new NewWindows();
    for (final var window: windows) toReturn.set(window, true);
    return toReturn;
  }

  public NewWindows(final Window w, final Boolean b) {
    this.set(w, b);
  }

  //i promise i won't mutate the elements
  @SafeVarargs
  public NewWindows(final Pair<Window, Boolean>... windows) {
    for (final var window: windows) this.set(window.getKey(), window.getValue());
  }

  public static NewWindows defaultTrueWindows(final Window... windows) {
    NewWindows toReturn = new NewWindows();
    for (final var window: windows) toReturn.set(window, true);
    return toReturn;
  }

  public void addAll(final NewWindows other) {
    //Acts like an or operation. If either true, set to true.
    //if one is null, set to other
    //if both null, then set to null
    //can be done with map2

    //orig | new | result
    //  T  |  T  |  T
    //  T  |  F  |  T
    //  T  |  N  |  T
    //  F  |  T  |  T
    //  F  |  F  |  F
    //  F  |  N  |  F
    //  N  |  T  |  T
    //  N  |  F  |  F
    //  N  |  N  |  N

    var newMap = IntervalMap.map2(this.windows,
                     other.windows,
                     (a$, b$) -> {
                        if(a$.isPresent() && b$.isPresent()) {
                          return Optional.of(a$.get() || b$.get());
                        }
                        else if (a$.isPresent()) {
                          return Optional.of(a$.get());
                        }
                        else if (b$.isPresent()) {
                          return Optional.of(b$.get());
                        }
                        else {
                          return Optional.empty();
                        }
                      });
    this.windows.clear();
    this.windows.setAll(newMap);
  }

  public void add(final Window window) {
    this.addAll(new NewWindows(Pair.of(window, true)));
  }

  public void add(final Window window, final Boolean value) {
    this.addAll(new NewWindows(Pair.of(window, value)));
  }

  public void addPoint(final long quantity, final Duration unit) {
    this.add(Window.at(quantity, unit), true);
  }

  public void addPoint(final long quantity, final Duration unit, final boolean value) {
    this.add(Window.at(quantity, unit), value);
  }

  public static NewWindows union(final NewWindows left, final NewWindows right) {
    final var result = new NewWindows(left);
    result.addAll(right);
    return result;
  }

  public void set(final Window window, final Boolean value) {
    this.windows.set(window, value);
  }

  public void setAll(final NewWindows other) { //implement in terms of map2
    this.windows.setAll(other.windows);
  }

  public void clear() {
    ArrayList<Pair<Window, Boolean>> allCurrentIntervals = new ArrayList<Pair<Window, Boolean>>();
    this.windows.ascendingOrder().forEach(allCurrentIntervals::add);
    this.windows.unsetAll(allCurrentIntervals);
  }

  public void unset(final Window other) {
    this.windows.unset(other);
  }

  public void unsetAll(final List<Window> other) {
    for (var w : other) {
      this.windows.unset(w);
    }
  }

  public void unsetAll(final NewWindows other) {
    for (var w : other) {
      this.windows.unset(w.getKey());
    }
  }

  public void subtractAll(final NewWindows other) {
    //Different from unset or unsetall - simply makes false in the window, but if null, stays null
    //if orig is true, other is true, set to false -> true in other means this is where we wanna subtract
    //if orig is true, other is false, keep true -> false in other means don't subtract here. acts the same as null
    //                                                but easier to work with it this way if say, other is retuned from
    //                                                another method.
    //if orig is false, other is true, keep false
    //if orig is false, other is false, keep false
    //if orig is null, keep null
    //if orig is true, other is null, keep true
    //if orig is false, other is null, keep false

    //orig | new | result
    //  T  |  T  |  F
    //  T  |  F  |  T
    //  T  |  N  |  T
    //  F  |  T  |  F
    //  F  |  F  |  F
    //  F  |  N  |  F
    //  N  |  T  |  N
    //  N  |  F  |  N
    //  N  |  N  |  N

    final var newWindows = IntervalMap.map2(
                                                this.windows,
                                                other.windows,
                                                (a$, b$) -> {
                                                  if(a$.isPresent() && b$.isPresent()) {
                                                    if (a$.get()) {
                                                      return Optional.of(!(a$.get() && b$.get()));
                                                    }
                                                    else {
                                                      return Optional.of(a$.get());
                                                    }
                                                  }
                                                  else if (a$.isPresent()) {
                                                    return Optional.of(a$.get());
                                                  }
                                                  else {
                                                    return Optional.empty();
                                                  }
                                                });
    this.windows.clear();
    this.windows.setAll(newWindows);
  }

  public void subtractAll(List<Window> windows) {
    this.subtractAll(NewWindows.defaultTrueWindows(windows));
  }

  public void subtractAll(Window... windows) {
    this.subtractAll(NewWindows.defaultTrueWindows(windows));
  }

  public void subtract(final Window window) {
    this.subtractAll(new NewWindows(window, true));
  }

  public void subtract(final long start, final long end, final Duration unit) {
    this.subtract(Window.between(start, end, unit));
  }

  public void subtractPoint(final long quantity, final Duration unit) {
    this.subtract(Window.at(quantity, unit));
  }

  public static NewWindows minus(final NewWindows left, final NewWindows right) {
    final var result = new NewWindows(left);
    result.subtractAll(right);
    return result;
  }

  //default behavior is subtract x from y, so obviously you'd actually WANT to subtract, only possible if x and y are
  // true
  public static NewWindows subtract(Window x, Window y){
    var tmp = new NewWindows(y, true);
    tmp.subtractAll(new NewWindows(x, true));
    return tmp;
  }

  //but if you want to subtract and maybe you're working with return values, then you want to specify the value of x
  // and y before subtracting.
  public static NewWindows subtract(Window x, boolean xvalue, Window y, boolean yvalue){
    var tmp = new NewWindows(y, yvalue);
    tmp.subtractAll(new NewWindows(x, xvalue));
    return tmp;
  }

  public void intersectWith(final NewWindows other) {
    //if anything is null, the intersection is null.
    //otherwise, intersection is just an AND operation...

    // orig | inFilter | output
    //  T   |    T     |   T
    //  T   |    F     |   F
    //  T   |    N     |   N
    //  F   |    T     |   F
    //  F   |    F     |   F
    //  F   |    N     |   N
    //  N   |    T     |   N
    //  N   |    F     |   N
    //  N   |    N     |   N

    final var intervals = other.windows;
    IntervalMap<Boolean> newWindows = IntervalMap.map2(
        this.windows,
        intervals,
        (a$, b$) -> { //authored by Jonathan
          if (a$.isPresent() && b$.isPresent()) {
            return Optional.of(a$.get() && b$.get());
          }
          return Optional.empty();
        });

    this.windows.clear();
    this.windows.setAll(newWindows);
  }

  public void intersectWith(final Window window) {
    this.intersectWith(new NewWindows(window, true));
  }

  public void intersectWith(final long start, final long end, final Duration unit) {
    this.intersectWith(Window.between(start, end, unit), true);
  }

  public void intersectWith(final Window window, final boolean value) {
    this.intersectWith(new NewWindows(window, value));
  }

  public void intersectWith(final long start, final long end, final Duration unit, final boolean value) {
    this.intersectWith(Window.between(start, end, unit), value);
  }

  public static NewWindows intersection(final NewWindows left, final NewWindows right) {
    final var result = new NewWindows(left);
    result.intersectWith(right);
    return result;
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

  public NewWindows complement(){
    //should not be a subtraction because then if it was null originally, then subtracting original from forever
    //  yields true where once was null, which isn't good. we want a simple inversion of true and false here, without
    //  filling nulls.

    return new NewWindows(StreamSupport.stream(
        IntervalMap.map(this.windows,
                        a$ -> a$.isPresent() ? Optional.of(!a$.get()) : Optional.of(false))
                   .ascendingOrder()
                   .spliterator(),
            false).collect(Collectors.toList()));
  }

  public NewWindows filterByDuration(Duration minDur, Duration maxDur){
    if (minDur.longerThan(maxDur)) {
      throw new IllegalArgumentException("MaxDur %d must be greater than MinDur %d".formatted(minDur.toString(), maxDur.toString()));
    }

    //if you have:
    //  input:  ---TTTTFFFFTTTTFFFFTT---FFFTTTFF--TTT---, 3, 3
    //  output: ---FFFFFFFFFFFFFFFFTT---FFF
    //  then you want to shorten only the trues, and replace them with F, don't mess with nulls
    //  so if false window encountered, keep the same. if true window encountered, and it is not in filter, replace
    //     with false
    //  if true window enountered, and true, keep the same
    //  if null, keep null

    // orig | inFilter | output
    //  T   |    T     |   T
    //  T   |    F     |   F
    //  F   |    T     |   F
    //  F   |    F     |   F
    //  N   |    T     |   N
    //  N   |    F     |   N


    /*final var ret = new NewWindows();
    StreamSupport
        .stream(windows.ascendingOrder().spliterator(), false)
        .filter(win -> win.getKey().duration().noShorterThan(minDur) && win.getKey().duration().noLongerThan(maxDur))
        .forEach(window -> ret.add(window.getKey(), window.getValue()));
    return ret;*/

    final var ret = new NewWindows();
    StreamSupport
        .stream(windows.ascendingOrder().spliterator(), false)
        .filter(win -> win.getKey().duration().noShorterThan(minDur) && win.getKey().duration().noLongerThan(maxDur) && win.getValue())
        .forEach(window -> ret.add(window.getKey(), window.getValue())); //add it (True) if it is true and lies in the filter
    StreamSupport
        .stream(windows.ascendingOrder().spliterator(), false)
        .filter(win -> !(win.getKey().duration().noShorterThan(minDur) && win.getKey().duration().noLongerThan(maxDur) && win.getValue()))
        .forEach(window -> ret.add(window.getKey(), window.getValue())); //add it (False) if it is false or falls out of the filter
    //Null vacuously handled (not added)
    return ret;
  }

  public NewWindows removeFirst(){
    var actualList = StreamSupport
        .stream(windows.ascendingOrder().spliterator(), false)
        .collect(Collectors.toList());
    if(!actualList.isEmpty())
      actualList.remove(0);
    return new NewWindows(actualList);
  }

  public NewWindows removeLast(){
    var actualList = StreamSupport
        .stream(windows.ascendingOrder().spliterator(), false)
        .collect(Collectors.toList());
    if(!actualList.isEmpty())
      actualList.remove(actualList.size()-1);
    return new NewWindows(actualList);
  }

  public NewWindows removeFirstAndLast(){
    List<Pair<Window, Boolean>> actualList = StreamSupport
        .stream(windows.ascendingOrder().spliterator(), false)
        .collect(Collectors.toList());
    if(actualList.size()>0)
      actualList.remove(0);
    if(actualList.size()>0)
      actualList.remove(actualList.size()-1);
    return new NewWindows(actualList);
  }

  public NewWindows shiftBy(Duration fromStart, Duration fromEnd){
    //cases:
    //if true window, add start and end always
    //if false and neighbor is null, do not stretch in that direction, otherwise add

    NewWindows ret = new NewWindows();
    var preStretch = StreamSupport.stream(windows.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    final var length = preStretch.size();

    for (int i = 0; i < length; i++) {
      final var current = preStretch.get(i);
      Duration start = Duration.of(0, Duration.SECOND);
      Duration end = Duration.of(0, Duration.SECOND);
      if (current.getValue()) {
        start = fromStart;
        end = fromEnd;
      }
      else {
        if (i - 1 >= 0 && preStretch.get(i - 1).getKey().adjacent(current.getKey())) { //regardless if true or false
          start = fromStart;
        }
        if (i + 1 < length && preStretch.get(i + 1).getKey().adjacent(current.getKey())) {
          end = fromEnd;
        }
      }
      ret.add(
          Window.between(
              current.getKey().start.plus(start),
              current.getKey().startInclusivity,
              current.getKey().end.plus(end),
              current.getKey().endInclusivity),
          current.getValue());
    }

    return ret;
  }

  public NewWindows subsetContained(Window gate){
    NewWindows ret = new NewWindows();
    for(var win : windows.ascendingOrder()){
      if(gate.contains(win.getKey()) && win.getValue()){
        ret.add(win.getKey(), win.getValue());
      }
    }
    return ret;
  }

  public NewWindows getOverlaps(Window interval) {
    return new NewWindows(this.windows.get(interval));
  }

  public int size(){
    return windows.size();
  }

  public static NewWindows forever(){
    return new NewWindows(Window.FOREVER, true);
  }

  public boolean isEmpty() {
    return this.windows.isEmpty();
  }

  public boolean isFalse() {
    //the main line will or every truth value. if it's all false, this will give false, but we want to know IF we are
    //  all false, so we not the result to say true, as in yes, everything is false.
    return !StreamSupport.stream(this.windows.ascendingOrder().spliterator(), false).collect(Collectors.toList())
                        .stream().map($a -> $a.getValue())
                        .reduce(false, Boolean::logicalOr);
  }

  //the equivalent to includes, but just checking not null, not necessarily for equal.
  public boolean isNotNull(final NewWindows other) {
    //like includes below, but agnostic to values. just sees if, true or false, is it present. This is just the
    //  includes method in IntervalMap.
    //so check inclusion of each window, replace with true or false, then crunch by anding. if all present, then all
    //  true, so return true, else false.
    return StreamSupport.stream(other.windows.ascendingOrder().spliterator(), false).collect(Collectors.toList())
        .stream().map($ -> this.windows.includes($.getKey(), $.getValue()))
        .reduce(true, Boolean::logicalAnd);
  }

  public boolean isNotNull(final Window probe, final boolean value) {
    return this.isNotNull(new NewWindows(probe, value));
  }


  public boolean isNotNull(final long start, final long end, final Duration unit, final boolean value) {
    return this.isNotNull(new NewWindows(Window.between(start, end, unit), value));
  }

  public boolean pointIsNotNull(final long quantity, final Duration unit, final boolean value) {
    return this.isNotNull(new NewWindows(Window.at(quantity, unit), value));
  }


  public boolean includes(final NewWindows other) {
    //if you have:
    //  other:    ---TTTT---TTT------
    //  original: ---------TTTFF-----
    //  then you fail twice, once because first interval not contained at all, second because overlap with false
    //  we can do this with a map2 with a truthtable, so wherever inclusion holds we say true, if its wrong we say false
    //  and then reduce and if there's any falses you failed overall.

    // other |  orig   | output
    //  T    |    T     |   T
    //  T    |    F     |   F
    //  T    |    N     |   F
    //  F    |    T     |   T     //probably won't pass false as a value anyways, but just in case we should handle
    //  F    |    F     |   T     //  in case user passes a NewWindows from another method that has falses...
    //  F    |    N     |   N     //since its false, not a problem if undefined. we handle actual null checks in isNotNull
    //  N    |    T     |   N
    //  N    |    F     |   N
    //  N    |    N     |   N

    final var inclusion = IntervalMap.map2(
        this.windows,
        other.windows,
        ($other, $original) -> {
          if ($other.isPresent()) {
            if($other.get()) {
              if (!$original.isPresent()) {
                return Optional.of(false);
              }
              return Optional.of($other.get() && $original.get());
            }
            else {
              if (!$original.isPresent()) {
                return Optional.empty();
              }
              return Optional.of(false);
            }
          }
          return Optional.empty();
        });

    //anywhere where the above has false means inclusion wasn't perfect, so squash and get a truth value:
    return StreamSupport.stream(inclusion.ascendingOrder().spliterator(), false).collect(Collectors.toList())
                        .stream().map($a -> $a.getValue())
                        .reduce(true, Boolean::logicalAnd);
  }

  public boolean includes(final Window probe) {
    return this.includes(new NewWindows(probe, true));
  }


  public boolean includes(final long start, final long end, final Duration unit) {
    return this.includes(new NewWindows(Window.between(start, end, unit), true));
  }

  public boolean includesPoint(final long quantity, final Duration unit) {
    return this.includes(new NewWindows(Window.at(quantity, unit), true));
  }


  @Override
  public Iterator<Pair<Window, Boolean>> iterator() {
    return this.windows.ascendingOrder().iterator();
  }

  public List<Pair<Window, Boolean>> getListCopy() {
    List<Pair<Window, Boolean>> L = new ArrayList<Pair<Window, Boolean>>();
    iterator().forEachRemaining(L::add);
    return L;
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof NewWindows)) return false;
    final var other = (NewWindows) obj;

    return this.windows.equals(other.windows);
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
