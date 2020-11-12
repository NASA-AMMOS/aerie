package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealDynamics;

import java.util.Objects;

public abstract class SolvableDynamics<Result> {
  public abstract Result solve(final Visitor visitor);

  public interface Visitor {
    Double real(final RealDynamics dynamics);

    <ResourceType>
    ResourceType discrete(final ResourceType fact);
  }

  public static SolvableDynamics<Double> real(final RealDynamics dynamics) {
    Objects.requireNonNull(dynamics);

    return new SolvableDynamics<>() {
      @Override
      public Double solve(final Visitor visitor) {
        return visitor.real(dynamics);
      }
    };
  }

  public static <ResourceType> SolvableDynamics<ResourceType> discrete(final ResourceType fact) {
    Objects.requireNonNull(fact);

    return new SolvableDynamics<>() {
      @Override
      public ResourceType solve(final Visitor visitor) {
        return visitor.discrete(fact);
      }
    };
  }
}
