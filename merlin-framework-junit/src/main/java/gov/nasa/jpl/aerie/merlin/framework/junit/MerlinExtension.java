package gov.nasa.jpl.aerie.merlin.framework.junit;

import gov.nasa.jpl.aerie.merlin.driver.DirectiveTypeRegistry;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelBuilder;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.framework.EmptyConfigurationType;
import gov.nasa.jpl.aerie.merlin.framework.InitializationContext;
import gov.nasa.jpl.aerie.merlin.framework.ModelActions;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.RootModel;
import gov.nasa.jpl.aerie.merlin.framework.Scoped;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.DynamicTestInvocationContext;
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
import java.util.Objects;

public final class MerlinExtension<UNUSED, Model>
    implements BeforeAllCallback, ParameterResolver, InvocationInterceptor, TestInstancePreDestroyCallback
{
  private State<UNUSED, Model> getState(final ExtensionContext context) {
    // SAFETY: This method is the only one where we store or retrieve a State,
    //   and it's always instantiated with <Model>.
    @SuppressWarnings("unchecked")
    final var stateClass = (Class<State<UNUSED, Model>>) (Object) State.class;

    return context
        .getStore(ExtensionContext.Namespace.create(context.getRequiredTestClass()))
        .getOrComputeIfAbsent("state", $ -> new State<>(new MissionModelBuilder()), stateClass);
  }

  @Override
  public void beforeAll(final ExtensionContext context) {
    final var lifecycle = context.getTestInstanceLifecycle().orElse(TestInstance.Lifecycle.PER_METHOD);
    if (lifecycle != TestInstance.Lifecycle.PER_CLASS) {
      throw new IllegalTestLifetimeException(context.getRequiredTestClass());
    }
  }

  @Override
  public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
  throws ParameterResolutionException
  {
    return parameterContext.getParameter().getType().equals(MerlinTestContext.class);
  }

  @Override
  public MerlinTestContext<UNUSED, Model> resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
  throws ParameterResolutionException
  {
    final var state = this.getState(extensionContext);
    final var paramType = parameterContext.getParameter().getType();

    if (!paramType.equals(MerlinTestContext.class)) {
      throw new Error("Unsupported Merlin extension parameter type: " + paramType.getSimpleName());
    }

    state.context = new MerlinTestContext<>(new Registrar(state.builder));

    return state.context;
  }

  @Override
  public <T> T interceptTestClassConstructor(
      final Invocation<T> invocation,
      final ReflectiveInvocationContext<Constructor<T>> invocationContext,
      final ExtensionContext extensionContext) throws Throwable
  {
    return this.getState(extensionContext).constructModel(invocation);
  }

  @Override
  public void interceptTestTemplateMethod(
      final Invocation<Void> invocation,
      final ReflectiveInvocationContext<Method> invocationContext,
      final ExtensionContext extensionContext) throws Throwable
  {
    this.getState(extensionContext).simulate(invocation);
  }

  @Override
  public void interceptTestMethod(
      final Invocation<Void> invocation,
      final ReflectiveInvocationContext<Method> invocationContext,
      final ExtensionContext extensionContext
  ) throws Throwable
  {
    this.getState(extensionContext).simulate(invocation);
  }

  @Override
  public void interceptDynamicTest(final Invocation<Void> invocation, final DynamicTestInvocationContext invocationContext, final ExtensionContext extensionContext)
  throws Throwable
  {
    this.getState(extensionContext).simulate(invocation);
  }

  @Override
  public void preDestroyTestInstance(final ExtensionContext extensionContext) {
    final var state = this.getState(extensionContext);

    state.missionModel.getModel().close();
    state.missionModel = null;
  }

  private static final class State<UNUSED, Model> {
    public MissionModelBuilder builder;
    public MerlinTestContext<UNUSED, Model> context;

    public MissionModel<RootModel<Model>> missionModel = null;
    public Scoped<RootModel<Model>> scoping = null;

    public State(final MissionModelBuilder builder) {
      this.builder = Objects.requireNonNull(builder);
      this.context = new MerlinTestContext<>(new Registrar(this.builder));
    }

    public <T> T constructModel(final Invocation<T> invocation) throws Throwable {
      final var executor = RootModel.makeExecutorService();

      final T value;
      try {
        value = InitializationContext.initializing(executor, this.builder, () -> {
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

      this.scoping = this.context.scoping();

      this.missionModel = this.builder.build(
          new RootModel<>(this.context.model(), executor),
          new EmptyConfigurationType(),
          new DirectiveTypeRegistry<>(Map.of()));

      // Clear the builder; it shouldn't be used from here on, and if it is, an error should be raised.
      this.builder = null;
      this.context = null;

      return value;
    }

    private void simulate(final Invocation<Void> invocation) throws Throwable {
      final var completed = new Object() { boolean value = false; };

      final var model = this.missionModel.getModel();
      final var task = ModelActions
          .threaded(() -> {
            try (final var token = this.scoping.set(model)) {
              invocation.proceed();
            } catch (final Throwable ex) {
              throw new WrappedException(ex);
            } finally {
              completed.value = true;
            }
          })
          .create(model.executor());

      try {
        SimulationDriver.simulateTask(this.missionModel, task);
      } catch (final WrappedException ex) {
        throw ex.wrapped;
      }

      if (!completed.value) {
        throw new AssertionError("test did not complete");
      }
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

  private static final class IllegalTestLifetimeException extends RuntimeException {
    public IllegalTestLifetimeException(final Class<?> offendingClass) {
      super("%s expects %s to be annotated with @TestInstance(Lifecycle.PER_CLASS)"
                .formatted(MerlinExtension.class.getSimpleName(), offendingClass));
    }
  }
}
