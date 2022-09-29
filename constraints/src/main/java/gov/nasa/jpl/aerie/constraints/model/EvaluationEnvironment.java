package gov.nasa.jpl.aerie.constraints.model;

import java.util.Map;

/** A container for additional context needed for Constraints AST evaluation. */
public record EvaluationEnvironment(
    Map<String, ActivityInstance> activityInstances
) {
  public EvaluationEnvironment() {
    this(Map.of());
  }
}
