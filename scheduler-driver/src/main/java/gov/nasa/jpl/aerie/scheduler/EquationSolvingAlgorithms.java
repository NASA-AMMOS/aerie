package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EquationSolvingAlgorithms {

  private static final Logger logger = LoggerFactory.getLogger(EquationSolvingAlgorithms.class);

  public record RootFindingResult<T, History>(T x, T fx, History history){}

  /**
   * Solves f(x) = y for x in [xLow, xHigh] with confidence interval [yLow, yHigh] around y such that we stop when
   * value y is in [y-yLow, y+yHigh].
   * x0 and x1 are initial guesses for x, they must be close to the solution to avoid diverging
   * It is considered that algorithm is diverging when the iterated value of x goes out of [xLow, xHigh].
   */
  public interface SecantAlgorithm<T, History>{
    RootFindingResult<T, History> findRoot(Function<T, History> f,
                                  History history,
                                  T x0,
                                  T x1,
                                  T y,
                                  T toleranceYLow,
                                  T toleranceYHigh,
                                  T xLow,
                                  T xHigh,
                                  int maxNbIterations) throws ZeroDerivativeException, InfiniteDerivativeException, DivergenceException,
                                                              ExceededMaxIterationException, NoSolutionException;
  }

  public interface Function<T, History> {
    T valueAt(T x, History historyType);
    boolean isApproximation();
  }

  public static class ZeroDerivativeException extends Exception{
    public ZeroDerivativeException() {}
  }

  public static class InfiniteDerivativeException extends Exception{
    public InfiniteDerivativeException() {}
  }

  public static class DivergenceException extends Exception{
    public DivergenceException(String errorMessage) {
      super(errorMessage);
    }
  }
  public static class WrongBracketingException extends Exception{
    public WrongBracketingException(String errorMessage) {
      super(errorMessage);
    }
  }

  public static class ExceededMaxIterationException extends Exception{
    public ExceededMaxIterationException() {
      super();
    }
  }

  public static class NoSolutionException extends Exception{
    public NoSolutionException() {
      super();
    }
    public NoSolutionException(String errorMessage) {
      super(errorMessage);
    }
  }

  public static class SecantDurationAlgorithm<History> implements SecantAlgorithm<Duration, History>{

    public RootFindingResult<Duration, History> findRoot(
        Function<Duration, History> f,
        History history,
        Duration x0,
        Duration x1,
        Duration y,
        Duration toleranceYLow,
        Duration toleranceYHigh,
        Duration xLow,
        Duration xHigh,
        int maxNbIterations)
    throws ZeroDerivativeException, InfiniteDerivativeException, DivergenceException, ExceededMaxIterationException, NoSolutionException
    {
      final var ff = new Function<Duration, History> (){
        @Override
        public Duration valueAt(final Duration x, final History history) {
          return f.valueAt(x, history).minus(y);
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
        throw new DivergenceException("Looking for root out of prescribed domain :[" + xLow + "," + xHigh + "]");
      }
      //We check whether the initial bounds might satisfy the exit criteria.
      var ff_x_nminus1 = ff.valueAt(x0, history);
      if (ff_x_nminus1.noShorterThan(Duration.negate(toleranceYLow)) && ff_x_nminus1.noLongerThan(toleranceYHigh)) {
        return new RootFindingResult<>(x0, ff_x_nminus1.plus(y), history);
      }
      var ff_x_n = ff.valueAt(x_n, history);
      if (ff_x_n.noShorterThan(Duration.negate(toleranceYLow)) && ff_x_n.noLongerThan(toleranceYHigh)) {
        return new RootFindingResult<>(x_n, ff_x_n.plus(y), history);
      }
      // After these checks, we can be sure that if the two bounds are the same, the derivative will be 0, and thus throw an exception.
      if (x0.isEqualTo(x1)) {
        throw new NoSolutionException();
      }
      for (int nbIt = 0; nbIt < maxNbIterations; nbIt++) {
        //(f(xn) - f(xn_m1)) / (xn - xn_m1)
        final double localDerivative =
            (float) (ff_x_n.minus(ff_x_nminus1)).in(Duration.MICROSECONDS) / (x_n_double - x_nminus1_double);
        if (localDerivative == 0) throw new ZeroDerivativeException();
        if (Double.isNaN(localDerivative)) throw new InfiniteDerivativeException();
        x_nminus1_double = x_n_double;
        ff_x_nminus1 = ff_x_n;
        //Note : xn_m2 is implicit here as it is used only for computing the derivative
        //localDerivative has been computed with what is now xn_m1 and xn_m2
        x_n_double = x_n_double - (ff_x_nminus1.in(Duration.MICROSECONDS) / localDerivative);
        x_n = Duration.of((long) x_n_double, Duration.MICROSECONDS);
        ff_x_n = ff.valueAt(x_n, history);
        //The final solution needs to be in the given bounds which is why this check is added here.
        if (ff_x_n.noShorterThan(Duration.negate(toleranceYLow)) &&
            ff_x_n.noLongerThan(toleranceYHigh) &&
            (x_n_double >= xLow_long && x_n_double <= xHigh_long)){
          logger.debug("Root found after " + nbIt + " iterations");
          return new RootFindingResult<>(x_n, ff_x_n.plus(y), history);
        }
      }
      throw new ExceededMaxIterationException();
    }
  }
}
