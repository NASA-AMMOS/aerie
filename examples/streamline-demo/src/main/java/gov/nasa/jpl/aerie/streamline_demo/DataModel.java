package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.BooleanValueMapper;
import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.LinearBoundaryConsistencySolver;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.LinearBoundaryConsistencySolver.Domain;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiring.neverExpiring;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.wheneverDynamicsChange;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.assertThat;
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
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.polynomialCellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.subtract;

public class DataModel {
  public CellResource<Polynomial> desiredRateA = polynomialCellResource(0);
  public CellResource<Polynomial> desiredRateB = polynomialCellResource(0);
  public CellResource<Polynomial> desiredRateC = polynomialCellResource(0);
  public CellResource<Polynomial> upperBoundOnTotalVolume = polynomialCellResource(10);

  public Resource<Polynomial> actualRateA, actualRateB, actualRateC;
  public CellResource<Polynomial> volumeA, volumeB, volumeC;
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
    this.volumeA = polynomialCellResource(0);
    this.volumeB = polynomialCellResource(0);
    this.volumeC = polynomialCellResource(0);
    var clampedVolumeA = clamp(this.volumeA, constant(0), upperBoundOnTotalVolume);
    var volumeB_ub = subtract(upperBoundOnTotalVolume, clampedVolumeA);
    var clampedVolumeB = clamp(this.volumeB, constant(0), volumeB_ub);
    var volumeC_ub = subtract(volumeB_ub, clampedVolumeB);
    var clampedVolumeC = clamp(this.volumeC, constant(0), volumeC_ub);
    var correctedVolumeA = bind(clampedVolumeA, v -> map(actualRateA, r -> r.integral(v.extract())));
    var correctedVolumeB = bind(clampedVolumeB, v -> map(actualRateB, r -> r.integral(v.extract())));
    var correctedVolumeC = bind(clampedVolumeC, v -> map(actualRateC, r -> r.integral(v.extract())));
    // Use the corrected integral values to set volumes, but erase expiry information in the process to avoid loops:
    wheneverDynamicsChange(correctedVolumeA, v -> this.volumeA.emit($ -> v.map(p -> neverExpiring(p.data()))));
    wheneverDynamicsChange(correctedVolumeB, v -> this.volumeB.emit($ -> v.map(p -> neverExpiring(p.data()))));
    wheneverDynamicsChange(correctedVolumeC, v -> this.volumeC.emit($ -> v.map(p -> neverExpiring(p.data()))));

    // Integrate the actual rates.
    totalVolume = add(this.volumeA, this.volumeB, this.volumeC);

    // Then use the solver to adjust the actual rates

    // When full, we never write more than the upper bound will tolerate, in total
    var isFull = greaterThanOrEquals(totalVolume, upperBoundOnTotalVolume);
    var totalRate_ub = bind(isFull, f -> f.extract() ? differentiate(upperBoundOnTotalVolume) : constant(Double.POSITIVE_INFINITY));
    rateSolver.declare(lx(rateA).add(lx(rateB)).add(lx(rateC)), LessThanOrEquals, lx(totalRate_ub));

    // We only exceed the desired rate when we try to delete from an empty bucket.
    var isEmptyA = lessThanOrEquals(this.volumeA, 0);
    var isEmptyB = lessThanOrEquals(this.volumeB, 0);
    var isEmptyC = lessThanOrEquals(this.volumeC, 0);
    var rateA_ub = bind(isEmptyA, e -> e.extract() ? max(desiredRateA, constant(0)) : desiredRateA);
    var rateB_ub = bind(isEmptyB, e -> e.extract() ? max(desiredRateB, constant(0)) : desiredRateB);
    var rateC_ub = bind(isEmptyC, e -> e.extract() ? max(desiredRateC, constant(0)) : desiredRateC);
    rateSolver.declare(lx(rateA), LessThanOrEquals, lx(rateA_ub));
    rateSolver.declare(lx(rateB), LessThanOrEquals, lx(rateB_ub));
    rateSolver.declare(lx(rateC), LessThanOrEquals, lx(rateC_ub));

    // We cannot delete from an empty bucket
    var rateA_lb = bind(isEmptyA, e -> e.extract() ? constant(0) : constant(Double.NEGATIVE_INFINITY));
    var rateB_lb = bind(isEmptyB, e -> e.extract() ? constant(0) : constant(Double.NEGATIVE_INFINITY));
    var rateC_lb = bind(isEmptyC, e -> e.extract() ? constant(0) : constant(Double.NEGATIVE_INFINITY));
    rateSolver.declare(lx(rateA), GreaterThanOrEquals, lx(rateA_lb));
    rateSolver.declare(lx(rateB), GreaterThanOrEquals, lx(rateB_lb));
    rateSolver.declare(lx(rateC), GreaterThanOrEquals, lx(rateC_lb));

    registerStates(registrar, config);
  }

  private void registerStates(Registrar registrar, Configuration config) {
    if (config.traceResources) registrar.setTrace();
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
    registrar.discrete(
        "totalVolumeConstraint",
        assertThat(
            "Total volume must not exceed upper bound.",
            lessThanOrEquals(totalVolume, upperBoundOnTotalVolume)),
        new BooleanValueMapper());
    registrar.clearTrace();
  }

  static Resource<Linear> linearize(Resource<Polynomial> p) {
    return map(p, p$ -> {
      if (p$.degree() <= 1) {
        return linear(p$.getCoefficient(0), p$.getCoefficient(1));
      } else {
        throw new IllegalStateException("Resource was super-linear");
      }
    });
  }
}
