package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints;

import java.util.List;

public final class GeneralSearch {

    private GeneralSearch(){}

    @FunctionalInterface
    public interface Predicate<T> {
        boolean accepts(T value);

        default boolean rejects(final T value) {
            return !this.accepts(value);
        }
    }

    // Assumes that `elements` has increasing order.
    // Assumes that `discriminator` is a monotone function, where false < true.
    public static <T> int bisectBy(final List<T> elements, final Predicate<T> discriminator) {
        // Check boundary cases to avoid traversing a big list for no reason.
        if (elements.isEmpty()) return 0;
        if (discriminator.rejects(elements.get(0))) return 0;
        if (discriminator.accepts(elements.get(elements.size() - 1))) return elements.size();

        // Loop termination condition: (right - left) > (right' - left')
        int left = 1;
        int right = elements.size() - 1;
        while (left < right) {
            final var pivot = left + ((right - left) / 2);
            if (discriminator.accepts(elements.get(pivot))) {
                left = pivot + 1;
            } else {
                right = pivot;
            }
        }
        assert left == right : "The bisection bounds didn't converge!";

        return right;
    }
}
