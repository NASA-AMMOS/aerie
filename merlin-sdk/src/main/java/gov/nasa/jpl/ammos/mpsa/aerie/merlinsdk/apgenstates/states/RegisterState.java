package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;

public final class RegisterState {
    public final TreeMap<Duration, Double> changes = new TreeMap<>();
    public final TreeMap<Duration, Boolean> conflicted = new TreeMap<>();
    private Duration elapsedTime;

    public RegisterState(final RegisterState other) {
        this.elapsedTime = other.elapsedTime;
        this.changes.putAll(other.changes);
        this.conflicted.putAll(other.conflicted);
    }

    public RegisterState(final double value) {
        this.elapsedTime = Duration.ZERO;

        this.changes.put(this.elapsedTime, value);
        this.conflicted.put(this.elapsedTime, false);
    }

    public void step(final Duration duration) {
        this.elapsedTime = this.elapsedTime.plus(duration);
    }

    public void set(final double value) {
        this.changes.put(this.elapsedTime, value);
        if (this.conflicted.lastEntry().getValue()) {
            this.conflicted.put(this.elapsedTime, false);
        }
    }

    public void setConflicted() {
        this.conflicted.put(this.elapsedTime, true);
    }

    public double get() {
        if (this.isConflicted()) System.err.println("Warning: getting conflicted state");;
        return this.changes.lastEntry().getValue();
    }

    public boolean isConflicted() {
        return this.conflicted.lastEntry().getValue();
    }

    public List<Window> when(final Predicate<Double> condition) {
        return matching(this.changes, this.elapsedTime, condition);
    }

    public List<Window> whenConflicted() {
        return matching(this.conflicted, this.elapsedTime, x -> x);
    }

    @Override
    public String toString() {
        return Double.toString(this.get());
    }

    private static <T> Optional<T> skipUntil(Iterator<T> iter, Predicate<? super T> predicate) {
        while (iter.hasNext()) {
            final var entry = iter.next();
            if (predicate.test(entry)) return Optional.of(entry);
        }
        return Optional.empty();
    }

    public static <T> List<Window> matching(final Map<Duration, T> changePoints, final Duration endTime, final Predicate<? super T> predicate) {
        final Predicate<Map.Entry<Duration, T>> matchesValue = entry -> predicate.test(entry.getValue());

        final var windows = new ArrayList<Window>();
        final var iter = changePoints.entrySet().iterator();
        while (iter.hasNext()) {
            // Find the first value matching the predicate.
            final var start = skipUntil(iter, matchesValue).map(Map.Entry::getKey);
            // Find the next value not matching the predicate.
            final var end = skipUntil(iter, matchesValue.negate()).map(Map.Entry::getKey);

            // Add the window between these two points.
            start.ifPresent(duration -> windows.add(Window.between(duration, end.orElse(endTime))));
        }

        return windows;
    }
}
