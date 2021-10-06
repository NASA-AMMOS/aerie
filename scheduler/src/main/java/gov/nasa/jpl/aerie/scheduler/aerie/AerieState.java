package gov.nasa.jpl.aerie.scheduler.aerie;

import gov.nasa.jpl.aerie.scheduler.ExternalState;
import gov.nasa.jpl.aerie.scheduler.Range;
import gov.nasa.jpl.aerie.scheduler.Time;
import gov.nasa.jpl.aerie.scheduler.TimeWindows;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;

import java.lang.reflect.Type;
import java.util.Collection;
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
    private NavigableMap<Time, T> values;

    @SuppressWarnings("unchecked")
    public void updateFromSimulation(Map<Time, Object> simValues) {
        values = new TreeMap<>(simValues.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> (T) e.getValue()
        )));
    }


    public Type getType() {
        throw new IllegalStateException("Directly instantiated AerieState does not know its type");
    }

    public T getValueAtTime(Time t) {
        Map.Entry<Time, T> entry = values.floorEntry(t);
        if (entry == null) {
            return null;
        }
        return entry.getValue();
    }

    /**
     * Helper to calculate the time windows in which a value matches some arbitrary predicate.
     */
    private TimeWindows whenValueIs(TimeWindows windows, Predicate<T> pred) {
        final TimeWindows returnWindows = new TimeWindows();

        final Collection<Range<Time>> windowsR = windows.getRangeSet();
        for (final Map.Entry<Time, T> entry : values.entrySet()) {
            if (pred.test(entry.getValue())) {
                final Time start = entry.getKey();
                Time end = values.higherKey(start);
                if (end == null) {
                    end = windows.getMaximum();
                }
                final Range<Time> stateRange = new Range<>(start, end);
                for (final Range<Time> range : windowsR) {
                    final Range<Time> inter = range.intersect(stateRange);
                    if (inter != null) {
                        returnWindows.union(inter);
                    }
                }
            }
        }
        return returnWindows;
    }

    public TimeWindows whenValueBetween(T inf, T sup, TimeWindows windows) {
        return whenValueIs(windows, v -> v.compareTo(inf) >= 0 && v.compareTo(sup) <= 0);
    }

    public TimeWindows whenValueBelow(T val, TimeWindows windows) {
        return whenValueIs(windows, v -> v.compareTo(val) < 0);
    }

    public TimeWindows whenValueAbove(T val, TimeWindows windows) {
        return whenValueIs(windows, v -> v.compareTo(val) > 0);
    }

    public TimeWindows whenValueEqual(T val, TimeWindows windows) {
        return whenValueIs(windows, v -> v.compareTo(val) == 0);
    }

    public TimeWindows whenValueNotEqual(T val, TimeWindows windows) {
        return whenValueIs(windows, v -> v.compareTo(val) != 0);
    }


    public Map<Range<Time>, T> getTimeline(TimeWindows timeDomain) {
        final Map<Range<Time>, T> returnMap = new TreeMap<>();
        Time start = null;
        T value = null;
        for (Map.Entry<Time, T> entry : values.entrySet()) {
            if (start != null && value != null) {
                returnMap.put(new Range<>(start, entry.getKey()), value);
            }
            start = entry.getKey();
            value = entry.getValue();
        }
        return returnMap;
    }

    private static <T extends Comparable<T>> AerieState<T> mapHelper(List<AerieState<?>> inputStates, Function<List<? extends Comparable<?>>, T> mapFunc) {
        final NavigableSet<Time> keys = new TreeSet<>();
        for (final AerieState<?> aerieState : inputStates) {
            keys.addAll(aerieState.values.keySet());
        }

        final NavigableMap<Time, T> returnValues = new TreeMap<>();
        for (final Time t : keys) {
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
    public static <T extends Comparable<T>, A extends Comparable<A>> AerieState<T> map(AerieState<A> a, Function1<A, T> mapFunc) {
        return mapHelper(List.of(a), s -> mapFunc.invoke((A) s.get(0)));
    }

  @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>, A extends Comparable<A>, B extends Comparable<B>> AerieState<T> map(AerieState<A> a, AerieState<B> b, Function2<A, B, T> mapFunc) {
        return mapHelper(List.of(a, b), s -> mapFunc.invoke((A) s.get(0), (B) s.get(1)));
    }

  @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>, A extends Comparable<A>, B extends Comparable<B>, C extends Comparable<C>> AerieState<T> map(AerieState<A> a, AerieState<B> b, AerieState<C> c, Function3<A, B, C, T> mapFunc) {
        return mapHelper(List.of(a, b, c), s -> mapFunc.invoke((A) s.get(0), (B) s.get(1), (C) s.get(2)));
    }

}
