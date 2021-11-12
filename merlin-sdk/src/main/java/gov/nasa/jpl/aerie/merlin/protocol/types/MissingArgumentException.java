package gov.nasa.jpl.aerie.merlin.protocol.types;

import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

public class MissingArgumentException extends RuntimeException {
  public MissingArgumentException(final String activityName, final String parameterName, final ValueSchema schema) {
    super("Required argument for activity \"%s\" not provided by plan: \"%s\" of type %s".formatted(activityName, parameterName, schema));
  }
}
