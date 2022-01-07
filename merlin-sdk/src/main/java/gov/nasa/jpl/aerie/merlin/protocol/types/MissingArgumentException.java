package gov.nasa.jpl.aerie.merlin.protocol.types;

public class MissingArgumentException extends RuntimeException {

  public MissingArgumentException(
      final String metaName,
      final String activityName,
      final String parameterName,
      final ValueSchema schema)
  {
    super("Required argument for %s \"%s\" not provided by plan: \"%s\" of type %s"
        .formatted(metaName, activityName, parameterName, schema));
  }
}
