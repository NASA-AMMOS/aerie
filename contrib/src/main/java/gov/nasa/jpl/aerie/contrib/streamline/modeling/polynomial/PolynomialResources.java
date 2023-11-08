package gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ExpiringToResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear;
import gov.nasa.jpl.aerie.contrib.streamline.unit_aware.StandardUnits;
import gov.nasa.jpl.aerie.contrib.streamline.unit_aware.Unit;
import gov.nasa.jpl.aerie.contrib.streamline.unit_aware.UnitAware;
import gov.nasa.jpl.aerie.contrib.streamline.unit_aware.UnitAwareOperations;
import gov.nasa.jpl.aerie.contrib.streamline.unit_aware.UnitAwareResources;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiring.expiring;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiring.neverExpiring;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.whenever;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.wheneverDynamicsChange;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.shift;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad.bindEffect;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad.map;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad.unit;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.eraseExpiry;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad.bind;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad.lift;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad.map;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.ClockResources.clock;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial.polynomial;
import static gov.nasa.jpl.aerie.contrib.streamline.unit_aware.UnitAwareResources.extend;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;

public final class PolynomialResources {
  private PolynomialResources() {}

  public static Resource<Polynomial> constant(double value) {
    var dynamics = unit(polynomial(value));
    return () -> dynamics;
  }

  public static UnitAware<Resource<Polynomial>> constant(UnitAware<Double> quantity) {
    return unitAware(constant(quantity.value()), quantity.unit());
  }

  /**
   * Treat a discrete resource as a polynomial with constant profile segments.
   */
  public static Resource<Polynomial> asPolynomial(Resource<Discrete<Double>> discrete) {
    return map(discrete, d -> polynomial(d.extract()));
  }

  /**
   * Treat a discrete resource as a polynomial with constant profile segments.
   */
  public static UnitAware<Resource<Polynomial>> asUnitAwarePolynomial(UnitAware<? extends Resource<Discrete<Double>>> discrete) {
    return unitAware(asPolynomial(discrete.value()), discrete.unit());
  }

  /**
   * Treat a discrete resource as a polynomial with constant profile segments.
   * <p>
   *   Note that this requires dimension-checking every segment individually, which can degrade performance.
   *   If possible, try using <code>UnitAware&lt;Resource&lt;Discrete&lt;Double&gt;&gt;&gt;</code>
   * </p>
   */
  public static UnitAware<Resource<Polynomial>> asUnitAwarePolynomial(Resource<Discrete<UnitAware<Double>>> discrete) {
    var unit = currentValue(discrete).unit();
    return unitAware(asPolynomial(DiscreteResourceMonad.map(discrete, q -> q.value(unit))), unit);
  }

  /**
   * Treat a linear resource as a polynomial with linear profile segments.
   */
  public static Resource<Polynomial> asPolynomial$(Resource<Linear> linear) {
    return map(linear, l -> polynomial(l.extract(), l.rate()));
  }

  /**
   * Treat a linear resource as a polynomial with linear profile segments.
   */
  public static UnitAware<Resource<Polynomial>> asUnitAwarePolynomial$(UnitAware<? extends Resource<Linear>> linear) {
    return unitAware(asPolynomial$(linear.value()), linear.unit());
  }

  /**
   * Returns a continuous resource that follows a precomputed sequence of values.
   * Before the first key in segments, value is the first entry in segments.
   * Between keys in segments, a linear interpolation between the two adjacent entries is used.
   * After the last key in segments, value is the last entry in segments.
   */
  public static Resource<Polynomial> precomputed(final NavigableMap<Duration, Double> segments) {
    if (segments.isEmpty()) {
      throw new IllegalArgumentException("Segments map must have at least one segment");
    }
    var clock = clock();
    return ResourceMonad.bind(clock, clock$ -> {
      var t = clock$.extract();
      var start = segments.floorEntry(t);
      var end = segments.higherEntry(t);
      Expiring<Polynomial> result;
      if (end == null) {
        result = neverExpiring(polynomial(start.getValue()));
      } else if (start == null) {
        result = expiring(polynomial(end.getValue()), end.getKey().minus(t));
      } else {
        // interpolate between start and end
        var startTime = start.getKey();
        var endTime = end.getKey();
        var slope = (end.getValue() - start.getValue()) / endTime.minus(startTime).ratioOver(SECOND);
        var data = polynomial(start.getValue(), slope).step(t.minus(startTime));
        result = expiring(data, endTime.minus(t));
      }
      return ExpiringToResourceMonad.unit(result);
    });
  }

  /**
   * Returns a continuous resource that follows a precomputed sequence of values.
   * Before the first key in segments, value is valueBeforeFirstEntry.
   * Between keys in segments, a linear interpolation between the two adjacent entries is used.
   * After the last key in segments, value is the last entry in segments.
   */
  public static Resource<Polynomial> precomputed(
      final NavigableMap<Instant, Double> segments, final Instant simulationStartTime) {
    var segmentsUsingDurationKeys = new TreeMap<Duration, Double>();
    for (var entry : segments.entrySet()) {
      segmentsUsingDurationKeys.put(
          Duration.of(
              ChronoUnit.MICROS.between(simulationStartTime, entry.getKey()),
              Duration.MICROSECONDS),
          entry.getValue());
    }
    return precomputed(segmentsUsingDurationKeys);
  }

  /**
   * Add polynomial resources.
   */
  @SafeVarargs
  public static Resource<Polynomial> add(Resource<Polynomial>... summands) {
    return sum(Arrays.stream(summands));
  }

  public static Resource<Polynomial> sum(Stream<? extends Resource<Polynomial>> summands) {
    return summands.reduce(constant(0), lift(Polynomial::add), lift(Polynomial::add)::apply);
  }

  /**
   * Subtract polynomial resources.
   */
  public static Resource<Polynomial> subtract(Resource<Polynomial> p, Resource<Polynomial> q) {
    return map(p, q, Polynomial::subtract);
  }

  /**
   * Flip the sign of a polynomial resource.
   */
  public static Resource<Polynomial> negate(Resource<Polynomial> p) {
    return multiply(constant(-1), p);
  }

  /**
   * Multiply polynomial resources.
   */
  @SafeVarargs
  public static Resource<Polynomial> multiply(Resource<Polynomial>... factors) {
    return Arrays.stream(factors)
                 .reduce(constant(1), (p, q) -> map(p, q, Polynomial::multiply));
  }

  /**
   * Multiply polynomial resources.
   */
  public static Resource<Polynomial> product(Stream<? extends Resource<Polynomial>> factors) {
    return factors.reduce(constant(1), lift(Polynomial::multiply), lift(Polynomial::multiply)::apply);
  }

  /**
   * Divide a polynomial by a discrete resource.
   * <p>
   *   The divisor must be discrete, because the quotient of two polynomials is not necessarily a polynomial.
   * </p>
   */
  public static Resource<Polynomial> divide(Resource<Polynomial> p, Resource<Discrete<Double>> q) {
    return map(p, q, (p$, q$) -> p$.divide(q$.extract()));
  }

  /**
   * Compute the integral of integrand, starting at startingValue.
   * <p>
   *   This method allocates a cell, so must be called during initialization, not simulation.
   * </p>
   */
  public static Resource<Polynomial> integrate(Resource<Polynomial> integrand, double startingValue) {
    var cell = cellResource(map(integrand.getDynamics(), (Polynomial $) -> $.integral(startingValue)));
    // Use integrand's expiry but not integral's, since we're refreshing the integral
    wheneverDynamicsChange(integrand, integrandDynamics ->
        cell.emit(bindEffect(integral -> DynamicsMonad.map(integrandDynamics, integrand$ ->
            integrand$.integral(integral.extract())))));
    return cell;
  }

  /**
   * Compute the integral of integrand, starting at startingValue.
   * Also clamp the integral between lowerBound and upperBound (inclusive).
   * <p>
   *   Note that <code>clampedIntegrate(r, l, u, s)</code> differs from
   *   <code>clamp(integrate(r, s), l, u)</code> in how they handle reversing from a boundary.
   * </p>
   * <p>
   *   To see how, consider bounds of [0, 5], with an integrand of 1 for 10 seconds, then -1 for 10 seconds.
   * </p>
   * <p>
   *   clamp and integrate:
   * </p>
   * <pre>
   *   5       /----------\
   *          /            \
   *         /              \
   *        /                \
   *   0   /                  \
   * time  0        10        20
   * </pre>
   * <p>
   *   clampedIntegrate:
   * </p>
   * <pre>
   *   5       /-----\
   *          /       \
   *         /         \
   *        /           \
   *   0   /             \-----
   * time  0        10        20
   * </pre>
   */
  public static ClampedIntegrateResult clampedIntegrate(
      Resource<Polynomial> integrand, Resource<Polynomial> lowerBound, Resource<Polynomial> upperBound, double startingValue) {
    var cell = cellResource(map(integrand.getDynamics(), (Polynomial $) -> $.integral(startingValue)));
    // Erase expiry information when calculating next iteration of integral
    var nonExpiringCell = eraseExpiry(cell);
    // Clamp integrand to UB/LB rates as needed
    var integrandUB = bind(greaterThanOrEquals(nonExpiringCell, upperBound), full -> full.extract() ? differentiate(upperBound) : constant(Double.POSITIVE_INFINITY));
    var integrandLB = bind(lessThanOrEquals(nonExpiringCell, lowerBound), empty -> empty.extract() ? differentiate(lowerBound) : constant(Double.NEGATIVE_INFINITY));
    var effectiveIntegrand = clamp(integrand, integrandLB, integrandUB);
    // Separately, clamp the value to the UB/LB value to account for overshooting due to discrete time
    // Erase the expiry information from cell to cut the feedback loop there.
    var effectiveIntegral = map(
        clamp(nonExpiringCell, lowerBound, upperBound),
        effectiveIntegrand,
        (value, rate) -> rate.integral(value.extract()));
    // Use integrand's expiry but not integral's, since we're refreshing the integral
    wheneverDynamicsChange(effectiveIntegral, integral -> cell.emit("Update integral", $ -> integral));
    // Finally, compute the overflow/underflow as the difference between input and output rates:
    var flowDifference = subtract(differentiate(effectiveIntegral), integrand);
    return new ClampedIntegrateResult(
        effectiveIntegral,
        max(flowDifference, constant(0)),
        negate(min(flowDifference, constant(0))));
  }

  /**
   * The result of a {@link PolynomialResources#clampedIntegrate(Resource, Resource, Resource, double)} call.
   *
   * @param integral    The clamped integral value.
   * @param overflow    The rate of overflow, when integral hits upper bound.
   *                    Integrate this to get cumulative overflow.
   * @param underflow   The rate of underflow, when integral hits lower bound.
   *                    Integrate this to get cumulative underflow.
   */
  public record ClampedIntegrateResult(
      Resource<Polynomial> integral,
      Resource<Polynomial> overflow,
      Resource<Polynomial> underflow
  ) {}

  /**
   * Returns the derivative of this resource.
   */
  public static Resource<Polynomial> differentiate(Resource<Polynomial> p) {
    return map(p, Polynomial::derivative);
  }

  /**
   * Return a resource which is the average of the operand over the last interval time.
   */
  public static Resource<Polynomial> movingAverage(Resource<Polynomial> p, Duration interval) {
    var pIntegral = integrate(p, 0);
    var shiftedIntegral = shift(pIntegral, interval, polynomial(0));
    return divide(subtract(pIntegral, shiftedIntegral), DiscreteResourceMonad.unit(interval.ratioOver(SECOND)));
  }

  public static Resource<Discrete<Boolean>> greaterThan(Resource<Polynomial> p, double threshold) {
    return bind(p, p$ -> ExpiringToResourceMonad.unit(p$.greaterThan(threshold)));
  }

  public static Resource<Discrete<Boolean>> greaterThanOrEquals(Resource<Polynomial> p, double threshold) {
    return bind(p, p$ -> ExpiringToResourceMonad.unit(p$.greaterThanOrEquals(threshold)));
  }

  public static Resource<Discrete<Boolean>> lessThan(Resource<Polynomial> p, double threshold) {
    return bind(p, p$ -> ExpiringToResourceMonad.unit(p$.lessThan(threshold)));
  }

  public static Resource<Discrete<Boolean>> lessThanOrEquals(Resource<Polynomial> p, double threshold) {
    return bind(p, p$ -> ExpiringToResourceMonad.unit(p$.lessThanOrEquals(threshold)));
  }

  public static Resource<Discrete<Boolean>> greaterThan(Resource<Polynomial> p, Resource<Polynomial> q) {
    return greaterThan(subtract(p, q), 0);
  }

  public static Resource<Discrete<Boolean>> greaterThanOrEquals(Resource<Polynomial> p, Resource<Polynomial> q) {
    return greaterThanOrEquals(subtract(p, q), 0);
  }

  public static Resource<Discrete<Boolean>> lessThan(Resource<Polynomial> p, Resource<Polynomial> q) {
    return lessThan(subtract(p, q), 0);
  }

  public static Resource<Discrete<Boolean>> lessThanOrEquals(Resource<Polynomial> p, Resource<Polynomial> q) {
    return lessThanOrEquals(subtract(p, q), 0);
  }

  public static Resource<Polynomial> min(Resource<Polynomial> p, Resource<Polynomial> q) {
    return ResourceMonad.bind(p, q, (p$, q$) -> ExpiringToResourceMonad.unit(p$.min(q$)));
  }

  public static Resource<Polynomial> max(Resource<Polynomial> p, Resource<Polynomial> q) {
    return ResourceMonad.bind(p, q, (p$, q$) -> ExpiringToResourceMonad.unit(p$.max(q$)));
  }

  /**
   * Absolute value
   */
  public static Resource<Polynomial> abs(Resource<Polynomial> p) {
    return max(p, negate(p));
  }

  /**
   * Returns min(max(p, lowerBound), upperBound).
   * <p>
   *   If lowerBound ever exceeds upperBound, this resource fails.
   * </p>
   */
  public static Resource<Polynomial> clamp(Resource<Polynomial> p, Resource<Polynomial> lowerBound, Resource<Polynomial> upperBound) {
    return ResourceMonad.bind(
        lessThan(upperBound, lowerBound),
        impossible -> {
          if (impossible.extract()) {
            throw new IllegalStateException(
                "Inverted bounds for clamp: maximum %f < minimum %f"
                    .formatted(currentValue(upperBound), currentValue(lowerBound)));
          }
          return max(lowerBound, min(upperBound, p));
        });
  }

  private static Polynomial scalePolynomial(Polynomial p, double s) {
    return p.multiply(polynomial(s));
  }

  /**
   * Add units to a polynomial resource.
   */
  public static UnitAware<Resource<Polynomial>> unitAware(Resource<Polynomial> p, Unit unit) {
    return UnitAwareResources.unitAware(p, unit, PolynomialResources::scalePolynomial);
  }

  /**
   * Add units to a polynomial resource.
   */
  public static UnitAware<CellResource<Polynomial>> unitAware(CellResource<Polynomial> p, Unit unit) {
    return UnitAwareResources.unitAware(p, unit, PolynomialResources::scalePolynomial);
  }

  /**
   * Add polynomial resources.
   */
  @SafeVarargs
  public static UnitAware<Resource<Polynomial>> add(UnitAware<? extends Resource<Polynomial>>... summands) {
    if (summands.length == 0) {
      throw new IllegalArgumentException("Cannot perform unit-aware addition of zero arguments.");
    }
    final Unit unit = summands[0].unit();
    return unitAware(sum(Arrays.stream(summands).map(r -> r.value(unit))), unit);
  }

  /**
   * Add polynomial resources.
   */
  public static UnitAware<Resource<Polynomial>> sum$(Stream<UnitAware<? extends Resource<Polynomial>>> summands) {
    return add(summands.<UnitAware<? extends Resource<Polynomial>>>toArray(UnitAware[]::new));
  }

  /**
   * Subtract polynomial resources.
   */
  public static UnitAware<Resource<Polynomial>> subtract(UnitAware<? extends Resource<Polynomial>> p, UnitAware<? extends Resource<Polynomial>> q) {
    return UnitAwareOperations.subtract(extend(PolynomialResources::scalePolynomial),
                                        PolynomialResources::subtract,
                                        p, q);
  }

  /**
   * Multiply polynomial resources.
   */
  @SafeVarargs
  public static UnitAware<Resource<Polynomial>> multiply(UnitAware<? extends Resource<Polynomial>>... factors) {
    return unitAware(
        product(Arrays.stream(factors).map(UnitAware::value)),
        Arrays.stream(factors).map(UnitAware::unit).reduce(Unit.SCALAR, Unit::multiply));
  }

  /**
   * Multiply polynomial resources.
   */
  public static UnitAware<Resource<Polynomial>> product$(Stream<UnitAware<? extends Resource<Polynomial>>> factors) {
    return multiply(factors.<UnitAware<? extends Resource<Polynomial>>>toArray(UnitAware[]::new));
  }

  /**
   * Divide a polynomial by a discrete resource.
   * <p>
   *   The divisor must be discrete, because the quotient of two polynomials is not necessarily a polynomial.
   * </p>
   */
  public static UnitAware<Resource<Polynomial>> divide(UnitAware<? extends Resource<Polynomial>> p, UnitAware<? extends Resource<Discrete<Double>>> q) {
    return UnitAwareOperations.divide(extend(PolynomialResources::scalePolynomial), PolynomialResources::divide, p, q);
  }

  /**
   * Compute the integral of integrand, starting at startingValue.
   * <p>
   *   This method allocates a cell, so must be called during initialization, not simulation.
   * </p>
   */
  public static UnitAware<Resource<Polynomial>> integrate(UnitAware<? extends Resource<Polynomial>> p, UnitAware<Double> startingValue) {
    return UnitAwareOperations.integrate(extend(PolynomialResources::scalePolynomial),
                                         PolynomialResources::integrate,
                                         p, startingValue);
  }

  /**
   * Compute the integral of integrand, starting at startingValue.
   * Also clamp the integral between lowerBound and upperBound (inclusive).
   * <p>
   *   Note that <code>clampedIntegrate(r, l, u, s)</code> differs from
   *   <code>clamp(integrate(r, s), l, u)</code> in how they handle reversing from a boundary.
   * </p>
   * <p>
   *   To see how, consider bounds of [0, 5], with an integrand of 1 for 10 seconds, then -1 for 10 seconds.
   * </p>
   * <p>
   *   clamp and integrate:
   * </p>
   * <pre>
   *   5       /----------\
   *          /            \
   *         /              \
   *        /                \
   *   0   /                  \
   * time  0        10        20
   * </pre>
   * <p>
   *   clampedIntegrate:
   * </p>
   * <pre>
   *   5       /-----\
   *          /       \
   *         /         \
   *        /           \
   *   0   /             \-----
   * time  0        10        20
   * </pre>
   */
  public static UnitAwareClampedIntegrateResult clampedIntegrate(UnitAware<? extends Resource<Polynomial>> p, UnitAware<? extends Resource<Polynomial>> lowerBound, UnitAware<? extends Resource<Polynomial>> upperBound, UnitAware<Double> startingValue) {
    final Unit resultUnit = p.unit().multiply(StandardUnits.SECOND);
    var unitNaiveResult = clampedIntegrate(
            p.value(),
            lowerBound.value(resultUnit),
            upperBound.value(resultUnit),
            startingValue.value(resultUnit));
    return new UnitAwareClampedIntegrateResult(
        unitAware(unitNaiveResult.integral(), resultUnit),
        unitAware(unitNaiveResult.overflow(), p.unit()),
        unitAware(unitNaiveResult.underflow(), p.unit()));
  }

  /**
   * The result of a {@link PolynomialResources#clampedIntegrate(UnitAware, UnitAware, UnitAware, UnitAware)} call.
   *
   * @param integral    The clamped integral value.
   * @param overflow    The rate of overflow, when integral hits upper bound.
   *                    Integrate this to get cumulative overflow.
   * @param underflow   The rate of underflow, when integral hits lower bound.
   *                    Integrate this to get cumulative underflow.
   */
  public record UnitAwareClampedIntegrateResult(
      UnitAware<Resource<Polynomial>> integral,
      UnitAware<Resource<Polynomial>> overflow,
      UnitAware<Resource<Polynomial>> underflow
  ) {}

  /**
   * Returns the derivative of this resource.
   */
  public static UnitAware<Resource<Polynomial>> differentiate(UnitAware<? extends Resource<Polynomial>> p) {
    return UnitAwareOperations.differentiate(extend(PolynomialResources::scalePolynomial),
                                             PolynomialResources::differentiate,
                                             p);
  }

  // Ugly $ suffix is to avoid ambiguous overloading after erasure.
  public static Resource<Discrete<Boolean>> greaterThan$(UnitAware<? extends Resource<Polynomial>> p, UnitAware<Double> threshold) {
    return greaterThan(p.value(), threshold.value(p.unit()));
  }

  public static Resource<Discrete<Boolean>> greaterThanOrEquals$(UnitAware<? extends Resource<Polynomial>> p, UnitAware<Double> threshold) {
    return greaterThanOrEquals(p.value(), threshold.value(p.unit()));
  }

  public static Resource<Discrete<Boolean>> lessThan$(UnitAware<? extends Resource<Polynomial>> p, UnitAware<Double> threshold) {
    return lessThan(p.value(), threshold.value(p.unit()));
  }

  public static Resource<Discrete<Boolean>> lessThanOrEquals$(UnitAware<? extends Resource<Polynomial>> p, UnitAware<Double> threshold) {
    return lessThanOrEquals(p.value(), threshold.value(p.unit()));
  }

  public static Resource<Discrete<Boolean>> greaterThan(UnitAware<? extends Resource<Polynomial>> p, UnitAware<? extends Resource<Polynomial>> q) {
    return greaterThan(subtract(p, q).value(), 0);
  }

  public static Resource<Discrete<Boolean>> greaterThanOrEquals(UnitAware<? extends Resource<Polynomial>> p, UnitAware<? extends Resource<Polynomial>> q) {
    return greaterThanOrEquals(subtract(p, q).value(), 0);
  }

  public static Resource<Discrete<Boolean>> lessThan(UnitAware<? extends Resource<Polynomial>> p, UnitAware<? extends Resource<Polynomial>> q) {
    return lessThan(subtract(p, q).value(), 0);
  }

  public static Resource<Discrete<Boolean>> lessThanOrEquals(UnitAware<? extends Resource<Polynomial>> p, UnitAware<? extends Resource<Polynomial>> q) {
    return lessThanOrEquals(subtract(p, q).value(), 0);
  }

  public static UnitAware<Resource<Polynomial>> min(UnitAware<? extends Resource<Polynomial>> p, UnitAware<? extends Resource<Polynomial>> q) {
    return unitAware(min(p.value(), q.value(p.unit())), p.unit());
  }

  public static UnitAware<Resource<Polynomial>> max(UnitAware<? extends Resource<Polynomial>> p, UnitAware<? extends Resource<Polynomial>> q) {
    return unitAware(max(p.value(), q.value(p.unit())), p.unit());
  }

  /**
   * Returns min(max(p, lowerBound), upperBound).
   * <p>
   *   If lowerBound ever exceeds upperBound, this resource fails.
   * </p>
   */
  public static UnitAware<Resource<Polynomial>> clamp(UnitAware<? extends Resource<Polynomial>> p, UnitAware<? extends Resource<Polynomial>> lowerBound, UnitAware<? extends Resource<Polynomial>> upperBound) {
    return unitAware(clamp(p.value(), lowerBound.value(p.unit()), upperBound.value(p.unit())), p.unit());
  }
}
