package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.LinearBoundaryConsistencySolver;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.LinearBoundaryConsistencySolver.Domain;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.eraseExpiry;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.forward;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad.*;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Naming.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.choose;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear.linear;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.LinearBoundaryConsistencySolver.Comparison.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.LinearBoundaryConsistencySolver.LinearExpression.lx;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.add;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.clamp;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.constant;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.differentiate;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.greaterThanOrEquals;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.lessThanOrEquals;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.max;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.polynomialMutableResource;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.subtract;

public class DataModel {
  public MutableResource<Polynomial> desiredRateA = PolynomialResources.polynomialMutableResource(0);
  public MutableResource<Polynomial> desiredRateB = PolynomialResources.polynomialMutableResource(0);
  public MutableResource<Polynomial> desiredRateC = PolynomialResources.polynomialMutableResource(0);
  public MutableResource<Polynomial> upperBoundOnTotalVolume = PolynomialResources.polynomialMutableResource(10);

  public Resource<Polynomial> actualRateA, actualRateB, actualRateC;
  public MutableResource<Polynomial> volumeA, volumeB, volumeC;
  public Resource<Polynomial> totalVolume;

  public DataModel(final Registrar registrar, final Configuration config) {
    // Adding a "derivative" operation makes the constraint problem no longer solvable without backtracking.
    // Instead of solving for it directly, we'll solve in two steps, one for rate, one for volume.

    // Set up the rate solver
    var rateSolver = new LinearBoundaryConsistencySolver("DataModel Rate Solver");
    var rateA = rateSolver.variable("rateA", Domain::upperBound);
    var rateB = rateSolver.variable("rateB", Domain::upperBound);
    var rateC = rateSolver.variable("rateC", Domain::upperBound);
    this.actualRateA = rateA.resource();
    this.actualRateB = rateB.resource();
    this.actualRateC = rateC.resource();

    // Use a simple feedback loop on volumes to do the integration and clamping.
    this.volumeA = PolynomialResources.polynomialMutableResource(0);
    this.volumeB = PolynomialResources.polynomialMutableResource(0);
    this.volumeC = PolynomialResources.polynomialMutableResource(0);
    var clampedVolumeA = clamp(this.volumeA, constant(0), upperBoundOnTotalVolume);
    var volumeB_ub = subtract(upperBoundOnTotalVolume, clampedVolumeA);
    var clampedVolumeB = clamp(this.volumeB, constant(0), volumeB_ub);
    var volumeC_ub = subtract(volumeB_ub, clampedVolumeB);
    var clampedVolumeC = clamp(this.volumeC, constant(0), volumeC_ub);
    var correctedVolumeA = map(clampedVolumeA, actualRateA, (v, r) -> r.integral(v.extract()));
    var correctedVolumeB = map(clampedVolumeB, actualRateB, (v, r) -> r.integral(v.extract()));
    var correctedVolumeC = map(clampedVolumeC, actualRateC, (v, r) -> r.integral(v.extract()));
    // Use the corrected integral values to set volumes, but erase expiry information in the process to avoid loops:
    forward(eraseExpiry(correctedVolumeA), this.volumeA);
    forward(eraseExpiry(correctedVolumeB), this.volumeB);
    forward(eraseExpiry(correctedVolumeC), this.volumeC);

    // Integrate the actual rates.
    totalVolume = add(this.volumeA, this.volumeB, this.volumeC);

    // Then use the solver to adjust the actual rates

    // When full, we never write more than the upper bound will tolerate, in total
    var isFull = greaterThanOrEquals(totalVolume, upperBoundOnTotalVolume);
    var totalRate_ub = choose(isFull, differentiate(upperBoundOnTotalVolume), constant(Double.POSITIVE_INFINITY));
    rateSolver.declare(lx(rateA).add(lx(rateB)).add(lx(rateC)), LessThanOrEquals, lx(totalRate_ub));

    // We only exceed the desired rate when we try to delete from an empty bucket.
    var isEmptyA = lessThanOrEquals(this.volumeA, 0);
    var isEmptyB = lessThanOrEquals(this.volumeB, 0);
    var isEmptyC = lessThanOrEquals(this.volumeC, 0);
    var rateA_ub = choose(isEmptyA, max(desiredRateA, constant(0)), desiredRateA);
    var rateB_ub = choose(isEmptyB, max(desiredRateB, constant(0)), desiredRateB);
    var rateC_ub = choose(isEmptyC, max(desiredRateC, constant(0)), desiredRateC);
    rateSolver.declare(lx(rateA), LessThanOrEquals, lx(rateA_ub));
    rateSolver.declare(lx(rateB), LessThanOrEquals, lx(rateB_ub));
    rateSolver.declare(lx(rateC), LessThanOrEquals, lx(rateC_ub));

    // We cannot delete from an empty bucket
    var rateA_lb = choose(isEmptyA, constant(0), constant(Double.NEGATIVE_INFINITY));
    var rateB_lb = choose(isEmptyB, constant(0), constant(Double.NEGATIVE_INFINITY));
    var rateC_lb = choose(isEmptyC, constant(0), constant(Double.NEGATIVE_INFINITY));
    rateSolver.declare(lx(rateA), GreaterThanOrEquals, lx(rateA_lb));
    rateSolver.declare(lx(rateB), GreaterThanOrEquals, lx(rateB_lb));
    rateSolver.declare(lx(rateC), GreaterThanOrEquals, lx(rateC_lb));

    registerStates(registrar, config);
  }

  private void registerStates(Registrar registrar, Configuration config) {
    registrar.real("desiredRateA", linearize(desiredRateA));
    registrar.real("desiredRateB", linearize(desiredRateB));
    registrar.real("desiredRateC", linearize(desiredRateC));

    registrar.real("actualRateA", linearize(actualRateA));
    registrar.real("actualRateB", linearize(actualRateB));
    registrar.real("actualRateC", linearize(actualRateC));

    registrar.real("volumeA", linearize(volumeA));
    registrar.real("volumeB", linearize(volumeB));
    registrar.real("volumeC", linearize(volumeC));
    registrar.real("totalVolume", linearize(totalVolume));
    registrar.real("maxVolume", linearize(upperBoundOnTotalVolume));
  }

  static Resource<Linear> linearize(Resource<Polynomial> p) {
    var result = map(p, p$ -> {
      if (p$.degree() <= 1) {
        return linear(p$.getCoefficient(0), p$.getCoefficient(1));
      } else {
        throw new IllegalStateException("Resource was super-linear");
      }
    });
    // Reverse the normal direction of naming, so that names registered for result propagate back to p
    name(p, "%s", result);
    return result;
  }
}
