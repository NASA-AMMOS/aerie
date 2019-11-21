package gov.nasa.jpl.ammos.mpsa.aerie.simulation.http;

import gov.nasa.jpl.ammos.mpsa.aerie.simulation.utils.InvalidEntityFailure;

import java.util.List;

public final class InvalidEntityException extends Exception {
  public final List<InvalidEntityFailure> failures;
  public InvalidEntityException(final List<InvalidEntityFailure> failures) {
    super("Failed to deserialize entity");
    this.failures = List.copyOf(failures);
  }
}
