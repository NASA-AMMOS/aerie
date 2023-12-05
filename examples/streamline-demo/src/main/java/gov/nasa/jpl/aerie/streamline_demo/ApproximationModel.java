package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.*;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial;

import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad.map;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.Approximation.approximate;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.DifferentiableResources.asDifferentiable;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.IntervalFunctions.byBoundingError;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.SecantApproximation.ErrorEstimates.errorByOptimization;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.SecantApproximation.ErrorEstimates.errorByQuadraticApproximation;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.SecantApproximation.secantApproximation;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.*;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.*;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MINUTE;

public class ApproximationModel {
  private static final double TOLERANCE = 1e-2;
  public CellResource<Polynomial> polynomial = polynomialCellResource(1);

  public Resource<Linear> assumedLinear = assumeLinear(polynomial);

  public Resource<Linear> uniformApproximation = approximate(
      polynomial,
      secantApproximation(IntervalFunctions.<Polynomial>byUniformSampling(MINUTE)));

  public Resource<Linear> differentiableApproximation = approximate(
      asDifferentiable(polynomial),
      secantApproximation(byBoundingError(
          TOLERANCE,
          SECOND,
          HOUR.times(24),
          errorByQuadraticApproximation())));

  public Resource<Linear> directApproximation = approximate(
      polynomial,
      secantApproximation(IntervalFunctions.<Polynomial>byBoundingError(
          TOLERANCE,
          SECOND,
          HOUR.times(24),
          errorByOptimization())));

  public Resource<Polynomial> divisor = constant(2);
  public Resource<Unstructured<Double>> polynomialOverTwo = UnstructuredResources.map(polynomial, divisor, (p, q) -> p / q);

  public Resource<Linear> uniformApproximation2 = approximate(
      polynomialOverTwo,
      secantApproximation(IntervalFunctions.<Unstructured<Double>>byUniformSampling(MINUTE)));

  public Resource<Linear> directApproximation2 = approximate(
      polynomialOverTwo,
      secantApproximation(IntervalFunctions.<Unstructured<Double>>byBoundingError(
          TOLERANCE,
          SECOND,
          HOUR.times(24),
          errorByOptimization())));

  public ApproximationModel(final Registrar registrar, final Configuration config) {
    registrar.real("approximation/assumedLinear", assumedLinear);
    registrar.real("approximation/uniform", uniformApproximation);
    registrar.real("approximation/differentiable", differentiableApproximation);
    registrar.real("approximation/direct", directApproximation);
    registrar.real("approximation/uniform2", uniformApproximation2);
    registrar.real("approximation/direct2", directApproximation2);
  }
}
