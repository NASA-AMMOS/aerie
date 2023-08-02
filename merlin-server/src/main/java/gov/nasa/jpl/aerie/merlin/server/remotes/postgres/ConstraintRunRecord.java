package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.constraints.model.ConstraintResult;

public record ConstraintRunRecord(
  long constraintId,
  ConstraintResult violation
) {}
