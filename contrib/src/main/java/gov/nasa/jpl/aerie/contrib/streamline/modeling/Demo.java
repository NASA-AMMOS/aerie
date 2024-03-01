package gov.nasa.jpl.aerie.contrib.streamline.modeling;

import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.*;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.monads.UnstructuredResourceApplicative;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.*;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialEffects;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources;
import gov.nasa.jpl.aerie.contrib.streamline.unit_aware.UnitAware;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.merlin.framework.ModelActions;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.time.Instant;
import java.util.Optional;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.Approximation.approximate;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.DifferentiableResources.asDifferentiable;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.DifferentiableResources.divide;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.DivergenceEstimators.byBoundingError;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.IntervalFunctions.byBoundingError;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.IntervalFunctions.byUniformSampling;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.SecantApproximation.ErrorEstimates.errorByOptimization;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.SecantApproximation.ErrorEstimates.errorByQuadraticApproximation;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.SecantApproximation.secantApproximation;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.UnstructuredResources.asUnstructured;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.AbsoluteVariableClockResources.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.AbsoluteVariableClockResources.subtract;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.VariableClockResources.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.set;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.toggle;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.using;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.constant;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad.map;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialEffects.consume;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.*;
import static gov.nasa.jpl.aerie.contrib.streamline.unit_aware.Quantities.quantity;
import static gov.nasa.jpl.aerie.contrib.streamline.unit_aware.StandardUnits.*;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.*;
import static gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource.resource;

/**
 * Non-executable demonstration class to quickly sketch out some use cases for the streamline framework.
 */
public final class Demo {

  // Unit-naive version of a model, to demonstrate some core concepts:

  // Consumable, continuous:
  MutableResource<Polynomial> fuel_kg = PolynomialResources.polynomialResource(20.0);
  // Non-consumable, discrete:
  MutableResource<Discrete<Double>> power_w = discreteResource(120.0);
  // Atomic non-consumable:
  MutableResource<Discrete<Integer>> rwaControl = DiscreteResources.discreteResource(1);
  // Settable / enum state:
  MutableResource<Discrete<OnOff>> enumSwitch = DiscreteResources.discreteResource(OnOff.ON);
  // Toggle / flag:
  MutableResource<Discrete<Boolean>> boolSwitch = DiscreteResources.discreteResource(true);

  // Derived states:
  Resource<Discrete<OnOff>> derivedEnumSwitch = map(boolSwitch, b -> b ? OnOff.ON : OnOff.OFF);
  Resource<Polynomial> batterySOC_J = integrate(asPolynomial(power_w), 100);
  Resource<Discrete<Double>> clampedPower_w = map(power_w, p -> p < 0 ? 0 : p);
  Resource<Polynomial> clampedBatterySOC_J = clamp(batterySOC_J, PolynomialResources.constant(0), PolynomialResources.constant(100));
  Resource<Discrete<Boolean>> lowPower = lessThan(batterySOC_J, 20);
  Resource<Discrete<Boolean>> badness = map(
      lowPower, enumSwitch,
      (lowPower$, switch$) ->
          lowPower$ && switch$ == OnOff.OFF);

  {
    using(power_w, 10, () -> {
      using(rwaControl, () -> {
        // Consume 5.4 kg of fuel over the next minute, linearly
        PolynomialEffects.consumeUniformly(fuel_kg, 5.4, Duration.MINUTE);
        // Separately, we could be doing things during that minute.
        delay(Duration.MINUTE);
      });
      set(enumSwitch, OnOff.OFF);
      toggle(boolSwitch);
    });

    set(boolSwitch, false);
  }


  // The exact same model again, but this time made unit-aware throughout.
  // States without units have been re-used instead of being re-defined

  // Consumable, continuous:
  // CellResource<Polynomial> fuel_kg = polynomialCellResource(20.0);
  UnitAware<MutableResource<Polynomial>> fuel = unitAware(
      PolynomialResources.polynomialResource(20.0), KILOGRAM);
  // Non-consumable, discrete:
  UnitAware<MutableResource<Discrete<Double>>> power = DiscreteResources.unitAware(
      discreteResource(120.0), WATT);

  UnitAware<Resource<Polynomial>> batterySOC = integrate(asUnitAwarePolynomial(power), quantity(100, JOULE));
  UnitAware<Resource<Discrete<Double>>> clampedPower = DiscreteResources.unitAware(map(power.value(WATT), p -> p < 0 ? 0 : p), WATT);
  UnitAware<Resource<Discrete<Double>>> clampedPower_v2 = /* map(power, p -> lessThan(p, quantity(0, WATT)) ? quantity(0, WATT) : p) */
      null;
  UnitAware<Resource<Polynomial>> clampedBatterySOC = clamp(batterySOC, PolynomialResources.constant(quantity(0, JOULE)), PolynomialResources.constant(quantity(100, JOULE)));
  Resource<Discrete<Boolean>> lowPower$ = lessThan$(batterySOC, quantity(20, JOULE));

  {
    using(power, quantity(10, WATT), () -> {
      using(rwaControl, () -> {
        // Consume 5.4 kg of fuel over the next minute, linearly
        consume(fuel, quantity(5.4, KILOGRAM), Duration.MINUTE);
        // Separately, we could be doing things during that minute.
        delay(Duration.MINUTE);
      });
      set(enumSwitch, OnOff.OFF);
      toggle(boolSwitch);
    });
  }

  // Example of using unstructured resources + approximation to represent functions that aren't
  // easily represented by analytic derivations
  Resource<Polynomial> p = PolynomialResources.polynomialResource(1, 2, 3);
  Resource<Polynomial> q = PolynomialResources.polynomialResource(6, 5, 4);
  Resource<Unstructured<Double>> quotient = UnstructuredResourceApplicative.map(asUnstructured(p), asUnstructured(q), (p$, q$) -> p$ / q$);
  Resource<Linear> approxQuotient = approximate(quotient, secantApproximation(IntervalFunctions.<Unstructured<Double>>byBoundingError(
      1e-6, Duration.SECOND, Duration.HOUR.times(24), errorByOptimization())));

  Resource<Unstructured<Pair<Vector3D, Vector3D>>> positionAndVelocity = resource(Unstructured.timeBased(t -> /* some spice call */ null));
  Resource<Discrete<Pair<Vector3D, Vector3D>>> approxPosVel = approximate(
      positionAndVelocity,
      DiscreteApproximation.<Pair<Vector3D, Vector3D>, Unstructured<Pair<Vector3D, Vector3D>>>discreteApproximation(
          byBoundingError(
              1e-6,
              Duration.SECOND,
              Duration.HOUR.times(24),
              (u, v) -> Math.max(
                  u.getLeft().distance(v.getLeft()) / v.getLeft().getNorm(),
                  u.getRight().distance(v.getRight()) / v.getRight().getNorm()))));

  // Example of the semi-structured "differentiable" resources, and using the additional information to approximate:
  Resource<Differentiable> pDiff = asDifferentiable(p);
  Resource<Differentiable> qDiff = asDifferentiable(q);
  Resource<Differentiable> quotient2 = divide(pDiff, qDiff);
  Resource<Linear> approxQuotient2 = approximate(
      quotient2,
      secantApproximation(byBoundingError(
          1e-6,
          Duration.SECOND,
          Duration.HOUR.times(24),
          errorByQuadraticApproximation())));

  // Another example, pushing a polynomial down to a linear for registering:
  Resource<Polynomial> r;
  Resource<Linear> approxR = approximate(r, SecantApproximation.<Polynomial>secantApproximation(byUniformSampling(Duration.HOUR)));


  // Example of a locking state:

  MutableResource<Discrete<Integer>> importantHardware = DiscreteResources.discreteResource(42);
  MutableResource<Discrete<Optional<Integer>>> importantHardwareLock = DiscreteResources.discreteResource(Optional.empty());
  Resource<Discrete<Boolean>> importantHardwareLockAssertion = assertThat(
      "Important hardware does not change state while locked",
      map(importantHardwareLock, importantHardware, (lock, state) -> lock.map(state::equals).orElse(true)));
  void lockImportantHardware() {
    if (currentValue(importantHardwareLock).isPresent()) {
      throw new IllegalStateException("Already locked!");
    } else {
      set(importantHardwareLock, Optional.of(currentValue(importantHardware)));
    }
  }

  void unlockImportantHardware() {
    set(importantHardwareLock, Optional.empty());
  }

  public enum OnOff { ON, OFF }


  // Clocks, especially working with absolute clocks

  // Pretend planStart is coming from the mission model constructor
  Instant planStart = Instant.parse("2024-01-01T00:00:00Z");
  Instant T1, T2, T3;

  Resource<Absolute<Clock>> currentTime = AbsoluteClockResources.absoluteClock(planStart);

  {
    // How to wait until a certain absolute time:
    ModelActions.waitUntil(when(AbsoluteClockResources.greaterThanOrEquals(currentTime, constant(T1))));
    // Of course, if you have a single "canonical" clock for your mission, you could bake the above into a helper method:
    waitUntil(T1);
  }

  // You can also use an absolute clock to get a clock relative to an absolute point in time:
  Resource<Clock> timeSinceT2 = AbsoluteClockResources.between(constant(T2), currentTime);

  // To get that in "countdown" style, we need a clock that can run backwards:
  Resource<VariableClock> timeUntilT2 = negate(asVariableClock(timeSinceT2));
  // Or we could compute that directly, by first treating T2 as a constant absolute-clock, then subtracting currentTime
  Resource<VariableClock> timeUntilT2_v2 = between(asAbsoluteVariableClock(currentTime), AbsoluteVariableClockResources.constant(T2));

  // Finally, given a relative clock and an absolute clock, we can build an absolute clock.
  // For example, we can start with a countdown timer and the current time to forecast an event.
  // Note that if the launchCountdownTimer is paused, then launchTime will go forward in time in lock-step with currentTime.
  Resource<VariableClock> launchCountdownTimer = resource(new VariableClock(Duration.HOUR, -1));
  Resource<Absolute<VariableClock>> launchTime = subtract(asAbsoluteVariableClock(currentTime), launchCountdownTimer);

  private void waitUntil(Instant time) {
    ModelActions.waitUntil(when(AbsoluteClockResources.greaterThanOrEquals(currentTime, constant(time))));
  }
}
