package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Spans;

import java.util.Map;

/** A container for additional context needed for Constraints AST evaluation. */
public record EvaluationEnvironment(
    Map<String, ActivityInstance> activityInstances,
    Map<String, Spans> spansInstances,
    Map<String, LinearProfile> realExternalProfiles,
    Map<String, DiscreteProfile> discreteExternalProfiles
) {
  public EvaluationEnvironment() {
    this(Map.of(), Map.of(), Map.of(), Map.of());
  }
}
