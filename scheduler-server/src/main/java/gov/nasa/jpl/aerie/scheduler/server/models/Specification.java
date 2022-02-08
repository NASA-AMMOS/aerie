package gov.nasa.jpl.aerie.scheduler.server.models;

import java.util.List;
import java.util.Map;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;

public record Specification(
    PlanId planId,
    List<RuleId> ruleIdsByPriority,
    Timestamp horizonStartTimestamp,
    Timestamp horizonEndTimestamp,
    Map<String, SerializedValue> simulationArguments
)
{
}
