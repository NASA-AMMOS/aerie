package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Windows;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public interface Constraint {
    Set<String> getActivityIds();
    Set<String> getStateIds();
    Windows getWindows();

    default Constraint and(final Constraint other) {
        return and(this, other);
    }

    default Constraint or(final Constraint other) {
        return or(this, other);
    }

    default Constraint minus(final Constraint other) { return minus(this, other); }

    static Constraint and(final Constraint x, final Constraint y) {
        return combine(x, y, Windows::intersectWith);
    }

    static Constraint or(final Constraint x, final Constraint y) {
        return combine(x, y, Windows::addAll);
    }

    static Constraint minus(final Constraint x, final Constraint y) {
        return combine(x, y, Windows::subtractAll);
    }

    static Constraint createStateConstraint(final String stateId, final Supplier<Windows> windowSupplier) {
        return create(Set.of(), Set.of(stateId), windowSupplier);
    }

    static Constraint createStateConstraint(final Set<String> stateIds, final Supplier<Windows> windowSupplier) {
        return create(Set.of(), stateIds, windowSupplier);
    }

    static Constraint createActivityConstraint(final String activityId, final Supplier<Windows> windowSupplier) {
        return create(Set.of(activityId), Set.of(), windowSupplier);
    }

    static Constraint createActivityConstraint(final Set<String> activityIds, final Supplier<Windows> windowSupplier) {
        return create(activityIds, Set.of(), windowSupplier);
    }

    static Constraint combine(final Constraint x, final Constraint y, final BiConsumer<Windows, Windows> windowCombinator) {
        // Because the affected activityIds and stateIds may be dependent on the computed windows,
        // we must defer their computation until specifically requested.
        Objects.requireNonNull(x);
        Objects.requireNonNull(y);
        Objects.requireNonNull(windowCombinator);
        return new Constraint() {
            @Override
            public Set<String> getActivityIds() {
                final var activityIds = x.getActivityIds();
                activityIds.addAll(y.getActivityIds());
                return activityIds;
            }

            @Override
            public Set<String> getStateIds() {
                final var stateIds = x.getStateIds();
                stateIds.addAll(y.getStateIds());
                return stateIds;
            }

            @Override
            public Windows getWindows() {
                final var windows = x.getWindows();
                windowCombinator.accept(windows, y.getWindows());
                return windows;
            }
        };
    }

    static Constraint create(Supplier<Windows> windowSupplier) {
        return create(Set.of(), Set.of(), windowSupplier);
    }

    static Constraint create(final Set<String> activityIds, final Set<String> stateIds, final Supplier<Windows> windowSupplier) {
        Objects.requireNonNull(activityIds).forEach(Objects::requireNonNull);
        Objects.requireNonNull(stateIds).forEach(Objects::requireNonNull);
        Objects.requireNonNull(windowSupplier);

        return new Constraint() {
            @Override
            public Set<String> getActivityIds() {
                return new HashSet<>(activityIds);
            }

            @Override
            public Set<String> getStateIds() {
                return new HashSet<>(stateIds);
            }

            @Override
            public Windows getWindows() {
                return windowSupplier.get();
            }
        };
    }
}
