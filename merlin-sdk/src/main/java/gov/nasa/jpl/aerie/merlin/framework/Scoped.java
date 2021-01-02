package gov.nasa.jpl.aerie.merlin.framework;

import java.util.function.Supplier;

/**
 * A dynamically-scoped cell.
 *
 * Values pushed into the cell are strictly popped in the same order,
 * and the lifetime of each value is tied to the call stack.
 *
 * See the related concept of
 * <a href="https://cr.openjdk.java.net/~rpressler/loom/loom/sol1_part2.html#scope-variables">scoped variables
 * in Project Loom</a>.
 */
public final class Scoped<T> implements Supplier<T> {
    private final ThreadLocal<T> dynamicSlot;

    private Scoped(final ThreadLocal<T> dynamicSlot) {
        this.dynamicSlot = dynamicSlot;
    }

  /**
   * Create a dynamically-scoped cell containing values of type {@link T}.
   *
   * @param <T> The type of value this cell will contain.
   * @return A new cell that can contain values of type {@link T}.
   */
    public static <T> Scoped<T> create() {
        return new Scoped<>(ThreadLocal.withInitial(() -> null));
    }

    /**
     * Provide a value for a dynamic cell for queries within the provided scope.
     */
    public static <T, Throws extends Throwable>
    void setDynamic(final Scoped<T> cell, final T value, final BlockScope<Throws> scope) throws Throws {
        cell.setWithin(value, scope);
    }

    /**
     * Provide a value for a dynamic cell for queries within the provided scope.
     */
    public static <T, Result, Throws extends Throwable>
    Result setDynamic(final Scoped<T> cell, final T value, final ExpressionScope<Result, Throws> scope) throws Throws {
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
