package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ConditionType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ResourceFamily;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ValueMapper;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

public final class DiscreteResourceFamily<$Schema, Resource>
    implements ResourceFamily<$Schema, DiscreteResource<$Schema, Resource>, Set<Resource>>
{
  private final ValueMapper<Resource> mapper;
  private final Map<String, DiscreteResource<$Schema, Resource>> resources;

  public DiscreteResourceFamily(
      final ValueMapper<Resource> mapper,
      final Map<String, DiscreteResource<$Schema, Resource>> resources)
  {
    this.mapper = mapper;
    this.resources = resources;
  }

  @Override
  public Map<String, DiscreteResource<$Schema, Resource>> getResources() {
    return Collections.unmodifiableMap(this.resources);
  }

  @Override
  public Map<String, UnaryOperator<DiscreteResource<$Schema, Resource>>> getUnaryOperators() {
    return Collections.emptyMap();
  }

  @Override
  public Map<String, BinaryOperator<DiscreteResource<$Schema, Resource>>> getBinaryOperators() {
    return Collections.emptyMap();
  }

  @Override
  public Map<String, ConditionType<Set<Resource>>> getConditionTypes() {
    return Collections.emptyMap();
  }

  @Override
  public DiscreteResourceSolver<$Schema, Resource> getSolver() {
    return new DiscreteResourceSolver<>(this.mapper);
  }
}
