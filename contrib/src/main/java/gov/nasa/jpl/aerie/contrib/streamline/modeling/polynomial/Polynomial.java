package gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial;

import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ExpiringMonad;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import org.apache.commons.math3.analysis.solvers.LaguerreSolver;
import org.apache.commons.math3.complex.Complex;

import java.util.Arrays;
import java.util.function.DoublePredicate;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiring.expiring;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiry.NEVER;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiry.expiry;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.EPSILON;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;
import static org.apache.commons.math3.analysis.polynomials.PolynomialsUtils.shift;

public record Polynomial(double[] coefficients) implements Dynamics<Double, Polynomial> {
  /**
   * Maximum imaginary component allowed in a root to be considered "real" when performing root-finding.
   * Should be a very small number to avoid spurious roots.
   */
  private static final double ROOT_FINDING_IMAGINARY_COMPONENT_TOLERANCE = 1e-12;

  /**
   * Maximum number of time steps to search in either direction around near-roots
   * to find the corresponding discretized transition point.
   */
  private static final int MAX_RANGE_FOR_ROOT_SEARCH = 2;

  // TODO: Add Duration parameter for unit of formal parameter
  public static Polynomial polynomial(double... coefficients) {
    int n = coefficients.length;
    if (n == 0) {
      return new Polynomial(new double[] { 0.0 });
    }
    while (n > 1 && coefficients[n - 1] == 0) --n;
    return new Polynomial(Arrays.copyOf(coefficients, n));
  }

  @Override
  public Double extract() {
    return coefficients()[0];
  }

  @Override
  public Polynomial step(Duration t) {
    return t.isEqualTo(ZERO) ? this : polynomial(shift(coefficients(), t.ratioOver(SECOND)));
  }

  public int degree() {
    return coefficients().length - 1;
  }

  public boolean isConstant() {
    return degree() == 0;
  }

  public boolean isNonFinite() {
    return Arrays.stream(coefficients()).anyMatch(c -> !Double.isFinite(c));
  }

  public Polynomial add(Polynomial other) {
    final double[] coefficients = coefficients();
    final double[] otherCoefficients = other.coefficients();
    final int minLength = Math.min(coefficients.length, otherCoefficients.length);
    final int maxLength = Math.max(coefficients.length, otherCoefficients.length);
    final double[] newCoefficients = new double[maxLength];
    for (int i = 0; i < minLength; ++i) {
      newCoefficients[i] = coefficients[i] + otherCoefficients[i];
    }
    if (coefficients.length > minLength)
      System.arraycopy(coefficients, minLength, newCoefficients, minLength, coefficients.length - minLength);
    if (otherCoefficients.length > minLength)
      System.arraycopy(
          otherCoefficients, minLength, newCoefficients, minLength, otherCoefficients.length - minLength);
    return polynomial(newCoefficients);
  }

  public Polynomial subtract(Polynomial other) {
    return add(other.multiply(polynomial(-1)));
  }

  public Polynomial multiply(Polynomial other) {
    final double[] coefficients = coefficients();
    final double[] otherCoefficients = other.coefficients();
    // Length = degree + 1, so
    // new length = 1 + new degree
    //   = 1 + (degree + other.degree)
    //   = 1 + (length - 1 + other.length - 1)
    //   = length + other.length - 1
    final double[] newCoefficients = new double[coefficients.length + otherCoefficients.length - 1];
    for (int exponent = 0; exponent < newCoefficients.length; ++exponent) {
      newCoefficients[exponent] = 0.0;
      // 0 <= k < length and 0 <= exponent - k < other.length
      // implies k >= 0, k > exponent - other.length,
      // k < length, and k <= exponent
      for (int k = Math.max(0, exponent - otherCoefficients.length + 1);
           k < Math.min(coefficients.length, exponent + 1);
           ++k) {
        newCoefficients[exponent] += coefficients[k] * otherCoefficients[exponent - k];
      }
    }
    return polynomial(newCoefficients);
  }

  public Polynomial divide(double scalar) {
    final double[] coefficients = coefficients();
    final double[] newCoefficients = new double[coefficients.length];
    for (int i = 0; i < coefficients.length; ++i) {
      newCoefficients[i] = coefficients[i] / scalar;
    }
    return polynomial(newCoefficients);
  }

  public Polynomial integral(double startingValue) {
    final double[] coefficients = coefficients();
    final double[] newCoefficients = new double[coefficients.length + 1];
    newCoefficients[0] = startingValue;
    for (int i = 0; i < coefficients.length; ++i) {
      newCoefficients[i + 1] = coefficients[i] / (i + 1);
    }
    return polynomial(newCoefficients);
  }

  public Polynomial derivative() {
    final double[] coefficients = coefficients();
    final double[] newCoefficients = new double[coefficients.length - 1];
    for (int i = 1; i < coefficients.length; ++i) {
      newCoefficients[i - 1] = coefficients[i] * i;
    }
    return polynomial(newCoefficients);
  }

  public double evaluate(Duration t) {
    return evaluate(t.ratioOver(SECOND));
  }

  public double evaluate(double x) {
    // Horner's method of polynomial evaluation:
    // Transforms a_0 + a_1 x + a_2 x^2 + ... + a_n x^n
    // into a_0 + x (a_1 + x (a_2 + ... x ( a_n ) ... ))
    // Which can be done with one addition and one multiplication per coefficient,
    // as opposed to the traditional method, which takes one addition and multiple multiplications.
    final double[] coefficients = coefficients();
    double accumulator = coefficients[coefficients.length - 1];
    for (int i = coefficients.length - 2; i >= 0; --i) {
      accumulator *= x;
      accumulator += coefficients[i];
    }
    return accumulator;
  }

  private Expiring<Discrete<Boolean>> compare(DoublePredicate predicate, double threshold) {
    return find(t -> predicate.test(evaluate(t)), threshold);
  }

  private Expiring<Discrete<Boolean>> find(Predicate<Duration> timePredicate, double target) {
    final boolean currentValue = timePredicate.test(ZERO);
    final var expiry = this.isConstant() || this.isNonFinite() ? NEVER : expiry(findFuturePreImage(target)
        .flatMap(t -> IntStream.rangeClosed(-MAX_RANGE_FOR_ROOT_SEARCH, MAX_RANGE_FOR_ROOT_SEARCH)
            .mapToObj(i -> t.plus(EPSILON.times(i))))
        .filter(t -> (timePredicate.test(t) ^ currentValue) && t.isPositive())
        .findFirst());
    return expiring(discrete(currentValue), expiry);
  }

  public Expiring<Discrete<Boolean>> greaterThan(double threshold) {
    return compare(x -> x > threshold, threshold);
  }

  public Expiring<Discrete<Boolean>> greaterThanOrEquals(double threshold) {
    return compare(x -> x >= threshold, threshold);
  }

  public Expiring<Discrete<Boolean>> lessThan(double threshold) {
    return compare(x -> x < threshold, threshold);
  }

  public Expiring<Discrete<Boolean>> lessThanOrEquals(double threshold) {
    return compare(x -> x <= threshold, threshold);
  }

  private boolean dominates$(Polynomial other) {
    for (int i = 0; i <= Math.max(this.degree(), other.degree()); ++i) {
      if (this.getCoefficient(i) > other.getCoefficient(i)) return true;
      if (this.getCoefficient(i) < other.getCoefficient(i)) return false;
    }
    // Equal, so either answer is correct
    return true;
  }

  private Expiring<Discrete<Boolean>> dominates(Polynomial other) {
    return this.subtract(other).find(t -> this.step(t).dominates$(other.step(t)), 0);
  }

  public Expiring<Polynomial> min(Polynomial other) {
    return ExpiringMonad.map(this.dominates(other), d -> d.extract() ? other : this);
  }

  public Expiring<Polynomial> max(Polynomial other) {
    return ExpiringMonad.map(this.dominates(other), d -> d.extract() ? this : other);
  }

  /**
   * Finds all occasions in the future when this function will reach the target value.
   */
  private Stream<Duration> findFuturePreImage(double target) {
    // add a check for an infinite target (i.e. unbounded above/below) or a poorly behaved polynomial
    if (!Double.isFinite(target) || this.isNonFinite()) {
      return Stream.empty();
    }

    final double[] shiftedCoefficients = add(polynomial(-target)).coefficients();
    final Complex[] solutions = new LaguerreSolver().solveAllComplex(shiftedCoefficients, 0);
    return Arrays.stream(solutions)
                 .filter(solution -> Math.abs(solution.getImaginary()) < ROOT_FINDING_IMAGINARY_COMPONENT_TOLERANCE)
                 .map(Complex::getReal)
                 .filter(t -> t >= 0 && t <= MAX_SECONDS_FOR_DURATION)
                 .sorted()
                 .map(t -> Duration.roundNearest(t, SECOND));
  }
  private static final double MAX_SECONDS_FOR_DURATION = Duration.MAX_VALUE.ratioOver(SECOND);

  /**
   * Get the nth coefficient.
   * @param n the n.
   * @return the nth coefficient
   */
  public double getCoefficient(int n) {
    return n >= coefficients().length ? 0.0 : coefficients()[n];
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Polynomial that = (Polynomial) o;
    return Arrays.equals(coefficients, that.coefficients);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(coefficients);
  }

  @Override
  public String toString() {
    return "Polynomial{" +
           "coefficients=" + Arrays.toString(coefficients) +
           '}';
  }
}
