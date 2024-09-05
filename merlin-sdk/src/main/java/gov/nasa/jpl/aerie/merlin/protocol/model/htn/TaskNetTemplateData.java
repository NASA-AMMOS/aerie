package gov.nasa.jpl.aerie.merlin.protocol.model.htn;

import java.util.Set;

public record TaskNetTemplateData(
  Set<ActivityReference> subtasks,
  Set<TaskNetworkTemporalConstraint> constraints
){}
