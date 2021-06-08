package gov.nasa.jpl.aerie.merlin.framework.resources.discrete;

import gov.nasa.jpl.aerie.merlin.framework.Context;
import gov.nasa.jpl.aerie.merlin.framework.Resource;
import gov.nasa.jpl.aerie.merlin.framework.Scoped;
import gov.nasa.jpl.aerie.merlin.protocol.ResourceFamily;
import gov.nasa.jpl.aerie.merlin.protocol.ValueMapper;

import java.util.Collections;
import java.util.Map;

public final class DiscreteResourceFamily<$Schema, Value>
    implements ResourceFamily<$Schema, Resource<Value>>
{
  private final Scoped<Context> rootContext;
  private final ValueMapper<Value> mapper;
  private final Map<String, Resource<Value>> resources;

  public DiscreteResourceFamily(
      final Scoped<Context> rootContext,
      final ValueMapper<Value> mapper,
      final Map<String, Resource<Value>> resources)
  {
    this.rootContext = rootContext;
    this.mapper = mapper;
    this.resources = resources;
  }

  @Override
  public Map<String, Resource<Value>> getResources() {
    return Collections.unmodifiableMap(this.resources);
  }

  @Override
  public DiscreteResourceSolver<$Schema, Value> getSolver() {
    return new DiscreteResourceSolver<>(this.rootContext, this.mapper);
  }
}
