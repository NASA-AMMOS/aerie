package gov.nasa.jpl.aerie.scheduler.aerie;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.ExternalState;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AerieState<T extends Comparable<T>> implements ExternalState<T> {
  /**
   * A map from times to state values. An entry `(t=v)` means that the state value changed to value `v` at time `t`.
   */
  private NavigableMap<Duration, T> values;

  @SuppressWarnings("unchecked")
  public void updateFromSimulation(Map<Duration, Object> simValues) {
    values = new TreeMap<>(simValues.entrySet().stream().collect(Collectors.toMap(
        Map.Entry::getKey,
        e -> (T) e.getValue()
    )));
  }


  public Type getType() {
    throw new IllegalStateException("Directly instantiated AerieState does not know its type");
  }

  public T getValueAtTime(Duration t) {
    Map.Entry<Duration, T> entry = values.floorEntry(t);
    if (entry == null) {
      return null;
    }
    return entry.getValue();
  }

  /**
   * Helper to calculate the time windows in which a value matches some arbitrary predicate.
   */
  private Windows whenValueIs(Windows windows, Predicate<T> pred) {
    final Windows returnWindows = new Windows();

    for (final Map.Entry<Duration, T> entry : values.entrySet()) {
      if (pred.test(entry.getValue())) {
        final Duration start = entry.getKey();
        Duration end = values.higherKey(start);
        if (end == null) {
          end = windows.maxTimePoint().get();
        }
        final Window stateRange = Window.between(start, Window.Inclusivity.Inclusive, end, Window.Inclusivity.Exclusive);
        for (var range : windows) {
          final Window inter = Window.intersect(range,stateRange);
          if (inter != null) {
            returnWindows.add(inter);
          }
        }
      }
    }
    return returnWindows;
  }

  public Windows whenValueBetween(T inf, T sup, Windows windows) {
    return whenValueIs(windows, v -> v.compareTo(inf) >= 0 && v.compareTo(sup) <= 0);
  }

  public Windows whenValueBelow(T val, Windows windows) {
    return whenValueIs(windows, v -> v.compareTo(val) < 0);
  }

  public Windows whenValueAbove(T val, Windows windows) {
    return whenValueIs(windows, v -> v.compareTo(val) > 0);
  }

  public Windows whenValueEqual(T val, Windows windows) {
    return whenValueIs(windows, v -> v.compareTo(val) == 0);
  }

  public Windows whenValueNotEqual(T val, Windows windows) {
    return whenValueIs(windows, v -> v.compareTo(val) != 0);
  }


  public Map<Window, T> getTimeline(Windows timeDomain) {
    final Map<Window, T> returnMap = new TreeMap<>();
    Duration start = null;
    T value = null;
    for (Map.Entry<Duration, T> entry : values.entrySet()) {
      if (start != null && value != null) {
        returnMap.put(Window.betweenClosedOpen(start, entry.getKey()), value);
      }
      start = entry.getKey();
      value = entry.getValue();
    }
    return returnMap;
  }

  private static <T extends Comparable<T>> AerieState<T> mapHelper(
      List<AerieState<?>> inputStates,
      Function<List<? extends Comparable<?>>, T> mapFunc)
  {
    final NavigableSet<Duration> keys = new TreeSet<>();
    for (final AerieState<?> aerieState : inputStates) {
      keys.addAll(aerieState.values.keySet());
    }

    final NavigableMap<Duration, T> returnValues = new TreeMap<>();
    for (final Duration t : keys) {
      final List<? extends Comparable<?>> inputs = inputStates.stream()
                                                              .map(s -> s.values.floorEntry(t))
                                                              .filter(Objects::nonNull)
                                                              .map(Map.Entry::getValue)
                                                              .collect(Collectors.toList());
      if (inputs.size() < inputStates.size()) {
        continue;
      }
      returnValues.put(t, mapFunc.apply(inputs));
    }
    final AerieState<T> returnState = new AerieState<>();
    returnState.values = returnValues;
    return returnState;
  }

  @SuppressWarnings("unchecked")
  public static <T extends Comparable<T>, A extends Comparable<A>> AerieState<T> map(
      AerieState<A> a,
      Function1<A, T> mapFunc)
  {
    return mapHelper(List.of(a), s -> mapFunc.invoke((A) s.get(0)));
  }

  @SuppressWarnings("unchecked")
  public static <T extends Comparable<T>, A extends Comparable<A>, B extends Comparable<B>> AerieState<T> map(
      AerieState<A> a,
      AerieState<B> b,
      Function2<A, B, T> mapFunc)
  {
    return mapHelper(List.of(a, b), s -> mapFunc.invoke((A) s.get(0), (B) s.get(1)));
  }

  @SuppressWarnings("unchecked")
  public static <T extends Comparable<T>, A extends Comparable<A>, B extends Comparable<B>, C extends Comparable<C>> AerieState<T> map(
      AerieState<A> a,
      AerieState<B> b,
      AerieState<C> c,
      Function3<A, B, C, T> mapFunc)
  {
    return mapHelper(List.of(a, b, c), s -> mapFunc.invoke((A) s.get(0), (B) s.get(1), (C) s.get(2)));
  }

}
