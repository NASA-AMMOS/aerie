package gov.nasa.jpl.aerie.merlin.server.http;

import static gov.nasa.jpl.aerie.json.JsonParseResult.FailureReason;

import java.util.List;

public class InvalidEntityException extends Exception {

  public final List<FailureReason> failures;

  public InvalidEntityException(List<FailureReason> failures) {
    this.failures = List.copyOf(failures);
  }
}
