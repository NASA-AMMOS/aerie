package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.model;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.StateQuery;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Windows;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;

public final class RegisterState<T> implements StateQuery<T> {
    public final TreeMap<Duration, T> changes = new TreeMap<>();
    public final TreeMap<Duration, Boolean> conflicted = new TreeMap<>();
    private Duration elapsedTime;

    public RegisterState(final RegisterState<T> other) {
        this.elapsedTime = other.elapsedTime;
        this.changes.putAll(other.changes);
        this.conflicted.putAll(other.conflicted);
    }

    public RegisterState(final T value) {
        this.elapsedTime = Duration.ZERO;

        this.changes.put(this.elapsedTime, value);
        this.conflicted.put(this.elapsedTime, false);
    }

    public void step(final Duration duration) {
        this.elapsedTime = this.elapsedTime.plus(duration);
    }

    public void set(final T value) {
        this.changes.put(this.elapsedTime, value);
        if (this.conflicted.lastEntry().getValue()) {
            this.conflicted.put(this.elapsedTime, false);
        }
    }

    public void setConflicted() {
        this.conflicted.put(this.elapsedTime, true);
    }

    @Override
    public T get() {
        if (this.isConflicted()) System.err.println("Warning: getting conflicted state");;
        return this.changes.lastEntry().getValue();
    }

    public boolean isConflicted() {
        return this.conflicted.lastEntry().getValue();
    }

    @Override
    public Windows when(final Predicate<T> condition) {
        return matching(this.changes, this.elapsedTime, condition);
    }

    public Windows whenConflicted() {
        return matching(this.conflicted, this.elapsedTime, x -> x);
    }

    @Override
    public String toString() {
        return Objects.toString(this.get());
    }

    private static <T> Optional<T> skipUntil(final Iterator<T> iter, final Predicate<? super T> predicate) {
        while (iter.hasNext()) {
            final var entry = iter.next();
            if (predicate.test(entry)) return Optional.of(entry);
        }
        return Optional.empty();
    }

    public static <T> Windows matching(final Map<Duration, T> changePoints, final Duration endTime, final Predicate<? super T> predicate) {
        final Predicate<Map.Entry<Duration, T>> matchesValue = entry -> predicate.test(entry.getValue());

        final var windows = new Windows();
        final var iter = changePoints.entrySet().iterator();
        while (iter.hasNext()) {
            // Find the first value matching the predicate.
            final var start = skipUntil(iter, matchesValue).map(Map.Entry::getKey);
            // Find the next value not matching the predicate.
            final var end = skipUntil(iter, matchesValue.negate()).map(Map.Entry::getKey);

            // Add the window between these two points.
            start.ifPresent(duration -> windows.add(duration, end.orElse(endTime)));
        }

        return windows;
    }
}
