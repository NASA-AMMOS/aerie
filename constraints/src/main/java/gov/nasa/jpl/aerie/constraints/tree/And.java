package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.List;
import java.util.Map;

public final class And implements Expression<Windows> {
  public final List<Expression<Windows>> expressions;

  @SafeVarargs
  public And(final Expression<Windows>... expressions) {
    this.expressions = List.of(expressions);
  }

  @Override
  public Windows evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment) {
    Windows windows = new Windows(results.bounds);
    for (final var expression : this.expressions) {
      windows.intersectWith(
          expression.evaluate(results, environment)
      );
    }
    return windows;
  }

  @Override
  public String prettyPrint(final String prefix) {
    final var builder = new StringBuilder();
    builder.append("\n")
           .append(prefix)
           .append("(and ");

    final var iter = this.expressions.iterator();
    while (iter.hasNext()) {
      builder.append(iter.next().prettyPrint(prefix + "  "));
      if (iter.hasNext()) builder.append(" ");
    }

    builder.append(")");
    return builder.toString();
  }
}
