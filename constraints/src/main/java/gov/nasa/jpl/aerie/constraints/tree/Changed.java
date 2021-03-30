package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.Profile;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.Map;
import java.util.Objects;

public final class Changed<P extends Profile<?>> implements Expression<Windows> {
  private final Expression<P> expression;

  public Changed(final Expression<P> expression) {
    this.expression = expression;
  }

  @Override
  public Windows evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment) {
    return this.expression.evaluate(results, environment).changePoints(results.bounds);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(changed %s)",
        prefix,
        this.expression.prettyPrint(prefix + "  ")
    );
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Changed)) return false;
    final var o = (Changed<?>)obj;

    return Objects.equals(this.expression, o.expression);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.expression);
  }
}
