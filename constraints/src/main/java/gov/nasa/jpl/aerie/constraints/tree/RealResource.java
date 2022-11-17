package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.InputMismatchException;
import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.profile.LinearEquation;
import gov.nasa.jpl.aerie.constraints.profile.LinearProfile;
import gov.nasa.jpl.aerie.constraints.profile.Profile;
import gov.nasa.jpl.aerie.constraints.time.Interval;

import java.util.Set;

public record RealResource(String name) implements Expression<Profile<LinearEquation>> {

  @Override
  public LinearProfile evaluate(final SimulationResults results, final Interval bounds, final EvaluationEnvironment environment) {
    if (results.realProfiles.containsKey(name)) {
      return results.realProfiles.get(name)::stream;
    } else if (environment.realExternalProfiles().containsKey(name)) {
      return environment.realExternalProfiles().get(name)::stream;
    } else if (results.discreteProfiles.containsKey(name)) {
      return LinearProfile.fromDiscrete(results.discreteProfiles.get(name));
    } else if (environment.discreteExternalProfiles().containsKey(name)) {
      return LinearProfile.fromDiscrete(environment.discreteExternalProfiles().get(name));
    }

    throw new InputMismatchException(String.format("%s is not a valid resource", this.name));
  }

  @Override
  public void extractResources(final Set<String> names) {
    names.add(this.name);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(resource %s)",
        prefix,
        this.name
    );
  }
}
