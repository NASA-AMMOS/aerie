package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Window;

import java.util.List;
import java.util.Map;

public final class SimulationResults {
  public final Window bounds;
  public final List<ActivityInstance> activities;
  public final Map<String, LinearProfile> realProfiles;
  public final Map<String, DiscreteProfile> discreteProfiles;

  public SimulationResults(
      final Window bounds,
      final List<ActivityInstance> activities,
      final Map<String, LinearProfile> realProfiles,
      final Map<String, DiscreteProfile> discreteProfiles
  ) {
    this.bounds = bounds;
    this.activities = activities;
    this.realProfiles = realProfiles;
    this.discreteProfiles = discreteProfiles;
  }
}
