package gov.nasa.jpl.aerie.merlin.framework.resources.real;

import gov.nasa.jpl.aerie.merlin.framework.Context;
import gov.nasa.jpl.aerie.merlin.framework.Scoped;
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
    implements ResourceFamily<$Schema, RealResource, RealCondition>
{
  private final Scoped<Context> rootContext;
  private final Map<String, RealResource> resources;

  public RealResourceFamily(final Scoped<Context> rootContext, final Map<String, RealResource> resources) {
    this.rootContext = Objects.requireNonNull(rootContext);
    this.resources = Objects.requireNonNull(resources);
  }

  @Override
  public Map<String, RealResource> getResources() {
    return Collections.unmodifiableMap(this.resources);
  }

  @Override
  public Map<String, UnaryOperator<RealResource>> getUnaryOperators() {
    // TODO: add (x) -> c*x
    return Map.of(
        "negate", $ -> $.scaledBy(-1));
  }

  @Override
  public Map<String, BinaryOperator<RealResource>> getBinaryOperators() {
    return Map.of(
        "add", RealResource::plus,
        "subtract", RealResource::minus);
  }

  @Override
  public Map<String, ConditionType<RealCondition>> getConditionTypes() {
    return Collections.emptyMap();
  }

  @Override
  public ResourceSolver<$Schema, RealResource, RealDynamics, RealCondition> getSolver() {
    return new RealResourceSolver<>(this.rootContext);
  }
}
