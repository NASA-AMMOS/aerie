package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Interval;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SimulationResults {
  public final Interval bounds;
  public final List<ActivityInstance> activities;
  public final Map<String, LinearProfile> realProfiles;
  public final Map<String, DiscreteProfile> discreteProfiles;

  public SimulationResults(
      final Interval bounds,
      final List<ActivityInstance> activities,
      final Map<String, LinearProfile> realProfiles,
      final Map<String, DiscreteProfile> discreteProfiles
  ) {
    this.bounds = bounds;
    this.activities = activities;
    this.realProfiles = realProfiles;
    this.discreteProfiles = discreteProfiles;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SimulationResults)) return false;
    final var o = (SimulationResults)obj;

    return Objects.equals(this.bounds, o.bounds) &&
           Objects.equals(this.activities, o.activities) &&
           Objects.equals(this.realProfiles, o.realProfiles) &&
           Objects.equals(this.discreteProfiles, o.discreteProfiles);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.bounds, this.activities, this.realProfiles, this.discreteProfiles);
  }
}
