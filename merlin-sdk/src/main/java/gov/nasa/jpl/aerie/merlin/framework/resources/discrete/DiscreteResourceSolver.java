package gov.nasa.jpl.aerie.merlin.framework.resources.discrete;

import gov.nasa.jpl.aerie.merlin.framework.Context;
import gov.nasa.jpl.aerie.merlin.framework.QueryContext;
import gov.nasa.jpl.aerie.merlin.framework.Scoped;
import gov.nasa.jpl.aerie.merlin.protocol.Checkpoint;
import gov.nasa.jpl.aerie.merlin.protocol.DelimitedDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.DiscreteApproximator;
import gov.nasa.jpl.aerie.merlin.protocol.ResourceSolver;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.ValueSchema;

import java.util.List;
import java.util.Objects;

public final class DiscreteResourceSolver<$Schema, Resource>
    implements ResourceSolver<$Schema, DiscreteResource<Resource>, Resource>
{
  private final Scoped<Context> rootContext;
  private final ValueMapper<Resource> mapper;

  public DiscreteResourceSolver(final Scoped<Context> rootContext, final ValueMapper<Resource> mapper) {
    this.rootContext = Objects.requireNonNull(rootContext);
    this.mapper = Objects.requireNonNull(mapper);
  }

  @Override
  public Resource getDynamics(
      final DiscreteResource<Resource> resource,
      final Checkpoint<? extends $Schema> now)
  {
    return this.rootContext.setWithin(new QueryContext<>(now), resource::getDynamics);
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
}
