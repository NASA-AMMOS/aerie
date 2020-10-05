package gov.nasa.jpl.ammos.mpsa.aerie.plan.http;

import java.util.List;

import static gov.nasa.jpl.ammos.mpsa.aerie.json.JsonParseResult.FailureReason;

public class InvalidEntityException extends Exception {

  public final List<FailureReason> failures;

  public InvalidEntityException(List<FailureReason> failures) {
    this.failures = List.copyOf(failures);
  }
}
