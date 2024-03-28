package gov.nasa.jpl.aerie.merlin.framework.dependency;

public sealed interface DiscreteValue {
  record DiscreteParameter(String activityType, String name) implements DiscreteValue {}
  record ModelReference(Object object) implements DiscreteValue {}
}
