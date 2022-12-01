package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;

import java.util.function.Function;

public interface Task<Input, Output> {
  /**
   * Perform one step of the task, returning the next step of the task and the conditions under which to perform it.
   *
   * <p>Clients must only call {@code step()} at most once, and must not invoke {@code step()} after {@link #release()}
   * has been invoked.</p>
   */
  TaskStatus<Output> step(Scheduler scheduler, Input input);

  /**
   * Release any transient system resources allocated to this task.
   *
   * <p>Any system resources held must be released by this method, so that garbage collection can take care of the rest.
   * For instance, if this object makes use of an OS-level Thread, that thread must be explicitly released to avoid
   * resource leaks</p>
   *
   * <p>This method <b>shall not</b> be called on this object after invoking {@code #step(Scheduler)};
   * nor shall {@link #step(Scheduler, Input)} be called after this method.</p>
   */
  default void release() {}

  /**
   * Perform another task following this one.
   *
   * @param <X>
   *   The type of the output of the suffixed task.
   * @param suffix
   *   The task to perform following this one.
   * @return
   *   A task performing this task and the suffixed task in sequence.
   */
  default <X> Task<Input, X> andThen(final Task<Output, X> suffix) {
    return Task.compose(this, suffix);
  }

  /**
   * Perform another task preceding this one.
   *
   * @param <X>
   *   The type of the output of the prefixed task.
   * @param prefix
   *   The task to perform preceding this one.
   * @return
   *   A task performing the prefixed task and this task in sequence.
   */
  default <X> Task<X, Output> butFirst(final Task<X, Input> prefix) {
    return Task.compose(prefix, this);
  }

  /**
   * Chain an effect-free transformation onto the output from this task.
   *
   * @param <X>
   *   The type of the new output from the output transformation.
   * @param step
   *   The transformation to apply over this task's output.
   * @return
   *   A task performing this task and the output transformation in sequence.
   */
  default <X> Task<Input, X> map(final Function<Output, X> step) {
    return this.andThen(Task.lift(step));
  }

  /**
   * Chain an effect-free transformation onto the input to this task.
   *
   * @param <X>
   *   The type of the new input to the input transformation.
   * @param step
   *   The transformation to apply over this task's input.
   * @return
   *   A task performing the input transformation and this task in sequence.
   */
  default <X> Task<X, Output> contramap(final Function<X, Input> step) {
    return this.butFirst(Task.lift(step));
  }


  /**
   * Create a task that performs a pure, side-effect-free function.
   *
   * @param <A>
   *   The input type of the given function.
   * @param <B>
   *   The output type of the given function.
   * @param step
   *   The pure function to perform as a task.
   * @return
   *   A task performing the given pure function.
   */
  // LAW: compose(lift(f), lift(g)) === lift(f.andThen(g))
  // LAW: lift(Function.identity()) === identity()
  static <A, B> Task<A, B> lift(final Function<A, B> step) {
    return (scheduler, input) -> TaskStatus.completed(step.apply(input));
  }

  /**
   * Create a task that performs no effects and produces its input unchanged.
   *
   * @param <A>
   *   The type of input (and hence output) for the task.
   * @return
   *   A task performing no effects and producing its input unchanged.
   */
  static <A> Task<A, A> identity() {
    return Task.lift(Function.identity());
  }

  /**
   * Compose two tasks in sequence, feeding the output of one as input to the other.
   *
   * @param first
   *   The first task to perform.
   * @param second
   *   The second task to perform.
   * @return
   *   A composite task performing the given tasks in sequence.
   * @param <A>
   *   The type of input to the first task.
   * @param <B>
   *   The type of output from the first task and input to the second task.
   * @param <C>
   *   The type of output from the second task.
   */
  // LAW: compose(compose(f, g), h)) === compose(f, compose(g, h))
  // LAW: compose(f, identity()) === f === compose(identity(), f)
  static <A, B, C> Task<A, C> compose(final Task<A, B> first, final Task<B, C> second) {
    return new Task<>() {
      @Override
      public TaskStatus<C> step(final Scheduler scheduler, final A a) {
        final var status = first.step(scheduler, a);
        if (status instanceof TaskStatus.Completed<B> s) {
          return second.step(scheduler, s.returnValue());
        } else if (status instanceof TaskStatus.Delayed<B> s) {
          return TaskStatus.delayed(s.delay(), Task.compose(s.continuation(), second));
        } else if (status instanceof TaskStatus.AwaitingCondition<B> s) {
          return TaskStatus.awaiting(s.condition(), Task.compose(s.continuation(), second));
        } else if (status instanceof TaskStatus.CallingTask<?, B> s) {
          // We need to bind the intermediate result type to a name using a helper method.
          return calling(s);
        } else {
          throw new IllegalArgumentException(
              "Unexpected variant %s of type %s".formatted(status, TaskStatus.class.getCanonicalName()));
        }
      }

      private <Midput> TaskStatus<C> calling(final TaskStatus.CallingTask<Midput, B> status) {
        return TaskStatus.calling(status.child(), Task.compose(status.continuation(), second));
      }

      @Override
      public void release() {
        first.release();
        second.release();
      }
    };
  }
}
