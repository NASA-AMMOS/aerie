package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

/**
 * A dynamically-scoped cell.
 *
 * For best effect, store an instance of this field in a `private static final` class variable,
 * and allow access only via static methods that fetch the current value of the cell.
 */
public final class DynamicCell<T> {
    private final ThreadLocal<T> dynamicSlot;

    private DynamicCell(final ThreadLocal<T> dynamicSlot) {
        this.dynamicSlot = dynamicSlot;
    }

    public DynamicCell() {
        this(ThreadLocal.withInitial(() -> null));
    }

    /**
     * Constructs an inheritable cell. Threads spawned in the scope of such a cell obtain their default value
     * from this cell at the point the thread is spawned.
     */
    public static <T> DynamicCell<T> inheritableCell() {
        return new DynamicCell<>(InheritableThreadLocal.withInitial(() -> null));
    }

    /**
     * Gets the current value of the dynamic cell.
     *
     * Throws an exception if nobody up-stack is currently serving this cell.
     */
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
