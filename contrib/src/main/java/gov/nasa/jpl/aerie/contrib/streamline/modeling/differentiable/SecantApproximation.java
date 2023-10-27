package gov.nasa.jpl.aerie.contrib.streamline.modeling.differentiable;

import gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.math3.analysis.solvers.BrentSolver;
import org.apache.commons.math3.exception.NoBracketingException;
import org.apache.commons.math3.exception.NumberIsTooLargeException;
import org.apache.commons.math3.exception.TooManyEvaluationsException;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching.success;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiring.expiring;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.every;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad.bind;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad.map;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear.linear;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;

/**
 * Utilities to build a secant approximation of a black-box {@link Differentiable} resource.
 */
public final class SecantApproximation {
  private SecantApproximation() {}

  /**
   * Creates a secant approximation by taking uniformly-spaced sample points
   * and interpolating linearly between them.
   */
  public static Resource<Linear> byUniformSampling(Resource<Differentiable> resource, Duration samplePeriod) {
    var result = cellResource(uniformSecant(resource, samplePeriod));
    every(samplePeriod, () -> {
      var newDynamics = uniformSecant(resource, samplePeriod);
      result.emit("Update secant approximation", $ -> newDynamics);
    });
    return result;
  }

  private static ErrorCatching<Expiring<Linear>> uniformSecant(Resource<Differentiable> resource, Duration samplePeriod) {
    return map(resource.getDynamics(), d -> secant(d, samplePeriod));
  }

  private static Linear secant(Differentiable d, Duration interval) {
    var s = d.extract();
    var e = d.step(interval).extract();
    return linear(s, (e - s) / interval.ratioOver(SECOND));
  }

  /**
   * Creates a secant approximation which attempts to bound the maximum absolute error.
   * <p>
   *   Assumes that the second-order Taylor expansion diverges monotonically from the true value;
   *   If f is the exact function and T_n is the n-th order Taylor expansion, assumes that
   *   <pre>
   *     0 &lt; a &lt; b  =&gt;  |T_2(a) - f(a)| &lt;= |T_2(b) - f(b)|
   *   </pre>
   * </p>
   * <p>
   *   This method takes a minimum and maximum sample period to guarantee efficient convergence.
   * </p>
   */
  public static Resource<Linear> byBoundedAbsoluteError(Resource<Differentiable> resource, double maximumAbsoluteError, Duration minimumSamplePeriod, Duration maximumSamplePeriod) {
    var result = cellResource(boundedAbsoluteErrorSecant(
        resource, maximumAbsoluteError, minimumSamplePeriod, maximumSamplePeriod));
    // REVIEW: This uses maximumSamplePeriod if anything goes wrong, for efficiency.
    //   minimumSamplePeriod would potentially "recover" faster, though.
    every(() -> result.getDynamics().match(
        s -> s.expiry().value().orElse(maximumSamplePeriod), e -> maximumSamplePeriod),
          () -> {
            var newDynamics = boundedAbsoluteErrorSecant(
                resource, maximumAbsoluteError, minimumSamplePeriod, maximumSamplePeriod);
            result.emit("Update secant approximation", $ -> newDynamics);
          });
    return result;
  }

  private static ErrorCatching<Expiring<Linear>> boundedAbsoluteErrorSecant(
      Resource<Differentiable> resource,
      double maximumAbsoluteError,
      Duration minimumSamplePeriod,
      Duration maximumSamplePeriod)
  {
    return bind(resource.getDynamics(), d -> {
      var interval = boundedAbsoluteErrorInterval(d, maximumAbsoluteError, minimumSamplePeriod, maximumSamplePeriod);
      return success(expiring(secant(d, interval), interval));
    });
  }

  private static Duration boundedAbsoluteErrorInterval(Differentiable d, double maximumAbsoluteError, Duration minimumSamplePeriod, Duration maximumSamplePeriod) {
    var solver = new BrentSolver();
    // REVIEW: This assumes that computing derivatives is usually more efficient that running an optimizer
    //   to estimate the error of the proposed secant. Need to measure whether that's actually true.
    double value = d.extract();
    var dPrime = d.derivative();
    double slope = dPrime.extract();
    double curvature = dPrime.derivative().extract();
    try {
      double intervalSize = solver.solve(
          100,
          t -> {
            Duration end = Duration.roundNearest(t, SECOND);
            Linear secant = secant(d, end);
            double b = secant.extract();
            double m = secant.rate();

            double extremePoint = (m - slope) / curvature;
            double extremeValue = (0 < extremePoint && extremePoint < t) ?
                value - b - ((slope - m) * (slope - m) / (2 * curvature)) :
                value + (slope * t) + (curvature * t * t / 2);
            return Math.abs(extremeValue - maximumAbsoluteError);
          },
          0,
          maximumSamplePeriod.ratioOver(SECOND));
      return Duration.roundNearest(intervalSize, SECOND);
    } catch (NoBracketingException e) {
      return maximumSamplePeriod;
    } catch (TooManyEvaluationsException | NumberIsTooLargeException e) {
      return minimumSamplePeriod;
    }
  }
}
