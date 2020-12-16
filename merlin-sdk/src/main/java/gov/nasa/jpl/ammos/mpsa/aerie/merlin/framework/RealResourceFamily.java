package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ConditionType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ResourceSolver;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ResourceFamily;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealCondition;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealDynamics;

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
