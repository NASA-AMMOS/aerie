package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import java.util.function.Supplier;

/**
 * A dynamically-scoped cell.
 *
 * For best effect, store an instance of this field in a `private static final` class variable,
 * and allow access only via static methods that fetch the current value of the cell.
 */
public final class DynamicCell<T> implements Supplier<T> {
    private final ThreadLocal<T> dynamicSlot;

    private DynamicCell(final ThreadLocal<T> dynamicSlot) {
        this.dynamicSlot = dynamicSlot;
    }

  /**
   * Create a dynamically-scoped cell containing values of type {@link T}.
   *
   * @param <T> The type of value this cell will contain.
   * @return A new cell that can contain values of type {@link T}.
   */
    public static <T> DynamicCell<T> create() {
        return new DynamicCell<>(ThreadLocal.withInitial(() -> null));
    }

    /**
     * Provide a value for a dynamic cell for queries within the provided scope.
     */
    public static <T, Throws extends Throwable>
    void setDynamic(final DynamicCell<T> cell, final T value, final BlockScope<Throws> scope) throws Throws {
        cell.setWithin(value, scope);
    }

    /**
     * Provide a value for a dynamic cell for queries within the provided scope.
     */
    public static <T, Result, Throws extends Throwable>
    Result setDynamic(final DynamicCell<T> cell, final T value, final ExpressionScope<Result, Throws> scope) throws Throws {
        return cell.setWithin(value, scope);
    }

    /**
     * Gets the current value of the dynamic cell.
     *
     * Throws an exception if nobody up-stack is currently serving this cell.
     */
    @Override
    public T get() throws EmptyDynamicCellException {
        final var value = dynamicSlot.get();
        if (value == null) throw new EmptyDynamicCellException();
        return value;
    }

    /**
     * Provide a value for this dynamic cell for queries within the provided scope.
     */
    public <Result, Throws extends Throwable>
    Result setWithin(final T value, final ExpressionScope<Result, Throws> scope) throws Throws {
        final var oldValue = dynamicSlot.get();

        dynamicSlot.set(value);
        try {
            return scope.run();
        } finally {
            dynamicSlot.set(oldValue);
        }
    }

    /**
     * Provide a value for a dynamic cell for queries within the provided scope.
     */
    public <Throws extends Throwable>
    void setWithin(final T value, final BlockScope<Throws> scope) throws Throws {
        this.setWithin(value, () -> { scope.run(); return null; });
    }

    public interface ExpressionScope<Result, Throws extends Throwable> {
        Result run() throws Throws;
    }

    public interface BlockScope<Throws extends Throwable> {
        void run() throws Throws;
    }

    public static class EmptyDynamicCellException extends RuntimeException {}
}
