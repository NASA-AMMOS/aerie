package gov.nasa.jpl.aerie.merlin.framework.junit;

import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.framework.AdaptationBuilder;
import gov.nasa.jpl.aerie.merlin.framework.BuiltAdaptation;
import gov.nasa.jpl.aerie.merlin.framework.InitializationContext;
import gov.nasa.jpl.aerie.merlin.framework.ModelActions;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.ThreadedTask;
import gov.nasa.jpl.aerie.merlin.timeline.Schema;
import gov.nasa.jpl.aerie.merlin.timeline.SimulationTimeline;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestInstancePreDestroyCallback;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class MerlinExtension implements ParameterResolver, InvocationInterceptor, TestInstancePreDestroyCallback {
  private static final class State {
    public AdaptationBuilder<?> builder = null;
    public BuiltAdaptation<?> adaptation = null;
  }

  private State getState(final ExtensionContext context) {
    // This may not work right if tests are run in parallel with Lifecycle.PER_METHOD set.
    // Each test method will cause construction of an independent instance of its containing class,
    // and when the extension operates on each instance, it will reference the *same* store,
    // since the defined namespace is given by the class, not the instance.
    //
    // This is hard to avoid, since an instance doesn't exist before construction,
    // and we need access to the store to be able to inject a unique Registrar into the constructor.
    return context
        .getStore(ExtensionContext.Namespace.create(context.getRequiredTestClass()))
        .getOrComputeIfAbsent("state", $ -> new State(), State.class);
  }


  @Override
  public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
  throws ParameterResolutionException
  {
    return parameterContext.getParameter().getType().equals(Registrar.class);
  }

  @Override
  public Registrar resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
  throws ParameterResolutionException
  {
    final var state = this.getState(extensionContext);

    if (state.builder == null) state.builder = new AdaptationBuilder<>(Schema.builder());

    return new Registrar(state.builder);
  }

  @Override
  public <T> T interceptTestClassConstructor(
      final Invocation<T> invocation,
      final ReflectiveInvocationContext<Constructor<T>> invocationContext,
      final ExtensionContext extensionContext) throws Throwable
  {
    final var state = this.getState(extensionContext);

    if (state.builder == null) state.builder = new AdaptationBuilder<>(Schema.builder());

    final T value;
    try {
      value = InitializationContext.initializing(state.builder, () -> {
        try {
          return invocation.proceed();
        } catch (final RuntimeException ex) {
          throw ex;
        } catch (final Throwable ex) {
          throw new WrappedException(ex);
        }
      });
    } catch (final WrappedException ex) {
      throw ex.wrapped;
    }

    state.adaptation = state.builder.build();
    state.builder = null;

    return value;
  }

  @Override
  public void interceptTestTemplateMethod(
      final Invocation<Void> invocation,
      final ReflectiveInvocationContext<Method> invocationContext,
      final ExtensionContext extensionContext) throws Throwable
  {
    this.interceptDynamicTest(invocation, extensionContext);
  }

  @Override
  public void interceptTestMethod(
      final Invocation<Void> invocation,
      final ReflectiveInvocationContext<Method> invocationContext,
      final ExtensionContext extensionContext
  ) throws Throwable
  {
    this.interceptDynamicTest(invocation, extensionContext);
  }

  @Override
  public void interceptDynamicTest(final Invocation<Void> invocation, final ExtensionContext extensionContext)
  throws Throwable
  {
    final var state = this.getState(extensionContext);

    simulate(state.adaptation, invocation);
  }

  @Override
  public void preDestroyTestInstance(final ExtensionContext extensionContext) {
    final var state = this.getState(extensionContext);

    state.adaptation = null;
  }


  private static <$Schema>
  void simulate(final BuiltAdaptation<$Schema> adaptation, final Invocation<Void> invocation) throws Throwable {
    simulate(adaptation, SimulationTimeline.create(adaptation.getSchema()), invocation);
  }

  private static <$Schema, $Timeline extends $Schema>
  void simulate(
      final BuiltAdaptation<$Schema> adaptation,
      final SimulationTimeline<$Timeline> timeline,
      final Invocation<Void> invocation)
  throws Throwable
  {
    final var completed = new Object() { boolean value = false; };

    final var task = ModelActions
        .threaded(() -> {
          try {
            invocation.proceed();
          } catch (final Throwable ex) {
            throw new WrappedException(ex);
          } finally {
            completed.value = true;
          }
        })
        .<$Timeline>create();

    try {
      SimulationDriver.simulateTask(adaptation, timeline, task);
    } catch (final ThreadedTask.TaskFailureException ex) {
      if (ex.cause instanceof WrappedException) {
        throw ((WrappedException) ex.cause).wrapped;
      } else {
        throw ex.cause;
      }
    }

    if (!completed.value) {
      throw new AssertionError("test did not complete");
    }
  }

  private static final class WrappedException extends RuntimeException {
    public final Throwable wrapped;

    public WrappedException(final Throwable ex) {
      super(ex);
      this.wrapped = ex;
    }
  }
}
