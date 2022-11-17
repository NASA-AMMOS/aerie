package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.profile.Profile;
import gov.nasa.jpl.aerie.constraints.profile.Windows;
import gov.nasa.jpl.aerie.constraints.time.Interval;

import java.util.Set;

public record Not(Expression<Profile<Boolean>> expression) implements Expression<Windows> {

  @Override
  public Windows evaluate(final SimulationResults results, final Interval bounds, final EvaluationEnvironment environment) {
    return ((Windows) this.expression.evaluate(results, bounds, environment)).not();
  }

  @Override
  public void extractResources(final Set<String> names) {
    this.expression.extractResources(names);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(not %s)",
        prefix,
        this.expression.prettyPrint(prefix + "  ")
    );
  }
}
