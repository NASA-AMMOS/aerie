package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.profile.Profile;
import gov.nasa.jpl.aerie.constraints.profile.Windows;
import gov.nasa.jpl.aerie.constraints.time.Interval;

import java.util.Set;

public record Changes<V>(Expression<Profile<V>> expression) implements Expression<Windows> {

  @Override
  public Windows evaluate(final SimulationResults results, final Interval bounds, final EvaluationEnvironment environment) {
    return this.expression.evaluate(results, bounds, environment).allEdges();
  }

  @Override
  public void extractResources(final Set<String> names) {
    this.expression.extractResources(names);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(changed %s)",
        prefix,
        this.expression.prettyPrint(prefix + "  ")
    );
  }
}
