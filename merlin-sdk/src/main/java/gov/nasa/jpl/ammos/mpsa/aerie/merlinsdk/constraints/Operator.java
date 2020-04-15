package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Operator {
    private Operator() {}

    /*
     * Assumption: in both methods intersection and union,
     * a and b are composed of in-order, disjoint intervals
     */

    public static List<Window> intersection(final List<Window> a, final List<Window> b) {
        final var intersection = new ArrayList<Window>();

        int i = 0;
        int j = 0;
        while (i < a.size() && j < b.size()) {
            final var x = a.get(i);
            final var y = b.get(j);

            if (x.overlaps(y)) {
                intersection.add(Window.greatestLowerBound(x, y));
            }

            if (x.end.isBefore(y.end)) {
                i += 1;
            } else {
                j += 1;
            }
        }

        return intersection;
    }

    public static List<Window> union(final List<Window> a, final List<Window> b) {
        final var temp = new ArrayList<Window>();
        temp.addAll(a);
        temp.addAll(b);
        temp.sort(Comparator.comparing(w -> w.start));

        final var union = new ArrayList<Window>();

        for (final var window : temp) {
            if (union.isEmpty()) {
                //empty list, add first window
                union.add(window);
            } else if (!union.get(union.size() - 1).overlaps(window)) {
                //no overlap b/w last element in list and next element in temp list
                union.add(window);
            } else {
                //there is an overlap
                union.set(union.size() - 1, Window.leastUpperBound(union.get(union.size() - 1), window));
            }
        }

        return union;
    }

    //returns elements in a that are not in b
    public static List<Window> minus(final List<Window> a, final List<Window> b) {
        return new ArrayList<>(CollectionUtils.subtract(a, b));
    }
}
