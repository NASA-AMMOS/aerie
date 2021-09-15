package gov.nasa.jpl.aerie.merlin.framework.resources.discrete;

import gov.nasa.jpl.aerie.merlin.framework.Context;
import gov.nasa.jpl.aerie.merlin.framework.QueryContext;
import gov.nasa.jpl.aerie.merlin.framework.Resource;
import gov.nasa.jpl.aerie.merlin.framework.Scoped;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Querier;
import gov.nasa.jpl.aerie.merlin.protocol.model.DiscreteApproximator;
import gov.nasa.jpl.aerie.merlin.protocol.model.ResourceSolver;
import gov.nasa.jpl.aerie.merlin.protocol.types.DelimitedDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.List;
import java.util.Objects;

public final class DiscreteResourceSolver<$Schema, Value>
    implements ResourceSolver<$Schema, Resource<Value>, Value>
{
  private final Scoped<Context> rootContext;
  private final ValueMapper<Value> mapper;

  public DiscreteResourceSolver(final Scoped<Context> rootContext, final ValueMapper<Value> mapper) {
    this.rootContext = Objects.requireNonNull(rootContext);
    this.mapper = Objects.requireNonNull(mapper);
  }

  @Override
  public Value getDynamics(final Resource<Value> resource, final Querier<? extends $Schema> now) {
    return this.rootContext.setWithin(new QueryContext<>(now), resource::getDynamics);
  }

  @Override
  public <Result> Result approximate(final ApproximatorVisitor<Value, Result> visitor) {
    return visitor.discrete(new DiscreteApproximator<>() {
      @Override
      public Iterable<DelimitedDynamics<SerializedValue>> approximate(final Value value) {
        return List.of(DelimitedDynamics.persistent(DiscreteResourceSolver.this.mapper.serializeValue(value)));
      }

      @Override
      public ValueSchema getSchema() {
        return DiscreteResourceSolver.this.mapper.getValueSchema();
      }
    });
  }
}
