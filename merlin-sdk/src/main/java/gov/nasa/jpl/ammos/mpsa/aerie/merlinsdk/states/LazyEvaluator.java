package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * The value of a state can be calculated.  This value should not be recalculated everytime a get() occurs
 * on the state.  The value should only be recalculated if one of the inputs that are used in the calculation
 * have changed.  The recalculation of a state's value only when necessary is referred to as lazy evaluation.
 *
 * Adapters can use this LazyEvaluator to implement lazy evaluation.  Adapters can define what changes to the
 * state object result in its value becoming invalidated.  Adapters must define the calculation to be performed when
 * computing the value.
 *
 * Examples of how you can use the LazyEvaluator are provided in the comments below.
 *
 * @param <T>
 */
public final class LazyEvaluator<T> {

    /**
     * If value is empty, then the value is recomputed.
     * If the value becomes invalidated, then the value is set to empty
     */
    private Optional<T> value = Optional.empty();

    /**
     * The supplier is defined by the adapter using a lambda expression or method reference.
     * An example of using a lambda to supply an adapter:
     *
     * Supplier<String> supplier = () -> {
     *         System.out.println("Hello World!");
     *         return "hello";
     *     };
     */
    private final Supplier<T> supplier;

    /**
     * An adapter can directly construct a LazyEvaluator object by providing a supplier.
     *
     * An adapter can later call this object's get() to lazily compute and return the value of
     * the lambda expression or method reference.
     *
     * @param supplier
     */
    public LazyEvaluator(final Supplier<T> supplier) {
        this.supplier = supplier;
    }

    /**
     * An adapter can get a LazyEvaluator object by indirectly creating it using this method.
     * In this case, the String printed will only occur when the value is recomputed, and not on every get.
     *
     * private final LazyEvaluator<String> greeting = LazyEvaluator.of(() -> {
     *         System.out.println("Hello World!");
     *         return "hello";
     *     });
     *
     * An adapter can later call this greeting.get() to lazily compute and return the value of the lambda expression.
     *
     * @param supplier
     * @param <T>
     * @return
     */
    public static <T> LazyEvaluator<T> of(final Supplier<T> supplier) {
        return new LazyEvaluator<T>(supplier);
    }

    /**
     * This method is used to invalidate the value of a state.  The value of the LazyEvaluator is set to Optional.empty(),
     * and the lambda provided is then evaluated when a get() occurs.
     *
     * Adapters can choose when to invalidate the value for any derived states.  Not all changes to the state object may
     * result in an invalidation, and the adapter has control over when this occurs.
     */
    public void invalidate() {
        this.value = Optional.empty();
    }

    /**
     * This method only evaluates the lambda expression or method reference provided if the value is Optional.empty()
     * @return
     */
    public T get() {
        final T result = this.value.orElseGet(this.supplier);
        this.value = Optional.of(result);
        return result;
    }
}
