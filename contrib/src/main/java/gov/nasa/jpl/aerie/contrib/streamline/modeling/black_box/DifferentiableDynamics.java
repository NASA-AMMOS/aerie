package gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box;

import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.Differentiable.differentiable;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial.polynomial;

public final class DifferentiableDynamics {
  private DifferentiableDynamics() {}

  public static Differentiable constant(double value) {
    return asDifferentiable(polynomial(value));
  }

  public static Differentiable asDifferentiable(Polynomial polynomial) {
    return new Differentiable() {
      @Override
      public Differentiable derivative() {
        return asDifferentiable(polynomial.derivative());
      }

      @Override
      public Double extract() {
        return polynomial.extract();
      }

      @Override
      public Differentiable step(final Duration t) {
        return asDifferentiable(polynomial.step(t));
      }
    };
  }

  public static Differentiable cos(Differentiable argument) {
    return differentiable(argument, Math::cos, d -> sin(d).multiply(-1));
  }

  public static Differentiable sin(Differentiable argument) {
    return differentiable(argument, Math::sin, DifferentiableDynamics::cos);
  }

  public static Differentiable exp(Differentiable argument) {
    return differentiable(argument, Math::exp, DifferentiableDynamics::exp);
  }
}
