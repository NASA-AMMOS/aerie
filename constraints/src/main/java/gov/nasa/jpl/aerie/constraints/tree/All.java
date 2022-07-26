package gov.nasa.jpl.aerie.constraints.tree;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class All implements WindowsExpression {
  public final List<WindowsExpression> expressions;

  @JsonCreator
  public All(@JsonProperty("expressions") final List<WindowsExpression> expressions) {
    this.expressions = expressions;
  }

  public All(final WindowsExpression... expressions) {
    this(List.of(expressions));
  }

  public static All fromExpressions(final List<Expression<Windows>> expressions) {
    return new All(
        expressions.stream().map(
            $ -> (WindowsExpression) $
        ).toList()
    );
  }

  @Override
  public Windows evaluate(final SimulationResults results, final Window bounds, final Map<String, ActivityInstance> environment) {
    Windows windows = new Windows(bounds);
    for (final var expression : this.expressions) {
      windows.intersectWith(
          expression.evaluate(results, bounds, environment)
      );
    }
    return windows;
  }

  @Override
  public void extractResources(final Set<String> names) {
    this.expressions.forEach(expression -> expression.extractResources(names));
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

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof final All o)) return false;

    return Objects.equals(this.expressions, o.expressions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.expressions);
  }
}
