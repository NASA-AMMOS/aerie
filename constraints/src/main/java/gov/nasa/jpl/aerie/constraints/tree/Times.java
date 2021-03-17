package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;

import java.util.Map;

public final class Times implements Expression<LinearProfile> {
  private final Expression<LinearProfile> profile;
  private final double multiplier;

  public Times(final Expression<LinearProfile> profile, final double multiplier) {
    this.profile = profile;
    this.multiplier = multiplier;
  }

  @Override
  public LinearProfile evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment) {
    return this.profile.evaluate(results, environment).times(this.multiplier);
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
