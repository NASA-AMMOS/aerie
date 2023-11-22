package gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.function.BiFunction;
import java.util.function.Function;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiring.expiring;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.whenever;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.wheneverUpdates;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.expires;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.updates;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.ExpiringMonad.bind;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Context.contextualized;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Tracing.trace;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.replaying;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.spawn;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;

/**
 * General framework for approximating resources.
 */
public final class Approximation {
  private Approximation() {}

  /**
   * Approximate a resource.
   * Result updates whenever resource updates or approximation expires.
   */
  public static <D extends Dynamics<?, D>, E extends Dynamics<?, E>> Resource<E> approximate(
      Resource<D> resource, Function<Expiring<D>, Expiring<E>> approximation) {
    var result = cellResource(resource.getDynamics().map(approximation));
    // Register the "updates" and "expires" conditions separately
    // so that the "updates" condition isn't triggered spuriously.
    wheneverUpdates(resource, newResourceDynamics -> updateApproximation(newResourceDynamics, approximation, result));
    whenever(expires(result), () -> updateApproximation(resource.getDynamics(), approximation, result));
    return result;
  }

  private static <D extends Dynamics<?, D>, E extends Dynamics<?, E>> void updateApproximation(
      ErrorCatching<Expiring<D>> resourceDynamics, Function<Expiring<D>, Expiring<E>> approximation, CellResource<E> result) {
    var newDynamics = resourceDynamics.map(approximation);
    result.emit("Update approximation to " + newDynamics, $ -> newDynamics);
  }

  /**
   * Build an approximation by first choosing an interval, then approximating over that interval.
   */
  public static <D, E> Function<Expiring<D>, Expiring<E>> intervalApproximation(
      BiFunction<Expiring<D>, Duration, Expiring<E>> intervalApproximation, Function<Expiring<D>, Duration> intervalSelector) {
    return d -> intervalApproximation.apply(d, intervalSelector.apply(d));
  }

  /**
   * Build an approximation by first choosing an approximating dynamics,
   * then estimating when that approximation diverges.
   */
  public static <D, E> Function<Expiring<D>, Expiring<E>> divergingApproximation(
      Function<D, E> baseApproximation, BiFunction<D, E, Duration> divergenceEstimator) {
    return d -> bind(d, d$ -> {
      var e$ = baseApproximation.apply(d$);
      return expiring(e$, divergenceEstimator.apply(d$, e$));
    });
  }

  /**
   * Interprets maximumError as a relative error, using the given absolute error estimate.
   * <p>
   *   Gets the function value at the interval midpoint to estimate a magnitude M,
   *   then computes absolute error as (relative error) * (M + epsilon).
   * </p>
   * <p>
   *   Epsilon should be close to the minimum value that is functionally different from zero for your application,
   *   to give enough accuracy everywhere without over-sampling near zero.
   * </p>
   */
  public static <D extends Dynamics<Double, D>> Function<IntervalFunctions.ErrorEstimateInput<D>, Double> relative(
      Function<IntervalFunctions.ErrorEstimateInput<D>, Double> absoluteErrorEstimate, double epsilon) {
    return input -> {
      var d = input.actualDynamics();
      var t = input.intervalInSeconds();
      var maxRelativeError = input.maximumError();
      var valueMagnitude = Math.abs(d.step(Duration.roundNearest(t / 2, SECOND)).extract());
      var maxAbsoluteError = maxRelativeError * (valueMagnitude + epsilon);
      return absoluteErrorEstimate.apply(new IntervalFunctions.ErrorEstimateInput<>(d, t, maxAbsoluteError)) / (valueMagnitude + epsilon);
    };
  }
}
