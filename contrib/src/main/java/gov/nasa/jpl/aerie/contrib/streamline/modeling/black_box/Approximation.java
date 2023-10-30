package gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box;

import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ExpiringMonad;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.function.BiFunction;
import java.util.function.Function;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiring.expiring;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.whenever;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.updates;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.ExpiringMonad.bind;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;

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
    whenever(() -> updates(resource).or(updates(result)), () -> {
      var newDynamics = resource.getDynamics().map(approximation);
      result.emit("Update approximation to " + newDynamics, $ -> newDynamics);
    });
    return result;
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
