package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.constraints.model.Violation;

public record ConstraintRunRecord(
  long constraintId,
  Violation violation
) {}
