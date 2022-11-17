package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.profile.IntervalMap;
import gov.nasa.jpl.aerie.constraints.profile.LinearEquation;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;

/** A container for additional context needed for Constraints AST evaluation. */
public record EvaluationEnvironment(
    Map<String, ActivityInstance> activityInstances,
    Map<String, IntervalMap<LinearEquation>> realExternalProfiles,
    Map<String, IntervalMap<SerializedValue>> discreteExternalProfiles
) {
  public EvaluationEnvironment() {
    this(Map.of(), Map.of(), Map.of());
  }
}
