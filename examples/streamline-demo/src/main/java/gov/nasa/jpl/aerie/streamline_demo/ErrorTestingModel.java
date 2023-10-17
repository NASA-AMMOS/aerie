package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.BooleanValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.IntegerValueMapper;
import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.signalling;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad.map;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial.polynomial;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.asPolynomial;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.clamp;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.constant;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.multiply;

public class ErrorTestingModel {
  public CellResource<Discrete<Boolean>> bool = cellResource(discrete(true));
  public CellResource<Discrete<Integer>> counter = cellResource(discrete(5));
  public CellResource<Polynomial> continuous = cellResource(polynomial(1));
  public Resource<Polynomial> derived = multiply(
      continuous,
      asPolynomial(map(counter, c -> (double) c)),
      asPolynomial(map(bool, $ -> $ ? 1.0 : -1.0)));

  public CellResource<Polynomial> upperBound = cellResource(polynomial(5));
  public CellResource<Polynomial> lowerBound = cellResource(polynomial(-5));
  public Resource<Polynomial> clamped = signalling(clamp(constant(10), lowerBound, upperBound));

  public ErrorTestingModel(final Registrar registrar, final Configuration config) {
    registrar.discrete("errorTesting/bool", bool, new BooleanValueMapper());
    registrar.discrete("errorTesting/counter", counter, new IntegerValueMapper());
    // Explicitly register a name for continuous, because the derived linearized resource can't have effects
    continuous.registerName("errorTesting/continuous");
    registrar.real("errorTesting/continuous", DataModel.linearize(continuous));
    registrar.real("errorTesting/derived", DataModel.linearize(derived));
    lowerBound.registerName("errorTesting/lowerBound");
    registrar.real("errorTesting/lowerBound", DataModel.linearize(lowerBound));
    upperBound.registerName("errorTesting/upperBound");
    registrar.real("errorTesting/upperBound", DataModel.linearize(upperBound));
    clamped.registerName("errorTesting/clamped");
    registrar.real("errorTesting/clamped", DataModel.linearize(clamped));
  }
}
