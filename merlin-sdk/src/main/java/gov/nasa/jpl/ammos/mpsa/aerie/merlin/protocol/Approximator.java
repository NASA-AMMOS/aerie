package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import java.util.Objects;

public interface Approximator<Dynamics> {
  <Result> Result match(Visitor<Dynamics, Result> visitor);

  interface Visitor<Dynamics, Result> {
    Result real(RealApproximator<Dynamics> approximator);
    Result discrete(DiscreteApproximator<Dynamics> approximator);
  }

  static <Dynamics> Approximator<Dynamics> real(final RealApproximator<Dynamics> approximator) {
    Objects.requireNonNull(approximator);

    return new Approximator<>() {
      @Override
      public <Result> Result match(final Visitor<Dynamics, Result> visitor) {
        return visitor.real(approximator);
      }
    };
  }

  static <Dynamics> Approximator<Dynamics> discrete(final DiscreteApproximator<Dynamics> approximator) {
    Objects.requireNonNull(approximator);

    return new Approximator<>() {
      @Override
      public <Result> Result match(final Visitor<Dynamics, Result> visitor) {
        return visitor.discrete(approximator);
      }
    };
  }
}
