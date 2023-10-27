package gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box;

import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ExpiringMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.math3.analysis.solvers.BrentSolver;
import org.apache.commons.math3.exception.NoBracketingException;
import org.apache.commons.math3.exception.NumberIsTooLargeException;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;

import java.util.function.Function;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.whenever;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.dynamicsChange;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear.linear;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;

/**
 * Utilities to build a secant approximation of a black-box {@link Differentiable} resource.
 */
public final class SecantApproximation {
  private SecantApproximation() {}

  /**
   * Approximate a resource using secants.
   * Intervals for the secants are given using an interval function.
   * Pre-built interval functions are available in {@link IntervalFunctions}.
   * For example:
   * <pre>
   * Resource&lt;BlackBox&lt;Double&gt;&gt; real;
   * Resource&lt;Linear&gt; approx = secantApproximation(real, byUniformSampling(Duration.HOUR));
   * </pre>
   */
  public static <D extends Dynamics<Double, D>> Resource<Linear> secantApproximation(Resource<D> resource, Function<Expiring<D>, Duration> secantInterval) {
    var result = cellResource(secantDynamics(resource, secantInterval));
    // Since result is cached, dynamicsChange(result) just detects expiry
    whenever(() -> dynamicsChange(resource).or(dynamicsChange(result)), () -> {
      var newDynamics = secantDynamics(resource, secantInterval);
      result.emit("Update secant approximation", $ -> newDynamics);
    });
    return result;
  }

  private static <D extends Dynamics<Double, ?>> ErrorCatching<Expiring<Linear>> secantDynamics(
      Resource<D> resource, Function<Expiring<D>, Duration> secantInterval) {
    return resource.getDynamics().map(d -> ExpiringMonad.map(d, d$ -> secant(d$, secantInterval.apply(d))));
  }

  private static Linear secant(Dynamics<Double, ?> d, Duration interval) {
    var s = d.extract();
    var e = d.step(interval).extract();
    return linear(s, (e - s) / interval.ratioOver(SECOND));
  }

  public static final class IntervalFunctions {
    private IntervalFunctions() {}

    /**
     * Secant approximation method using uniform point spacing.
     */
    public static Function<Expiring<?>, Duration> byUniformSampling(Duration samplePeriod) {
      return exp -> Duration.min(samplePeriod, exp.expiry().value().orElse(Duration.MAX_VALUE));
    }

    /**
     * Chooses secant intervals which attempt to bound the maximum error.
     * To ensure efficient convergence, a minimum and maximum size for the interval must be supplied.
     * Error is measured by the provided error estimate function.
     * Pre-built error functions are available in {@link ErrorEstimates}.
     * For example,
     * <pre>
     * Resource&lt;Differentiable&gt; real;
     * Resource&lt;Linear&gt; approx = secantApproximation(real,
     *     byBoundingError(1e-4, Duration.MINUTE, Duration.HOUR,
     *         relative(errorByQuadraticApproximation(), 1e-8)));
     * </pre>
     */
    public static <D extends Dynamics<Double, D>> Function<Expiring<D>, Duration> byBoundingError(
        double maximumError, Duration minimumSamplePeriod, Duration maximumSamplePeriod, Function<ErrorEstimateInput<D>, Double> errorEstimate) {
      if (maximumError <= 0) {
        throw new IllegalArgumentException("maximumError must be positive");
      }
      if (!minimumSamplePeriod.isPositive()) {
        throw new IllegalArgumentException("minimumSamplePeriod must be positive");
      }
      if (maximumSamplePeriod.shorterThan(minimumSamplePeriod)) {
        throw new IllegalArgumentException("maximumSamplePeriod must be at least minimumSamplePeriod");
      }

      return exp -> {
        var e = exp.expiry().value().orElse(Duration.MAX_VALUE);
        var solver = new BrentSolver();
        try {
          double intervalSize = solver.solve(
              100,
              t -> maximumError - errorEstimate.apply(new ErrorEstimateInput<>(exp.data(), t, maximumError)),
              Duration.min(e, minimumSamplePeriod).ratioOver(SECOND),
              Duration.min(e, maximumSamplePeriod).ratioOver(SECOND));
          return Duration.roundNearest(intervalSize, SECOND);
        } catch (NoBracketingException x) {
          return maximumSamplePeriod;
        } catch (TooManyEvaluationsException | NumberIsTooLargeException x) {
          return minimumSamplePeriod;
        }
      };
    }
  }

  public record ErrorEstimateInput<D>(D actualDynamics, Double intervalInSeconds, Double maximumError) {}

  public static final class ErrorEstimates {
    private ErrorEstimates() {}

    /**
     * Expands a second-order Taylor approximation at the midpoint of a proposed interval
     * to estimate the error of taking a secant across that interval.
     */
    public static Function<ErrorEstimateInput<Differentiable>, Double> errorByQuadraticApproximation() {
      return input -> {
        var d = input.actualDynamics();
        var t = input.intervalInSeconds();

        // Shift d to the interval midpoint
        var dMidpoint = d.step(Duration.roundNearest(t / 2, SECOND));
        var dPrimeMidpoint = dMidpoint.derivative();

        // Taylor expansion at the midpoint
        var midpointValue = dMidpoint.extract();
        var midpointSlope = dPrimeMidpoint.extract();
        var midpointCurvature = dPrimeMidpoint.derivative().extract();

        // Secant across the interval
        var secant = secant(d, Duration.roundNearest(t, SECOND));
        var secantStartValue = secant.extract();
        var secantSlope = secant.rate();
        var secantMidValue = secantStartValue + (secantSlope * t / 2);

        // Error is measured by Taylor expansion minus secant, which is a quadratic ax^2 + bx + c
        // Note that this curve is expressed with x = 0 at the interval midpoint
        var a = midpointCurvature / 2;
        var b = midpointSlope - secantSlope;
        var c = midpointValue - secantMidValue;

        // Find the location of the extremum:
        var extremePoint = -b / (2 * a);
        // If the extreme point is within the interval, consider the error there
        var extremumError = ((-t / 2) < extremePoint && extremePoint < (t / 2))
            ? Math.abs(c - (b * b / (4 * a))) : 0;
        // Also evaluate the error function at +/- (t/2), the interval start and end points.
        var startSecantError = Math.abs((a * t * t / 4) - (b * t / 2) + c);
        var endSecantError = Math.abs((a * t * t / 4) + (b * t / 2) + c);
        // Compute the maximum error between the Taylor expansion and the secant
        var secantError = Math.max(extremumError, Math.max(startSecantError, endSecantError));

        // Assuming that the Taylor expansion monotonically diverges from the true value,
        // we can bound the error between the Taylor expansion and the true value by measuring the endpoints.
        var startTaylorError = Math.abs((midpointCurvature * t * t / 8) - (midpointSlope * t / 2) + midpointValue);
        var endTaylorError = Math.abs((midpointCurvature * t * t / 8) + (midpointSlope * t / 2) + midpointValue);
        var taylorError = Math.max(startTaylorError, endTaylorError);

        // Finally, we can bound the error across the entire interval as
        // error between secant and Taylor expansion plus error between Taylor expansion and true value.
        return secantError + taylorError;
      };
    }

    /**
     * Uses a direct optimizer to numerically estimate the error of a secant interval.
     */
    public static <D extends Dynamics<Double, D>> Function<ErrorEstimateInput<D>, Double> errorByOptimization() {
      return input -> {
        var d = input.actualDynamics();
        var t = input.intervalInSeconds();
        var E = input.maximumError();

        var secant = secant(d, Duration.roundNearest(t, SECOND));
        var secantValue = secant.extract();
        var secantSlope = secant.rate();
        // We only need to compare error between secants, so the error calculation can be pretty coarse.
        var optimizer = new BrentOptimizer(1e-2, 1e-2 * E);
        try {
          return optimizer.optimize(
              GoalType.MAXIMIZE,
              new SearchInterval(0, t),
              new MaxEval(100),
              new UnivariateObjectiveFunction(
                  s -> Math.abs( (secantValue + secantSlope * s) - d.step(Duration.roundNearest(s, SECOND)).extract() )))
                          .getValue();
        } catch (TooManyEvaluationsException e) {
          // If we can't evaluate the error, play it safe by returning infinite error.
          return Double.POSITIVE_INFINITY;
        }
      };
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
    public static <D extends Dynamics<Double, D>> Function<ErrorEstimateInput<D>, Double> relative(
        Function<ErrorEstimateInput<D>, Double> absoluteErrorEstimate, double epsilon) {
      return input -> {
        var d = input.actualDynamics;
        var t = input.intervalInSeconds;
        var maxRelativeError = input.maximumError;
        var valueMagnitude = Math.abs(d.step(Duration.roundNearest(t / 2, SECOND)).extract());
        var maxAbsoluteError = maxRelativeError * (valueMagnitude + epsilon);
        return absoluteErrorEstimate.apply(new ErrorEstimateInput<>(d, t, maxAbsoluteError));
      };
    }
  }
}
