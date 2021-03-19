package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.Profile;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.Map;

public final class Changed<P extends Profile<P>> implements Expression<Windows> {
  private final Expression<P> profile;

  public Changed(final Expression<P> profile) {
    this.profile = profile;
  }

  @Override
  public Windows evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment) {
    return this.profile.evaluate(results, environment).changePoints(results.bounds);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(changed %s)",
        prefix,
        this.profile.prettyPrint(prefix + "  ")
    );
  }
}
