package gov.nasa.jpl.aerie.merlin.framework.resources.discrete;

import gov.nasa.jpl.aerie.merlin.framework.Context;
import gov.nasa.jpl.aerie.merlin.framework.Scoped;
import gov.nasa.jpl.aerie.merlin.protocol.ConditionType;
import gov.nasa.jpl.aerie.merlin.protocol.ResourceFamily;
import gov.nasa.jpl.aerie.merlin.protocol.ValueMapper;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

public final class DiscreteResourceFamily<$Schema, Resource>
    implements ResourceFamily<$Schema, DiscreteResource<Resource>, Set<Resource>>
{
  private final Scoped<Context> rootContext;
  private final ValueMapper<Resource> mapper;
  private final Map<String, DiscreteResource<Resource>> resources;

  public DiscreteResourceFamily(
      final Scoped<Context> rootContext,
      final ValueMapper<Resource> mapper,
      final Map<String, DiscreteResource<Resource>> resources)
  {
    this.rootContext = rootContext;
    this.mapper = mapper;
    this.resources = resources;
  }

  @Override
  public Map<String, DiscreteResource<Resource>> getResources() {
    return Collections.unmodifiableMap(this.resources);
  }

  @Override
  public Map<String, UnaryOperator<DiscreteResource<Resource>>> getUnaryOperators() {
    return Collections.emptyMap();
  }

  @Override
  public Map<String, BinaryOperator<DiscreteResource<Resource>>> getBinaryOperators() {
    return Collections.emptyMap();
  }

  @Override
  public Map<String, ConditionType<Set<Resource>>> getConditionTypes() {
    return Collections.emptyMap();
  }

  @Override
  public DiscreteResourceSolver<$Schema, Resource> getSolver() {
    return new DiscreteResourceSolver<>(this.rootContext, this.mapper);
  }
}
