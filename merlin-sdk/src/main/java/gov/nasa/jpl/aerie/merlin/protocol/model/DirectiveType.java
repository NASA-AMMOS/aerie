package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;

import java.util.Map;

/**
 * A type of directive that can be issued to a Merlin model.
 *
 * <p> A <i>directive</i> is a means by which a model's environment can influence the model's behavior. For instance, a
 * space relay system might normally take no autonomous actions of its own; instead, it waits until it receives a
 * request to store and forward a given payload to another node in the system. </p>
 *
 * <p> A directive can be instantiated using its associated {@link InputType}, and its associated behavior can be
 * extracted using the {@link #getTaskFactory(Model)} method. When the directive's behavior is finished, its
 * result value can be manipulated using its associated {@link OutputType}. </p>
 *
 * <p> As a reflective interface, {@code DirectiveType} is analogous to {@link java.lang.reflect.Method}. It represents
 * an executable method on a {@code Model} receiver with an {@code Arguments} input and a {@code Result} output. </p>
 *
 * @param <Model>
 *   The model that can be influenced by this directive.
 * @param <Arguments>
 *   The type of the arguments to the directive.
 * @param <Result>
 *   The result of performing the directive to completion.
 */
public interface DirectiveType<Model, Arguments, Result> {
  /** Gets the type of input accepted by this directive. */
  InputType<Arguments> getInputType();

  /** Gets the type of output produced by this directive. */
  // TODO: Remove this, since anything that currently depends on this
  //   ought to instead depend on the topics exported by a model on which
  //   the output data is emitted.
  OutputType<Result> getOutputType();

  /**
   * Initializes a {@link Task} operating on the given model given a directive instance.
   *
   * <p> This method and the {@code Task} returned from the {@code TaskFactory} must never cause the given {@code Model} or {@code Arguments} to be
   * mutated. It may only read/write cells allocated to the model via any {@link gov.nasa.jpl.aerie.merlin.protocol.driver.CellId}
   * references the model holds, or update the private state of the {@code Task} itself. This is because multiple {@code Task}s
   * may reference the {@code Model} or {@code Arguments} concurrently, and updates by one {@code Task} may interfere
   * with the transactional isolation afforded to others. </p>
   */
  TaskFactory<Arguments, Result> getTaskFactory(Model model);

  /**
   * Initializes a {@link Task} given a set of arguments determining the directive instance.
   *
   * <p> This method is a convenience method, allowing callers to avoid binding the generic type {@code T}. This method
   * must behave as though implemented as: </p>
   *
   * {@snippet :
   * final var input = this.getInputType().instantiate(arguments);
   * return this.getTaskFactory(model).butFirst(Task.lift($ -> input));
   * }
   *
   * @param model
   *   The model to perform the directive upon.
   * @param arguments
   *   Arguments uniquely determining the directive to perform.
   * @return
   *   An executable task operating on the model's state, terminating with a {@code Result} value.
   * @throws InstantiationException
   *   When the given arguments do not uniquely determine a directive instance.
   * @see InputType#instantiate(Map)
   * @see #getTaskFactory(Model)
   */
  default TaskFactory<Unit, Result> getTaskFactory(
      final Model model,
      final Map<String, SerializedValue> arguments
  ) throws InstantiationException {
    final var input = this.getInputType().instantiate(arguments);
    return this.getTaskFactory(model).butFirst(Task.lift($ -> input));
  }
}
