package gov.nasa.jpl.aerie.constraints.model;

public sealed interface Dependency {
  record ResourceDependency(String resourceName) implements Dependency{}
  record SpecificActivityDurationDependency(String activityAlias) implements Dependency{}
  //for goals or activityexpression
  record ActivityTypeDurationDependency(String activityTypeName) implements Dependency{}
}
