package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.BrentSolver;
import org.apache.commons.math3.exception.NoBracketingException;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EquationSolvingAlgorithms {

  private static final Logger logger = LoggerFactory.getLogger(EquationSolvingAlgorithms.class);

  public record History<T>(List<Pair<T, T>> history) {}

  public record RootFindingResult<T>(T x, T fx, History<T> history) {}

  public interface BracketingAlgorithm<T> {
    /**
     * Solves f(x) in [y - toleranceYLow, y + toleranceYHigh] for x in [xLow, xHigh].
     * the sign of f(bracket1) must be different from the sign of f(bracket2)
     *
     * xLow {@literal <} xHigh
     * toleranceYLow > 0, toleranceYHigh > 0
     */
    RootFindingResult<T> findRoot(
        Function<T> f,
        T bracket1,
        T bracket2,
        T y,
        T toleranceYLow,
        T toleranceYHigh,
        T xLow,
        T xHigh,
        int maxNbIterations)
        throws ExceededMaxIterationException, WrongBracketingException, NoSolutionException;
  }
  /**
   * Solves f(x) = y for x in [xLow, xHigh] with confidence interval [yLow, yHigh] around y such that we stop when
   * value y is in [y-yLow, y+yHigh].
   * x0 and x1 are initial guesses for x, they must be close to the solution to avoid diverging
   * It is considered that algorithm is diverging when the iterated value of x goes out of [xLow, xHigh].
   */
  public interface SecantAlgorithm<T> {
    RootFindingResult<T> findRoot(
        Function<T> f,
        T x0,
        T x1,
        T y,
        T toleranceYLow,
        T toleranceYHigh,
        T xLow,
        T xHigh,
        int maxNbIterations)
        throws ZeroDerivativeException,
            InfiniteDerivativeException,
            DivergenceException,
            ExceededMaxIterationException,
            NoSolutionException;
  }

  /**
   * Solves f(x) in [y - yLow, y + yHigh] for x in [xLow, xHigh] with confidence interval [yLow, yHigh] around y such that we stop when
   * value y is in [y - yLow, y + yHigh].
   * x0 is a first guess for x, it must be close to the solution to avoid diverging.
   * It is considered that algorithm is diverging when the iterated value of x goes out of [xLow, xHigh].
   */
  public interface NewtonAlgorithm<T> {
    RootFindingResult<T> findRoot(
        FunctionWithDerivative<T> f,
        T x0,
        T y,
        T toleranceYLow,
        T toleranceYHigh,
        T xLow,
        T xHigh,
        int maxNbIterations)
        throws DivergenceException, ZeroDerivativeException, ExceededMaxIterationException;
  }

  public interface Function<T> {
    T valueAt(T x);

    boolean isApproximation();
  }

  public interface FunctionWithDerivative<T> extends Function<T> {
    T derivativeAt(T x);
  }

  public static class ZeroDerivativeException extends Exception {
    public ZeroDerivativeException() {}
  }

  public static class InfiniteDerivativeException extends Exception {
    public InfiniteDerivativeException() {}
  }

  public static class DivergenceException extends Exception {
    public History<?> history;

    public DivergenceException(String errorMessage) {
      super(errorMessage);
    }

    public DivergenceException(String errorMessage, History<?> history) {
      super(errorMessage);
      this.history = history;
    }
  }

  public static class WrongBracketingException extends Exception {
    public WrongBracketingException(String errorMessage) {
      super(errorMessage);
    }
  }

  public static class ExceededMaxIterationException extends Exception {
    public History<?> history;

    public ExceededMaxIterationException() {
      super();
    }

    public ExceededMaxIterationException(History<?> history) {
      super();
      this.history = history;
    }
  }

  public static class NoSolutionException extends Exception {
    public NoSolutionException() {
      super();
    }

    public NoSolutionException(String errorMessage) {
      super(errorMessage);
    }
  }

  static final class BrentDoubleAlgorithm implements BracketingAlgorithm<Double> {
    public RootFindingResult<Double> findRoot(
        Function<Double> f,
        Double bracket1,
        Double bracket2,
        Double y,
        Double toleranceYLow,
        Double toleranceYHigh,
        Double xLow,
        Double xHigh,
        int maxNbIterations)
        throws ExceededMaxIterationException, WrongBracketingException, NoSolutionException {
      if (bracket1 < xLow || bracket2 > xHigh) {
        throw new WrongBracketingException("brackets are out of prescribed root domain");
      }
      if (Math.abs(toleranceYLow - toleranceYHigh) > 1E-6) {
        throw new NoSolutionException(
            "To use Brent's method, you must provide equal lower and upper confidence bounds.");
      }
      BrentSolver solver = new BrentSolver(toleranceYHigh);
      // the brent implementation solves f(x) = 0 so we provide lf(x) = f(x) - y
      var lf =
          new UnivariateFunction() {
            @Override
            public double value(final double v) {
              return f.valueAt(v) - y;
            }
          };
      try {
        final var root = solver.solve(maxNbIterations, lf, bracket1, bracket2);
        return new RootFindingResult<>(root, f.valueAt(root) + y, null);
      } catch (TooManyEvaluationsException e) {
        throw new ExceededMaxIterationException();
      } catch (NoBracketingException e) {
        throw new WrongBracketingException(e.getMessage());
      }
    }
  }

  public static final class BisectionDoubleAlgorithm implements BracketingAlgorithm<Double> {

    public RootFindingResult<Double> findRoot(
        Function<Double> f,
        Double bracket1,
        Double bracket2,
        Double y,
        Double toleranceYLow,
        Double toleranceYHigh,
        Double xLow,
        Double xHigh,
        int maxNbIterations)
        throws WrongBracketingException, ExceededMaxIterationException {
      if (bracket1 < xLow || bracket1 > xHigh || bracket2 < xLow || bracket2 > xHigh) {
        throw new WrongBracketingException("Brackets are out of prescribed root domain");
      }
      final var ff =
          new Function<Double>() {
            @Override
            public Double valueAt(final Double x) {
              return f.valueAt(x) - y;
            }

            @Override
            public boolean isApproximation() {
              return f.isApproximation();
            }
          };

      double fx_m = ff.valueAt(bracket1);
      double fx_p = ff.valueAt(bracket2);
      if (fx_m * fx_p > 0) {
        throw new WrongBracketingException(
            "f(x0)=" + fx_m + " and f(x1)=" + fx_p + " are of the same sign");
      }
      for (int nbIt = 0; nbIt < maxNbIterations; nbIt++) {
        final double xg = (bracket1 + bracket2) / 2.;
        final var fxg = ff.valueAt(xg);
        if (fxg >= -toleranceYLow && fxg <= toleranceYHigh) {
          return new RootFindingResult<>(xg, fxg + y, null);
        } else {
          if (fxg > 0 && fx_m > 0 || fxg < 0 && fx_m < 0) {
            bracket1 = xg;
          } else {
            bracket2 = xg;
          }
          fx_m = ff.valueAt(bracket1);
        }
      }
      throw new ExceededMaxIterationException();
    }
  }

  public static final class BisectionDurationAlgorithm implements BracketingAlgorithm<Duration> {

    public RootFindingResult<Duration> findRoot(
        Function<Duration> f,
        Duration bracket1,
        Duration bracket2,
        Duration y,
        Duration toleranceYLow,
        Duration toleranceYHigh,
        Duration xLow,
        Duration xHigh,
        int maxNbIterations)
        throws WrongBracketingException, ExceededMaxIterationException {
      if (bracket1.shorterThan(xLow)
          || bracket1.longerThan(xHigh)
          || bracket2.shorterThan(xLow)
          || bracket2.longerThan(xHigh)) {
        throw new WrongBracketingException("Brackets are out of prescribed root domain");
      }
      final var ff =
          new Function<Duration>() {
            @Override
            public Duration valueAt(final Duration x) {
              return f.valueAt(x).minus(y);
            }

            @Override
            public boolean isApproximation() {
              return f.isApproximation();
            }
          };
      var bracket1Long = bracket1.in(Duration.MICROSECONDS);
      var bracket2Long = bracket2.in(Duration.MICROSECONDS);
      var ff_bracket1 = ff.valueAt(bracket1);
      var ff_bracket2 = ff.valueAt(bracket2);
      if (ff_bracket1.in(Duration.MICROSECONDS) * ff_bracket2.in(Duration.MICROSECONDS) > 0) {
        throw new WrongBracketingException("f(bracket1) and f(bracket2) are of the same sign");
      }
      for (int nbIt = 0; nbIt < maxNbIterations; nbIt++) {
        long xg = Math.round(((float) bracket1Long + bracket2Long) / 2);
        final var ff_xg = ff.valueAt(Duration.of(xg, Duration.MICROSECONDS));
        if (ff_xg.noShorterThan(Duration.negate(toleranceYLow))
            && ff_xg.noLongerThan(toleranceYHigh)) {
          return new RootFindingResult<>(
              Duration.of(xg, Duration.MICROSECONDS), ff_xg.plus(y), null);
        } else {
          if ((ff_xg.longerThan(Duration.ZERO) && ff_bracket1.longerThan(Duration.ZERO)
              || (ff_xg.shorterThan(Duration.ZERO) && ff_bracket1.shorterThan(Duration.ZERO)))) {
            bracket1Long = xg;
          } else {
            bracket2Long = xg;
          }
          ff_bracket1 = ff.valueAt(bracket1);
        }
      }
      throw new ExceededMaxIterationException();
    }
  }

  public static final class SecantDoubleAlgorithm implements SecantAlgorithm<Double> {

    public RootFindingResult<Double> findRoot(
        Function<Double> f,
        Double x0,
        Double x1,
        Double y,
        Double toleranceYLow,
        Double toleranceYHigh,
        Double xLow,
        Double xHigh,
        int maxNbIterations)
        throws ZeroDerivativeException, InfiniteDerivativeException, ExceededMaxIterationException {
      var history = new ArrayList<Pair<Double, Double>>();

      var x_nminus1 = x0;
      // we make the assumption that a very local derivative will be representative
      var x_n = x1;

      final var ff =
          new Function<Double>() {
            @Override
            public Double valueAt(final Double x) {
              return f.valueAt(x) - y;
            }

            @Override
            public boolean isApproximation() {
              return f.isApproximation();
            }
          };

      var ff_x_nminus1 = ff.valueAt(x_nminus1);
      if (ff_x_nminus1 >= -toleranceYLow
          && ff_x_nminus1 <= toleranceYHigh
          && (x_nminus1 >= xLow && x_nminus1 <= xHigh)) {
        return new RootFindingResult<>(x_nminus1, ff_x_nminus1 + y, new History<>(history));
      }
      var ff_x_n = ff.valueAt(x_n);
      if (ff_x_n >= -toleranceYLow && ff_x_n <= toleranceYHigh && (x_n >= xLow && x_n <= xHigh)) {
        return new RootFindingResult<>(x_n, ff_x_n + y, new History<>(history));
      }
      for (int nbIt = 0; nbIt < maxNbIterations; nbIt++) {
        final var localDerivative = (ff_x_n - ff_x_nminus1) / (x_n - x_nminus1);
        if (localDerivative == 0) throw new ZeroDerivativeException();
        if (Double.isNaN(localDerivative)) throw new InfiniteDerivativeException();
        x_nminus1 = x_n;
        ff_x_nminus1 = ff_x_n;
        x_n = x_n - (ff_x_nminus1 / localDerivative);
        ff_x_n = ff.valueAt(x_n);
        history.add(Pair.of(x_n, ff_x_n));
        if (ff_x_n >= -toleranceYLow && ff_x_n <= toleranceYHigh && (x_n >= xLow && x_n <= xHigh)) {
          return new RootFindingResult<>(x_n, ff_x_n + y, new History<>(history));
        }
      }
      throw new ExceededMaxIterationException();
    }
  }

  public static final class SecantDurationAlgorithm implements SecantAlgorithm<Duration> {

    public RootFindingResult<Duration> findRoot(
        Function<Duration> f,
        Duration x0,
        Duration x1,
        Duration y,
        Duration toleranceYLow,
        Duration toleranceYHigh,
        Duration xLow,
        Duration xHigh,
        int maxNbIterations)
        throws ZeroDerivativeException,
            InfiniteDerivativeException,
            DivergenceException,
            ExceededMaxIterationException,
            NoSolutionException {
      final var history = new ArrayList<Pair<Duration, Duration>>();

      final var ff =
          new Function<Duration>() {
            @Override
            public Duration valueAt(final Duration x) {
              return f.valueAt(x).minus(y);
            }

            @Override
            public boolean isApproximation() {
              return f.isApproximation();
            }
          };

      double x_nminus1_double = x0.in(Duration.MICROSECONDS);
      double x_n_double = x1.in(Duration.MICROSECONDS);
      var x_n = x1;
      final var xLow_long = xLow.in(Duration.MICROSECONDS);
      final var xHigh_long = xHigh.in(Duration.MICROSECONDS);

      if (x_n_double < xLow_long || x_n_double > xHigh_long) {
        throw new DivergenceException(
            "Looking for root out of prescribed domain :[" + xLow + "," + xHigh + "]");
      }
      // We check whether the initial bounds might satisfy the exit criteria.
      var ff_x_nminus1 = ff.valueAt(x0);
      if (ff_x_nminus1.noShorterThan(Duration.negate(toleranceYLow))
          && ff_x_nminus1.noLongerThan(toleranceYHigh)) {
        return new RootFindingResult<>(x0, ff_x_nminus1.plus(y), null);
      }
      var ff_x_n = ff.valueAt(x_n);
      if (ff_x_n.noShorterThan(Duration.negate(toleranceYLow))
          && ff_x_n.noLongerThan(toleranceYHigh)) {
        return new RootFindingResult<>(x_n, ff_x_n.plus(y), null);
      }
      // After these checks, we can be sure that if the two bounds are the same, the derivative will
      // be 0, and thus throw an exception.
      if (x0.isEqualTo(x1)) {
        throw new NoSolutionException();
      }
      history.add(Pair.of(x0, ff_x_nminus1));
      history.add(Pair.of(x1, ff_x_n));
      for (int nbIt = 0; nbIt < maxNbIterations; nbIt++) {
        // (f(xn) - f(xn_m1)) / (xn - xn_m1)
        final double localDerivative =
            (float) (ff_x_n.minus(ff_x_nminus1)).in(Duration.MICROSECONDS)
                / (x_n_double - x_nminus1_double);
        if (localDerivative == 0) throw new ZeroDerivativeException();
        if (Double.isNaN(localDerivative)) throw new InfiniteDerivativeException();
        x_nminus1_double = x_n_double;
        ff_x_nminus1 = ff_x_n;
        // Note : xn_m2 is implicit here as it is used only for computing the derivative
        // localDerivative has been computed with what is now xn_m1 and xn_m2
        x_n_double = x_n_double - (ff_x_nminus1.in(Duration.MICROSECONDS) / localDerivative);
        x_n = Duration.of((long) x_n_double, Duration.MICROSECONDS);
        ff_x_n = ff.valueAt(x_n);
        history.add(Pair.of(x_n, ff_x_n));
        // The final solution needs to be in the given bounds which is why this check is added here.
        if (ff_x_n.noShorterThan(Duration.negate(toleranceYLow))
            && ff_x_n.noLongerThan(toleranceYHigh)
            && (x_n_double >= xLow_long && x_n_double <= xHigh_long)) {
          logger.debug("Root found after " + nbIt + " iterations");
          return new RootFindingResult<>(x_n, ff_x_n.plus(y), new History<>(history));
        }
      }
      throw new ExceededMaxIterationException(new History<>(history));
    }
  }

  public static final class NewtonDoubleAlgorithm implements NewtonAlgorithm<Double> {
    public RootFindingResult<Double> findRoot(
        FunctionWithDerivative<Double> f,
        Double x0,
        Double y,
        Double toleranceYLow,
        Double toleranceYHigh,
        Double xLow,
        Double xHigh,
        int maxNbIterations)
        throws DivergenceException, ZeroDerivativeException, ExceededMaxIterationException {
      var history = new ArrayList<Pair<Double, Double>>();

      final var ff =
          new FunctionWithDerivative<Double>() {
            @Override
            public Double derivativeAt(final Double x) {
              return f.derivativeAt(x);
            }

            @Override
            public Double valueAt(final Double x) {
              return f.valueAt(x) - y;
            }

            @Override
            public boolean isApproximation() {
              return f.isApproximation();
            }
          };

      if (x0 <= xHigh && x0 >= xLow) {
        throw new DivergenceException("Initial solution is out of prescribed domain");
      }
      var x_n = x0;
      var ff_x_n = ff.valueAt(x_n);
      for (int nbIt = 0; nbIt < maxNbIterations; nbIt++) {
        var localDerivative = ff.derivativeAt(x_n);
        if (localDerivative == 0.) {
          throw new ZeroDerivativeException();
        }
        x_n = x_n - ((ff_x_n - y) / localDerivative);
        ff_x_n = ff.valueAt(x_n);
        if (ff_x_n >= -toleranceYLow && ff_x_n <= toleranceYHigh) {
          return new RootFindingResult<>(x_n, ff_x_n, null);
        }
        // outside of domain, diverging
        if (x_n < xLow || x_n > xHigh) {
          throw new DivergenceException(
              "Looking for root out of prescribed domain :[" + xLow + "," + xHigh + "]",
              new History<>(history));
        }
      }
      throw new ExceededMaxIterationException(new History<>(history));
    }
  }
}
