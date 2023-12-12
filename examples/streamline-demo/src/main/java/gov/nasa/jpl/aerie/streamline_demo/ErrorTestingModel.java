package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.BooleanValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.IntegerValueMapper;
import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources;

import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.discreteMutableResource;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad.map;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.asPolynomial;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.clamp;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.constant;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.multiply;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.polynomialMutableResource;

public class ErrorTestingModel {
  public MutableResource<Discrete<Boolean>> bool = DiscreteResources.discreteMutableResource(true);
  public MutableResource<Discrete<Integer>> counter = DiscreteResources.discreteMutableResource(5);
  public MutableResource<Polynomial> continuous = PolynomialResources.polynomialMutableResource(1);
  public Resource<Polynomial> derived = multiply(
      continuous,
      asPolynomial(map(counter, c -> (double) c)),
      asPolynomial(map(bool, $ -> $ ? 1.0 : -1.0)));

  public MutableResource<Polynomial> upperBound = PolynomialResources.polynomialMutableResource(5);
  public MutableResource<Polynomial> lowerBound = PolynomialResources.polynomialMutableResource(-5);
  public Resource<Polynomial> clamped = clamp(constant(10), lowerBound, upperBound);

  public ErrorTestingModel(final Registrar registrar, final Configuration config) {
    registrar.discrete("errorTesting/bool", bool, new BooleanValueMapper());
    registrar.discrete("errorTesting/counter", counter, new IntegerValueMapper());
    // Explicitly register a name for continuous, because the derived linearized resource can't have effects
    registrar.real("errorTesting/continuous", DataModel.linearize(continuous));
    registrar.real("errorTesting/derived", DataModel.linearize(derived));
    registrar.real("errorTesting/lowerBound", DataModel.linearize(lowerBound));
    registrar.real("errorTesting/upperBound", DataModel.linearize(upperBound));
    registrar.real("errorTesting/clamped", DataModel.linearize(clamped));
  }
}
