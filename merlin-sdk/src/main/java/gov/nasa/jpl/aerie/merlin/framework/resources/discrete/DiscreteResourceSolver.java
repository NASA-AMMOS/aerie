package gov.nasa.jpl.aerie.merlin.framework.resources.discrete;

import gov.nasa.jpl.aerie.merlin.protocol.DelimitedDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.DiscreteApproximator;
import gov.nasa.jpl.aerie.merlin.protocol.ResourceSolver;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.ValueSchema;
import gov.nasa.jpl.aerie.merlin.timeline.History;
import gov.nasa.jpl.aerie.time.Duration;
import gov.nasa.jpl.aerie.time.Window;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class DiscreteResourceSolver<$Schema, Resource>
    implements ResourceSolver<$Schema, DiscreteResource<$Schema, Resource>, Resource, Set<Resource>>
{
  private final ValueMapper<Resource> mapper;

  public DiscreteResourceSolver(final ValueMapper<Resource> mapper) {
    this.mapper = Objects.requireNonNull(mapper);
  }

  @Override
  public DelimitedDynamics<Resource> getDynamics(
      final DiscreteResource<$Schema, Resource> resource,
      final History<? extends $Schema> now)
  {
    return resource.getDynamics(now);
  }

  @Override
  public <Result> Result approximate(final ApproximatorVisitor<Resource, Result> visitor) {
    return visitor.discrete(new DiscreteApproximator<>() {
      @Override
      public Iterable<DelimitedDynamics<SerializedValue>> approximate(final Resource value) {
        return List.of(DelimitedDynamics.persistent(DiscreteResourceSolver.this.mapper.serializeValue(value)));
      }

      @Override
      public ValueSchema getSchema() {
        return DiscreteResourceSolver.this.mapper.getValueSchema();
      }
    });
  }

  @Override
  public Optional<Duration> firstSatisfied(final Resource value, final Set<Resource> values, final Window selection) {
    if (values.contains(value)) {
      return Optional.of(selection.start);
    } else {
      return Optional.empty();
    }
  }
}
