package gov.nasa.jpl.aerie.merlin.framework;

/**
 * <p>
 *   A `Scoping` allows a RootModel to be stashed in a thread-local cell, allowing it to be accessed
 *   by model methods regardless of their depth in the stack.
 * </p>
 * <p>
 *   The `contextualizeModel` method should always be used in a `try-with-resources` method,
 *   or in general, in some way enforcing a stack discipline on the thread-local cell. In other words,
 *   this method emulates *dynamic scoping* in combination with a `try-with-resources` method.
 * </p>
 */
public interface Scoping<Registry, Model> {
  Undo contextualizeModel(final RootModel<Registry, Model> model);

  // Undos must not throw exceptions, and should be usable in try-with-resources without an Exception catch-all block.
  interface Undo extends AutoCloseable {
    void close();
  }
}
