package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.processor;

final class ActivityType {
  private final String name;
  private final ParameterInfo parameter;

  ActivityType(
      final String name,
      final ParameterInfo parameter
  ) {
    this.name = name;
    this.parameter = parameter;
  }

  String getName() {
    return name;
  }
  ParameterInfo getParameter() {
    return parameter;
  }
}
