package gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ExpiringToResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.StandardUnits;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.Unit;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.UnitAware;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.UnitAwareOperations;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.UnitAwareResources;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.NavigableMap;
import java.util.TreeMap;

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
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad.map;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.ClockResources.clock;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial.polynomial;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.UnitAwareResources.extend;
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

  public static Resource<Polynomial> asPolynomial(Resource<Discrete<Double>> discrete) {
    return map(discrete, d -> polynomial(d.extract()));
  }

  public static UnitAware<Resource<Polynomial>> asUnitAwarePolynomial(UnitAware<? extends Resource<Discrete<Double>>> discrete) {
    return unitAware(asPolynomial(discrete.value()), discrete.unit());
  }

  public static UnitAware<Resource<Polynomial>> asUnitAwarePolynomial(Resource<Discrete<UnitAware<Double>>> discrete) {
    var unit = currentValue(discrete).unit();
    return unitAware(asPolynomial(DiscreteResourceMonad.map(discrete, q -> q.value(unit))), unit);
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

  @SafeVarargs
  public static Resource<Polynomial> add(Resource<Polynomial>... summands) {
    return Arrays.stream(summands)
        .reduce(constant(0), (p, q) -> map(p, q, Polynomial::add));
  }

  public static Resource<Polynomial> subtract(Resource<Polynomial> p, Resource<Polynomial> q) {
    return map(p, q, Polynomial::subtract);
  }

  public static Resource<Polynomial> negate(Resource<Polynomial> p) {
    return multiply(constant(-1), p);
  }

  @SafeVarargs
  public static Resource<Polynomial> multiply(Resource<Polynomial>... factors) {
    return Arrays.stream(factors)
                 .reduce(constant(1), (p, q) -> map(p, q, Polynomial::multiply));
  }

  public static Resource<Polynomial> divide(Resource<Polynomial> p, Resource<Discrete<Double>> q) {
    return map(p, q, (p$, q$) -> p$.divide(q$.extract()));
  }

  public static Resource<Polynomial> integrate(Resource<Polynomial> integrand, double startingValue) {
    var cell = cellResource(map(integrand.getDynamics(), (Polynomial $) -> $.integral(startingValue)));
    // Use integrand's expiry but not integral's, since we're refreshing the integral
    wheneverDynamicsChange(integrand, integrandDynamics ->
        cell.emit(bindEffect(integral -> DynamicsMonad.map(integrandDynamics, integrand$ ->
            integrand$.integral(integral.extract())))));
    return cell;
  }

  public static Resource<Polynomial> clampedIntegrate(
      Resource<Polynomial> integrand, Resource<Polynomial> lowerBound, Resource<Polynomial> upperBound, double startingValue) {
    var cell = cellResource(map(integrand.getDynamics(), (Polynomial $) -> $.integral(startingValue)));
    // Clamp integrand to UB/LB rates as needed
    var integrandUB = bind(greaterThanOrEquals(cell, upperBound), full -> full.extract() ? differentiate(upperBound) : constant(Double.POSITIVE_INFINITY));
    var integrandLB = bind(lessThanOrEquals(cell, lowerBound), empty -> empty.extract() ? differentiate(lowerBound) : constant(Double.NEGATIVE_INFINITY));
    var effectiveIntegrand = clamp(integrand, integrandLB, integrandUB);
    // Separately, clamp the value to the UB/LB value to account for overshooting due to discrete time
    // Erase the expiry information from cell to cut the feedback loop there.
    var effectiveIntegral = map(
        clamp(eraseExpiry(cell), lowerBound, upperBound),
        effectiveIntegrand,
        (value, rate) -> rate.integral(value.extract()));
    // Use integrand's expiry but not integral's, since we're refreshing the integral
    wheneverDynamicsChange(effectiveIntegral, integral -> cell.emit("Update integral", $ -> integral));
    return cell;
  }

  public static Resource<Polynomial> differentiate(Resource<Polynomial> p) {
    return map(p, Polynomial::derivative);
  }

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

  public static Resource<Polynomial> abs(Resource<Polynomial> p) {
    return max(p, negate(p));
  }

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

  public static UnitAware<Resource<Polynomial>> unitAware(Resource<Polynomial> p, Unit unit) {
    return UnitAwareResources.unitAware(p, unit, PolynomialResources::scalePolynomial);
  }

  public static UnitAware<CellResource<Polynomial>> unitAware(CellResource<Polynomial> p, Unit unit) {
    return UnitAwareResources.unitAware(p, unit, PolynomialResources::scalePolynomial);
  }

  public static UnitAware<Resource<Polynomial>> add(UnitAware<? extends Resource<Polynomial>> p, UnitAware<? extends Resource<Polynomial>> q) {
    return UnitAwareOperations.add(extend(PolynomialResources::scalePolynomial), p, q, PolynomialResources::add);
  }

  public static UnitAware<Resource<Polynomial>> subtract(UnitAware<? extends Resource<Polynomial>> p, UnitAware<? extends Resource<Polynomial>> q) {
    return UnitAwareOperations.subtract(extend(PolynomialResources::scalePolynomial), p, q, PolynomialResources::subtract);
  }

  public static UnitAware<Resource<Polynomial>> multiply(UnitAware<? extends Resource<Polynomial>> p, UnitAware<? extends Resource<Polynomial>> q) {
    return UnitAwareOperations.multiply(extend(PolynomialResources::scalePolynomial), p, q, PolynomialResources::multiply);
  }

  public static UnitAware<Resource<Polynomial>> divide(UnitAware<? extends Resource<Polynomial>> p, UnitAware<? extends Resource<Discrete<Double>>> q) {
    return UnitAwareOperations.divide(extend(PolynomialResources::scalePolynomial), p, q, PolynomialResources::divide);
  }

  public static UnitAware<Resource<Polynomial>> integrate(UnitAware<? extends Resource<Polynomial>> p, UnitAware<Double> startingValue) {
    return UnitAwareOperations.integrate(extend(PolynomialResources::scalePolynomial), p, startingValue, PolynomialResources::integrate);
  }

  public static UnitAware<Resource<Polynomial>> clampedIntegrate(UnitAware<? extends Resource<Polynomial>> p, UnitAware<? extends Resource<Polynomial>> lowerBound, UnitAware<? extends Resource<Polynomial>> upperBound, UnitAware<Double> startingValue) {
    final Unit resultUnit = p.unit().multiply(StandardUnits.SECOND);
    return unitAware(
        clampedIntegrate(
            p.value(),
            lowerBound.value(resultUnit),
            upperBound.value(resultUnit),
            startingValue.value(resultUnit)),
        resultUnit);
  }

  public static UnitAware<Resource<Polynomial>> differentiate(UnitAware<? extends Resource<Polynomial>> p) {
    return UnitAwareOperations.differentiate(extend(PolynomialResources::scalePolynomial), p, PolynomialResources::differentiate);
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

  public static UnitAware<Resource<Polynomial>> clamp(UnitAware<? extends Resource<Polynomial>> p, UnitAware<? extends Resource<Polynomial>> lowerBound, UnitAware<? extends Resource<Polynomial>> upperBound) {
    return unitAware(clamp(p.value(), lowerBound.value(p.unit()), upperBound.value(p.unit())), p.unit());
  }
}
