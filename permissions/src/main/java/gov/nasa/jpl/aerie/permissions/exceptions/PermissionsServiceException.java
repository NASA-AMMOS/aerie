package gov.nasa.jpl.aerie.permissions.exceptions;

import javax.json.JsonValue;

public class PermissionsServiceException extends Exception {
  public final JsonValue errors;
  public PermissionsServiceException(final String message, final JsonValue errors) {
    super(message);
    this.errors = errors;
  }
}
