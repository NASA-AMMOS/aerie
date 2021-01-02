package gov.nasa.jpl.aerie.merlin.framework.resources.real;

import gov.nasa.jpl.aerie.merlin.protocol.ConditionType;
import gov.nasa.jpl.aerie.merlin.protocol.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.ResourceFamily;
import gov.nasa.jpl.aerie.merlin.protocol.ResourceSolver;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

public final class RealResourceFamily<$Schema>
    implements ResourceFamily<$Schema, RealResource<$Schema>, RealCondition>
{
  private final Map<String, RealResource<$Schema>> resources;

  public RealResourceFamily(final Map<String, RealResource<$Schema>> resources) {
    this.resources = Objects.requireNonNull(resources);
  }

  @Override
  public Map<String, RealResource<$Schema>> getResources() {
    return Collections.unmodifiableMap(this.resources);
  }

  @Override
  public Map<String, UnaryOperator<RealResource<$Schema>>> getUnaryOperators() {
    // TODO: add (x) -> c*x
    return Map.of(
        "negate", $ -> $.scaledBy(-1));
  }

  @Override
  public Map<String, BinaryOperator<RealResource<$Schema>>> getBinaryOperators() {
    return Map.of(
        "add", RealResource::plus,
        "subtract", RealResource::minus);
  }

  @Override
  public Map<String, ConditionType<RealCondition>> getConditionTypes() {
    return Collections.emptyMap();
  }

  @Override
  public ResourceSolver<$Schema, RealResource<$Schema>, RealDynamics, RealCondition> getSolver() {
    return new RealResourceSolver<>();
  }
}
