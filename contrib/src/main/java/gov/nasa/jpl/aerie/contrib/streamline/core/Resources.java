package gov.nasa.jpl.aerie.contrib.streamline.core;

import gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.Clock;
import gov.nasa.jpl.aerie.merlin.framework.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.List;
import java.util.Optional;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.staticallyCreated;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.Clock.clock;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;

/**
 * Utility methods for {@link Resource}s.
 */
public final class Resources {
  private Resources() {}

  /**
   * Ensure that Resources are initialized.
   *
   * <p>
   *   This method needs to be called during simulation initialization.
   *   This method is idempotent; calling it multiple times is the same as calling it once.
   * </p>
   */
  public static void init() {
    currentTime();
  }

  private static final Resource<Clock> CLOCK = staticallyCreated(() -> cellResource(clock(ZERO)));
  public static Duration currentTime() {
    return currentValue(CLOCK);
  }

  public static <D> D currentData(Resource<D> resource) {
    return resource.getDynamics().getOrThrow().data();
  }

  public static <D> D currentData(Resource<D> resource, D dynamicsIfError) {
    return resource.getDynamics().match(Expiring::data, error -> dynamicsIfError);
  }

  public static <V, D extends Dynamics<V, D>> V currentValue(Resource<D> resource) {
    return currentData(resource).extract();
  }

  public static <V, D extends Dynamics<V, D>> V currentValue(Resource<D> resource, V valueIfError) {
    return resource.getDynamics().match(result -> result.data().extract(), error -> valueIfError);
  }

  public static <D extends Dynamics<?, D>> Condition dynamicsChange(Resource<D> resource) {
    final var startingDynamics = resource.getDynamics();
    final Duration startTime = currentTime();
    return (positive, atEarliest, atLatest) -> {
      var currentDynamics = resource.getDynamics();
      boolean haveChanged = startingDynamics.match(
          start -> currentDynamics.match(
              current -> !current.data().equals(start.data().step(currentTime().minus(startTime))),
              ignored -> true),
          startException -> currentDynamics.match(
              ignored -> true,
              // Use semantic comparison for exceptions, since derivation can generate the exception each invocation.
              currentException -> !equivalentExceptions(startException, currentException)));

      return positive == haveChanged
          ? Optional.of(atEarliest)
          : positive
            ? currentDynamics.match(
                expiring -> expiring.expiry().value().filter(atLatest::noShorterThan),
                exception -> Optional.empty())
            : Optional.empty();
    };
  }

  // TODO: Should this be moved somewhere else?
  /**
   * Tests if two exceptions are equivalent from the point of view of resource values.
   * Two exceptions are equivalent if they have the same type and message.
   */
  public static boolean equivalentExceptions(Throwable startException, Throwable currentException) {
    return startException.getClass().equals(currentException.getClass())
           && startException.getMessage().equals(currentException.getMessage());
  }

  public static <D extends Dynamics<?, D>> Condition dynamicsChange(List<Resource<D>> resources) {
    assert resources.size() > 0;
    var result = dynamicsChange(resources.get(0));
    for (Resource<D> r : resources) {
      result = result.or(dynamicsChange(r));
    }
    return result;
  }
}
