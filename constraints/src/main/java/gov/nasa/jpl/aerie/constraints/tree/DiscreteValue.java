package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.DiscreteProfile;
import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.AbsoluteInterval;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Segment;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Objects;
import java.util.Set;

public record DiscreteValue(
    SerializedValue value,
    AbsoluteInterval interval
) implements Expression<DiscreteProfile> {

  public DiscreteValue(final SerializedValue value) {
    this(value, AbsoluteInterval.FOREVER);
  }

  @Override
  public DiscreteProfile evaluate(final SimulationResults results, final Interval bounds, final EvaluationEnvironment environment) {
    final Interval relativeInterval = interval.toRelative(results.planStart);
    return new DiscreteProfile(Segment.of(Interval.intersect(bounds, relativeInterval), this.value));
  }

  @Override
  public void extractResources(final Set<String> names) {
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(value %s)",
        prefix,
        this.value
    );
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof DiscreteValue)) return false;
    final var o = (DiscreteValue) obj;

    return Objects.equals(this.value, o.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.value);
  }
}
