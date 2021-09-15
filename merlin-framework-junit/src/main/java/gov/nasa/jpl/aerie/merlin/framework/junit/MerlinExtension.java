package gov.nasa.jpl.aerie.merlin.framework.junit;

import gov.nasa.jpl.aerie.merlin.driver.Adaptation;
import gov.nasa.jpl.aerie.merlin.driver.AdaptationBuilder;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.framework.InitializationContext;
import gov.nasa.jpl.aerie.merlin.framework.ModelActions;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.protocol.types.Phantom;
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
import java.util.Map;

public final class MerlinExtension<Model> implements ParameterResolver, InvocationInterceptor, TestInstancePreDestroyCallback {

  private static final class State<Model> {
    public MerlinTestContext<Model> context = null;
    public AdaptationBuilder<?> builder = null;
    public Adaptation<?, ?> adaptation = null;
  }

  private State<Model> getState(final ExtensionContext context) {
    // SAFETY: This method is the only one where we store or retrieve a State,
    //   and it's always instantiated with <Model>.
    @SuppressWarnings("unchecked")
    final var stateClass = (Class<State<Model>>) (Object) State.class;

    // This may not work right if tests are run in parallel with Lifecycle.PER_METHOD set.
    // Each test method will cause construction of an independent instance of its containing class,
    // and when the extension operates on each instance, it will reference the *same* store,
    // since the defined namespace is given by the class, not the instance.
    //
    // This is hard to avoid, since an instance doesn't exist before construction,
    // and we need access to the store to be able to inject a unique Registrar into the constructor.
    return context
        .getStore(ExtensionContext.Namespace.create(context.getRequiredTestClass()))
        .getOrComputeIfAbsent("state", $ -> new State<>(), stateClass);
  }


  @Override
  public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
  throws ParameterResolutionException
  {
    return parameterContext.getParameter().getType().equals(MerlinTestContext.class);
  }

  @Override
  public MerlinTestContext<Model> resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
  throws ParameterResolutionException
  {
    final var state = this.getState(extensionContext);
    final var paramType = parameterContext.getParameter().getType();

    if (!paramType.equals(MerlinTestContext.class)) {
      throw new Error("Unsupported Merlin extension parameter type: " + paramType.getSimpleName());
    }

    if (state.builder == null) state.builder = new AdaptationBuilder<>(Schema.builder());
    return state.context = new MerlinTestContext<>(new Registrar(state.builder));
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

    if (state.context == null) {
      state.adaptation = state.builder.build(new Phantom<>(new Object()), Map.of());
    } else {
      state.adaptation = state.builder.build(new Phantom<>(state.context.model()), state.context.activityTypes());
    }
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
  void simulate(final Adaptation<$Schema, ?> adaptation, final Invocation<Void> invocation) throws Throwable {
    simulate(adaptation, SimulationTimeline.create(adaptation.getSchema()), invocation);
  }

  private static <$Schema, $Timeline extends $Schema, Model>
  void simulate(
      final Adaptation<$Schema, Model> adaptation,
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
    } catch (final WrappedException ex) {
      throw ex.wrapped;
    }

    if (!completed.value) {
      throw new AssertionError("test did not complete");
    }
  }

  /** An exception for tunneling checked exceptions through an interface that expects no exceptions. */
  private static final class WrappedException extends Error {
    public final Throwable wrapped;

    public WrappedException(final Throwable ex) {
      super(null, ex, /* capture suppressed exceptions? */ false, /* capture stack trace? */ false);
      this.wrapped = ex;
    }
  }
}
