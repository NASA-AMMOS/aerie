package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.constraints.model.ConstraintResult;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;

public record ConstraintRunRecord(
  long constraintId,
  long constraintInvocationId,
  Map<String, SerializedValue> arguments,
  ConstraintResult result
) {}
