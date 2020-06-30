package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public interface Constraint {
    Set<String> getActivityIds();
    Set<String> getStateIds();
    List<Window> getWindows();

    default Constraint and(final Constraint other) {
        return and(this, other);
    }

    default Constraint or(final Constraint other) {
        return or(this, other);
    }

    default Constraint minus(final Constraint other) { return minus(this, other); }

    static Constraint and(final Constraint x, final Constraint y) {
        return combine(x, y, Operator::intersection);
    }

    static Constraint or(final Constraint x, final Constraint y) {
        return combine(x, y, Operator::union);
    }

    static Constraint minus(final Constraint x, final Constraint y) {
        return combine(x, y, Operator::minus);
    }

    static Constraint createStateConstraint(final String stateId, final Supplier<List<Window>> windowSupplier) {
        return create(Set.of(), Set.of(stateId), windowSupplier);
    }

    static Constraint createStateConstraint(final Set<String> stateIds, final Supplier<List<Window>> windowSupplier) {
        return create(Set.of(), stateIds, windowSupplier);
    }

    static Constraint createActivityConstraint(final String activityId, final Supplier<List<Window>> windowSupplier) {
        return create(Set.of(activityId), Set.of(), windowSupplier);
    }

    static Constraint createActivityConstraint(final Set<String> activityIds, final Supplier<List<Window>> windowSupplier) {
        return create(activityIds, Set.of(), windowSupplier);
    }

    static Constraint combine(final Constraint x, final Constraint y, final BiFunction<List<Window>, List<Window>, List<Window>> windowCombinator) {
        final var activityIds = x.getActivityIds();
        activityIds.addAll(y.getActivityIds());
        final var stateIds = x.getStateIds();
        stateIds.addAll(y.getStateIds());
        return create(activityIds, stateIds, () -> windowCombinator.apply(x.getWindows(), y.getWindows()));
    }

    static Constraint create(Supplier<List<Window>> windowSupplier) {
        return create(Set.of(), Set.of(), windowSupplier);
    }

    static Constraint create(final Set<String> activityIds, final Set<String> stateIds, final Supplier<List<Window>> windowSupplier) {
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
            public List<Window> getWindows() {
                return windowSupplier.get();
            }
        };
    }
}
