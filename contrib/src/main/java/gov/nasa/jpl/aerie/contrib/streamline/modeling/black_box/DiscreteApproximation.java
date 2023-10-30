package gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box;

import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ExpiringMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.IntervalFunctions.ErrorEstimateInput;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;

import java.util.function.BiFunction;
import java.util.function.Function;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiring.expiring;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.whenever;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.updates;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.roundNearest;

/**
 * Utilities to build discrete approximations of {@link Unstructured} resources.
 */
public final class DiscreteApproximation {
  private DiscreteApproximation() {}

  /**
   * Approximate a resource using secants.
   * Intervals for the secants are given using an interval function.
   * Pre-built interval functions are available in {@link IntervalFunctions}.
   */
  public static <V, D extends Dynamics<V, D>> Resource<Discrete<V>> discreteApproximation(
      Resource<D> resource, Function<Expiring<D>, Duration> sampleInterval) {
    var result = cellResource(discreteSample(resource, sampleInterval));
    // Since result is cached, updates(result) just detects expiry
    whenever(() -> updates(resource).or(updates(result)), () -> {
      var newDynamics = discreteSample(resource, sampleInterval);
      result.emit("Update secant approximation", $ -> newDynamics);
    });
    return result;
  }

  private static <T, D extends Dynamics<T, D>> ErrorCatching<Expiring<Discrete<T>>> discreteSample(
      Resource<D> resource, Function<Expiring<D>, Duration> sampleInterval) {
    return resource.getDynamics().map(d -> {
      var interval = sampleInterval.apply(d);
      return ExpiringMonad.bind(d, d$ -> expiring(discrete(d$.extract()), interval));
    });
  }

  public static final class ErrorEstimates {
    private ErrorEstimates() {}

    /**
     * Measures error only at the end of a discrete sample interval
     */
    public static <V, D extends Dynamics<V, D>> Function<ErrorEstimateInput<D>, Double> errorByEndpoint(BiFunction<V, V, Double> errorMetric) {
      return input -> {
        var d = input.actualDynamics();
        var t = input.intervalInSeconds();

        return errorMetric.apply(d.extract(), d.step(roundNearest(t, SECOND)).extract());
      };
    }

    /**
     * Uses a direct optimizer to numerically estimate the error of a discrete sample interval.
     */
    public static <V, D extends Dynamics<V, D>> Function<ErrorEstimateInput<D>, Double> errorByOptimization(BiFunction<V, V, Double> errorMetric) {
      return input -> {
        var d = input.actualDynamics();
        var t = input.intervalInSeconds();
        var E = input.maximumError();

        // We only need to compare error between secants, so the error calculation can be pretty coarse.
        var optimizer = new BrentOptimizer(1e-2, 1e-2 * E);
        try {
          var v = d.extract();
          return optimizer.optimize(
                              GoalType.MAXIMIZE,
                              new SearchInterval(0, t),
                              new MaxEval(100),
                              new UnivariateObjectiveFunction(
                                  s -> errorMetric.apply(v, d.step(Duration.roundNearest(s, SECOND)).extract())))
                          .getValue();
        } catch (TooManyEvaluationsException e) {
          // If we can't evaluate the error, play it safe by returning infinite error.
          return Double.POSITIVE_INFINITY;
        }
      };
    }
  }
}
