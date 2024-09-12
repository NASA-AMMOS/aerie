package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.services.constraints.ConstraintResult;

public record ConstraintRunRecord(
  long constraintId,
  ConstraintResult result
) {}
