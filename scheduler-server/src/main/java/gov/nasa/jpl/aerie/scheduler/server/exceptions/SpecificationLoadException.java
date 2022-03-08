package gov.nasa.jpl.aerie.scheduler.server.exceptions;

import gov.nasa.jpl.aerie.scheduler.server.models.SpecificationId;

public final class SpecificationLoadException extends Exception {
  public SpecificationLoadException(final SpecificationId specificationId, final String reason) {
    super("Failed to load specification with id `" + specificationId.id() + "` reason: " + reason);
  }
}
