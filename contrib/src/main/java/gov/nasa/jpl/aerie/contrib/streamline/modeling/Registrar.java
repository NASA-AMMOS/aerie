package gov.nasa.jpl.aerie.contrib.streamline.modeling;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.DummyValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.IntegerValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.NullableValueMapper;
import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resources;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ThinResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.debugging.Logging;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.whenever;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentData;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Logging.LOGGER;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Naming.*;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Profiling.profile;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Tracing.trace;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar.ErrorBehavior.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.increment;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear.linear;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.waitUntil;

/**
 * Wrapper for {@link gov.nasa.jpl.aerie.merlin.framework.Registrar} specialized for {@link Resource}.
 *
 * <p>
 *     Automatically creates and populates "errors" and "numberOfErrors" resources, if {@link ErrorBehavior#Log} is used.
 * </p>
 */
public class Registrar {
  private final gov.nasa.jpl.aerie.merlin.framework.Registrar baseRegistrar;
  private boolean trace = false;
  private boolean profile = false;
  private final ErrorBehavior errorBehavior;
  private final MutableResource<Discrete<Integer>> numberOfErrors = discreteResource(0);

  public enum ErrorBehavior {
    /**
     * Log errors to {@link Logging#LOGGER}
     * and replace resource value with null.
     */
    Log,
    /**
     * Throw errors, crashing the simulation immediately.
     */
    Throw
  }

  public Registrar(final gov.nasa.jpl.aerie.merlin.framework.Registrar baseRegistrar, final ErrorBehavior errorBehavior) {
    Resources.init();
    Logging.init(baseRegistrar);
    this.baseRegistrar = baseRegistrar;
    this.errorBehavior = errorBehavior;

    discrete("numberOfErrors", numberOfErrors, new IntegerValueMapper());
  }

  public void setTrace() {
    trace = true;
  }

  public void clearTrace() {
    trace = false;
  }

  public void setProfile() {
    profile = true;
  }

  public void clearProfile() {
    profile = false;
  }

  public <Value> void discrete(final String name, final Resource<Discrete<Value>> resource, final ValueMapper<Value> mapper) {
    name(resource, name);
    var debugResource = debug(name, resource);
    gov.nasa.jpl.aerie.merlin.framework.Resource<Value> registeredResource = switch (errorBehavior) {
      case Log -> () -> currentValue(debugResource, null);
      case Throw -> wrapErrors(name, () -> currentValue(debugResource));
    };
    baseRegistrar.discrete(name, registeredResource, new NullableValueMapper<>(mapper));
    if (errorBehavior.equals(Log)) logErrors(name, debugResource);
  }

  public void real(final String name, final Resource<Linear> resource) {
    name(resource, name);
    var debugResource = debug(name, resource);
    gov.nasa.jpl.aerie.merlin.framework.Resource<RealDynamics> registeredResource = switch (errorBehavior) {
      case Log -> () -> realDynamics(currentData(debugResource, linear(0, 0)));
      case Throw -> wrapErrors(name, () -> realDynamics(currentData(debugResource)));
    };
    baseRegistrar.real(name, registeredResource);
    if (errorBehavior.equals(Log)) logErrors(name, debugResource);
  }

  private static RealDynamics realDynamics(Linear linear) {
    return RealDynamics.linear(linear.extract(), linear.rate());
  }

  private <D> Resource<D> debug(String name, Resource<D> resource) {
    var tracedResource = trace ? trace(resource) : resource;
    return profile ? profile(tracedResource) : tracedResource;
  }

  private <D extends Dynamics<?, D>> void logErrors(String name, Resource<D> resource) {
    // TODO: Is there any way to avoid computing resources twice, once for sampling and separately for errors?
    Resource<Discrete<Boolean>> hasError = ThinResourceMonad.bind(resource, ec -> DiscreteResourceMonad.pure(ec.match(value -> false, error -> true)))::getDynamics;
    whenever(hasError, () -> {
      resource.getDynamics().match($ -> null, e -> logError(name, e));
      // Avoid infinite loops by waiting for resource to clear before logging a new error.
      // TODO: This means we won't log if a resource changes from error1 to error2 without clearing in between.
      //   Maybe we should implement a condition like "is not the current error"?
      waitUntil(when(not(hasError)));
    });
  }

  private Unit logError(String resourceName, Throwable e) {
    LOGGER.error("Error affecting %s: %s", resourceName, e);
    increment(numberOfErrors);
    return Unit.UNIT;
  }

  /**
   * Include the resource name in the error to give context
   */
  private static <D> gov.nasa.jpl.aerie.merlin.framework.Resource<D> wrapErrors(String resourceName, gov.nasa.jpl.aerie.merlin.framework.Resource<D> resource) {
    return () -> {
      try {
        return resource.getDynamics();
      } catch (Throwable e) {
        throw new RuntimeException("Error affecting " + resourceName, e);
      }
    };
  }
}
