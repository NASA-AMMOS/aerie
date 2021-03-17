package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;

import java.util.Map;

public final class Rate implements Expression<LinearProfile> {
  private final Expression<LinearProfile> profile;

  public Rate(final Expression<LinearProfile> profile) {
    this.profile = profile;
  }


  @Override
  public LinearProfile evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment) {
    return this.profile.evaluate(results, environment).rate();
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "(rate-of %s)",
        this.profile.prettyPrint(prefix + "  ")
    );
  }
}
