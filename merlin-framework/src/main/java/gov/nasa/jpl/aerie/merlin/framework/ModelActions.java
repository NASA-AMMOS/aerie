package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.DirectiveTypeId;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.function.Supplier;

public /*non-final*/ class ModelActions {
  protected ModelActions() {}

  /* package-local */
  static final Scoped<Context> context = Scoped.create();


  public static <T> Context.TaskFactory<T> threaded(final Supplier<T> task) {
    return executor -> new ThreadedTask<>(executor, ModelActions.context, task);
  }

  public static Context.TaskFactory<VoidEnum> threaded(final Runnable task) {
    return threaded(() -> {
      task.run();
      return VoidEnum.VOID;
    });
  }

  public static <T> Context.TaskFactory<T> replaying(final Supplier<T> task) {
    return executor -> new ReplayingTask<>(executor, ModelActions.context, task);
  }

  public static Context.TaskFactory<VoidEnum> replaying(final Runnable task) {
    return replaying(() -> {
      task.run();
      return VoidEnum.VOID;
    });
  }


  public static <T> String spawn(final Supplier<T> task) {
    return spawn(threaded(task));
  }

  public static String spawn(final Runnable task) {
    return spawn(() -> {
      task.run();
      return VoidEnum.VOID;
    });
  }

  public static <T> String spawn(final Context.TaskFactory<T> task) {
    return context.get().spawn(task);
  }

  public static <Input, Output>
  String spawn(final DirectiveTypeId<Input, Output> id, final Input activity, final Task<Output> task) {
    return context.get().spawn(id, activity, task);
  }

  public static void call(final Runnable task) {
    call(threaded(task));
  }

  public static <T> void call(final Supplier<T> task) {
    call(threaded(task));
  }

  public static <T> void call(final Context.TaskFactory<T> task) {
    waitFor(spawn(task));
  }

  public static String defer(final Duration duration, final Runnable task) {
    return spawn(replaying(() -> { delay(duration); spawn(task); }));
  }

  public static String defer(final Duration duration, final Context.TaskFactory<?> task) {
    return spawn(replaying(() -> { delay(duration); spawn(task); }));
  }

  public static <Input, Output> String defer(final Duration duration, final DirectiveTypeId<Input, Output> id, final Input activity, final Task<Output> task) {
    return spawn(replaying(() -> { delay(duration); spawn(id, activity, task); }));
  }

  public static String defer(final long quantity, final Duration unit, final Runnable task) {
    return spawn(replaying(() -> { delay(quantity, unit); spawn(task); }));
  }

  public static String defer(final long quantity, final Duration unit, final Context.TaskFactory<?> task) {
    return spawn(replaying(() -> { delay(quantity, unit); spawn(task); }));
  }

  public static <Input, Output> String defer(final long quantity, final Duration unit, final DirectiveTypeId<Input, Output> id, final Input activity, final Task<Output> task) {
    return spawn(replaying(() -> { delay(quantity, unit); spawn(id, activity, task); }));
  }


  public static void delay(final Duration duration) {
    context.get().delay(duration);
  }

  public static void delay(final long quantity, final Duration unit) {
    delay(unit.times(quantity));
  }

  public static void waitFor(final String id) {
    context.get().waitFor(id);
  }

  public static void waitUntil(final Condition condition) {
    context.get().waitUntil(condition);
  }
}
