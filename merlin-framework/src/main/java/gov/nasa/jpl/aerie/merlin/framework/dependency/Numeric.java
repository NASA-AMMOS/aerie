package gov.nasa.jpl.aerie.merlin.framework.dependency;

public sealed interface Numeric {
  record Value(double value) implements Numeric {}
  record ParameterValue(String activityType, String parameterName) implements Numeric {}
  record ObjectValue(Object object) implements Numeric {}

  static Value doubleValue(double value){
    return new Value(value);
  }

  static ParameterValue parameterValue(String activityType, String parameterValue){
    return new ParameterValue(activityType, parameterValue);
  }
}
