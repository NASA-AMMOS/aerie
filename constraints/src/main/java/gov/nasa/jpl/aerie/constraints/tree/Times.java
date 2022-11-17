package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.profile.LinearEquation;
import gov.nasa.jpl.aerie.constraints.profile.LinearProfile;
import gov.nasa.jpl.aerie.constraints.profile.Profile;
import gov.nasa.jpl.aerie.constraints.time.Interval;

import java.util.Set;

public record Times(
    Expression<Profile<LinearEquation>> profile,
    double multiplier) implements Expression<Profile<LinearEquation>> {

  @Override
  public LinearProfile evaluate(final SimulationResults results, final Interval bounds, final EvaluationEnvironment environment) {
    return ((LinearProfile) this.profile.evaluate(results, bounds, environment)).times(this.multiplier);
  }

  @Override
  public void extractResources(final Set<String> names) {
    this.profile.extractResources(names);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(* %s %s)",
        prefix,
        this.profile.prettyPrint(prefix + "  "),
        String.format("\n%s  %s", prefix, this.multiplier)
    );
  }
}
