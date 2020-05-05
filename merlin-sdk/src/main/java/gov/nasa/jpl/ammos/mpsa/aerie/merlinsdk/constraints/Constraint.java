package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;

import java.util.List;

@FunctionalInterface
public interface Constraint {
    List<Window> getWindows();

    default Constraint and(final Constraint other) {
        return and(this, other);
    }

    default Constraint or(final Constraint other) {
        return or(this, other);
    }

    default Constraint minus(final Constraint other) { return minus(this, other); }

    static Constraint and(final Constraint x, final Constraint y) {
        return () -> Operator.intersection(x.getWindows(), y.getWindows());
    }

    static Constraint or(final Constraint x, final Constraint y) {
        return () -> Operator.union(x.getWindows(), y.getWindows());
    }

    static Constraint minus(final Constraint x, final Constraint y) {
        return () -> Operator.minus(x.getWindows(), y.getWindows());
    }
}
